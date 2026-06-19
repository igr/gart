package work.nervure

import dev.oblac.gart.Gart
import dev.oblac.gart.color.CyanotypeColors
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.*
import dev.oblac.gart.noise.PoissonDiskSamplingNoise
import dev.oblac.gart.noise.SimplexNoise
import org.jetbrains.skia.*
import org.jetbrains.skia.Shader.Companion.makeRadialGradient
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * NERVURE
 *
 * Open venation, wrote the engine myself (space colonization basically). Start
 * w/ one seed, scatter a bunch of food points around. Each food tugs the nearest
 * branch one step toward it, and once a branch gets close enough the food gets
 * eaten. thats the whole rule - the tree, forks and all, just falls out of it.
 *
 * Drawn cyanotype style: dark prussian blue in the old trunk, bright pale blue at
 * the fresh tips. segments get thicker the more flow they carry.
 */

private const val W = 1200
private const val H = 1200

// growth
private const val MARGIN = 60f
private const val ATTRACTOR_MINDIST = 9.0
private const val INFLUENCE_RADIUS = 46f
private const val KILL_RADIUS = 16f
private const val STEP_LEN = 6f
private const val WOBBLE = 0.16f
private const val NOISE_SCALE = 0.0032
private const val DENSITY_MIN = 0.8f
private const val DENSITY_MAX = 1.0f
private const val MAX_NODES = 120_000
private const val MAX_STEPS = 3000
private val SEED_X = W * 0.5f
private val SEED_Y = H - MARGIN     // seat the seed inside the attractor field so growth always starts

// render
private const val GRAD_STEPS = 512
private const val AGE_LO = 0.38f      // oldest veins start this far up the cyanotype ramp (skip bg-dark stops)
private const val MIN_W = 0.6f
private const val MAX_W = 9.0f
private const val THICKNESS_EXP = 2.1f
private const val EDGE_BAND = 26
private const val GLOW_W = 5.5f
private const val GLOW_BLUR = 6f
private const val CORE_W = 1.1f
private const val SPARK_R = 2.0f
private val BG_EDGE = 0xFF070C16.toInt()
private val GLOW_COLOR = 0xFF9ACDE0.toInt()
private val CORE_COLOR = 0xFFEFF7FA.toInt()

fun main() {
    val seed = System.getProperty("seed")?.toLong() ?: 9L
    val outName = System.getProperty("out") ?: "nervure.png"
    val headless = System.getProperty("headless") != null

    val gart = Gart.of("nervure", W, H)
    println(gart)

    val g = gart.gartvas()
    val c = g.canvas

    val rng = Random(seed)
    val venation = Venation(rng)
    println("attractors: ${venation.attractorCount}")
    venation.grow()
    println("nodes: ${venation.nodeCount}, steps: ${venation.lastStep}")

    drawBackground(c)
    drawVeins(c, venation)

    gart.saveImage(g, outName)
    if (!headless) gart.window().showImage(g)
}

// SIMULATION

private class Node(val x: Float, val y: Float, val parent: Int, val birth: Int)

private class Venation(private val rng: Random) {
    val nodes = ArrayList<Node>(MAX_NODES)

    // attractors as flat parallel lists
    private val ax = ArrayList<Float>()
    private val ay = ArrayList<Float>()
    private val alive = ArrayList<Boolean>()
    private var aliveCount = 0

    // uniform grid over node positions (cell == influence radius -> 3x3 search)
    private val cell = INFLUENCE_RADIUS
    private val cols = ceil(W / cell).toInt()
    private val rows = ceil(H / cell).toInt()
    private val grid = Array(cols * rows) { ArrayList<Int>(4) }

    var lastStep = 0; private set
    val nodeCount get() = nodes.size
    val attractorCount get() = ax.size

    init {
        seedAttractors()
        addNode(Node(SEED_X, SEED_Y, -1, 0))
    }

    private fun seedAttractors() {
        val poisson = PoissonDiskSamplingNoise(rng.nextLong())
        val pts = poisson.generate(
            MARGIN.toDouble(), MARGIN.toDouble(),
            (W - MARGIN).toDouble(), (H - MARGIN).toDouble(),
            ATTRACTOR_MINDIST, 11
        )
        for (p in pts) {
            val n = map(SimplexNoise.noise(p.x * NOISE_SCALE, p.y * NOISE_SCALE), -1, 1, 0, 1)
            if (rng.rndf() < lerp(DENSITY_MIN, DENSITY_MAX, n)) {
                ax.add(p.x); ay.add(p.y); alive.add(true); aliveCount++
            }
        }
    }

    private fun cellOf(x: Float, y: Float): Int {
        val gx = (x / cell).toInt().coerceIn(0, cols - 1)
        val gy = (y / cell).toInt().coerceIn(0, rows - 1)
        return gy * cols + gx
    }

    private fun addNode(node: Node): Int {
        val idx = nodes.size
        nodes.add(node)
        grid[cellOf(node.x, node.y)].add(idx)
        return idx
    }

    private fun nearestNode(x: Float, y: Float): Int {
        val gx = (x / cell).toInt().coerceIn(0, cols - 1)
        val gy = (y / cell).toInt().coerceIn(0, rows - 1)
        var best = -1
        var bestD = INFLUENCE_RADIUS * INFLUENCE_RADIUS
        for (cy in (gy - 1).coerceAtLeast(0)..(gy + 1).coerceAtMost(rows - 1)) {
            for (cx in (gx - 1).coerceAtLeast(0)..(gx + 1).coerceAtMost(cols - 1)) {
                for (i in grid[cy * cols + cx]) {
                    val dx = x - nodes[i].x
                    val dy = y - nodes[i].y
                    val d2 = dx * dx + dy * dy
                    if (d2 < bestD) { bestD = d2; best = i }
                }
            }
        }
        return best
    }

    fun grow() {
        var step = 0
        while (aliveCount > 0 && nodes.size < MAX_NODES && step < MAX_STEPS) {
            step++
            val n = nodes.size
            val dirx = FloatArray(n)
            val diry = FloatArray(n)
            val touched = BooleanArray(n)
            val touchedList = ArrayList<Int>()

            for (a in ax.indices) {
                if (!alive[a]) continue
                val near = nearestNode(ax[a], ay[a])
                if (near < 0) continue
                val dx = ax[a] - nodes[near].x
                val dy = ay[a] - nodes[near].y
                val d = hypotFast(dx, dy)
                if (d <= KILL_RADIUS) { alive[a] = false; aliveCount-- }
                if (d > 1e-4f) {
                    dirx[near] += dx / d
                    diry[near] += dy / d
                    if (!touched[near]) { touched[near] = true; touchedList.add(near) }
                }
            }

            if (touchedList.isEmpty()) break

            for (ni in touchedList) {
                if (nodes.size >= MAX_NODES) break
                var ux = dirx[ni]
                var uy = diry[ni]
                val len = hypotFast(ux, uy)
                if (len < 1e-4f) continue
                ux /= len; uy /= len
                val angle = rng.rndf(-WOBBLE, WOBBLE)
                val ca = cos(angle); val sa = sin(angle)
                val rx = ux * ca - uy * sa
                val ry = ux * sa + uy * ca
                addNode(Node(nodes[ni].x + rx * STEP_LEN, nodes[ni].y + ry * STEP_LEN, ni, step))
            }
        }
        lastStep = step
    }
}

// RENDER

private fun drawBackground(c: Canvas) {
    c.clear(CyanotypeColors.palette2[0])
    c.drawPaint(Paint().apply {
        shader = makeRadialGradient(
            W * 0.5f, H * 0.62f, W * 0.92f,
            gradientOf(
                intArrayOf(CyanotypeColors.palette2[2], CyanotypeColors.palette2[0], BG_EDGE),
                floatArrayOf(0f, 0.55f, 1f)
            )
        )
    })
}

/** how many tips each node feeds = roughly the flow going thru it. */
private fun computeTips(nodes: List<Node>): IntArray {
    val tips = IntArray(nodes.size)
    val hasChild = BooleanArray(nodes.size)
    for (i in nodes.indices) {
        val p = nodes[i].parent
        if (p >= 0) hasChild[p] = true
    }
    for (i in nodes.indices) if (!hasChild[i]) tips[i] = 1
    // parent index is always < child index (parent existed first), so a reverse
    // pass accumulates each subtree's tips into its parent.
    for (i in nodes.indices.reversed()) {
        val p = nodes[i].parent
        if (p >= 0) tips[p] += tips[i]
    }
    return tips
}

private fun drawVeins(c: Canvas, v: Venation) {
    val nodes = v.nodes
    val tips = computeTips(nodes)
    val maxTips = (tips.maxOrNull() ?: 1).coerceAtLeast(1)
    val maxBirth = v.lastStep.coerceAtLeast(1)
    val gradient = CyanotypeColors.palette2.expand(GRAD_STEPS)
    val denom = maxTips.toFloat().pow(1f / THICKNESS_EXP)

    // oldest first so freshly-grown bright tips layer on top of the dark trunk
    val order = nodes.indices.filter { nodes[it].parent >= 0 }.sortedBy { nodes[it].birth }
    for (i in order) {
        val node = nodes[i]
        val p = nodes[node.parent]
        val age = node.birth.toFloat() / maxBirth                 // 0 = old, 1 = young
        val w = lerp(MIN_W, MAX_W, tips[i].toFloat().pow(1f / THICKNESS_EXP) / denom)
            .coerceIn(MIN_W, MAX_W)
        val color = gradient.safe((lerp(AGE_LO, 1f, age) * (GRAD_STEPS - 1)).toInt())
        c.drawLine(p.x, p.y, node.x, node.y, strokeOf(color, w).apply {
            strokeCap = PaintStrokeCap.ROUND
        })
    }

    drawLivingEdge(c, nodes, tips, maxBirth)
}

private fun drawLivingEdge(c: Canvas, nodes: List<Node>, tips: IntArray, maxBirth: Int) {
    val cutoff = maxBirth - EDGE_BAND
    for (i in nodes.indices) {
        val node = nodes[i]
        if (node.parent < 0) continue
        if (node.birth < cutoff) continue
        if (tips[i] != 1) continue           // only fresh leaf tips
        val p = nodes[node.parent]
        // soft glow under the tip
        c.drawLine(p.x, p.y, node.x, node.y, strokeOf(alpha(GLOW_COLOR, 110), GLOW_W).apply {
            maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, GLOW_BLUR)
        })
        // crisp near-white core
        c.drawLine(p.x, p.y, node.x, node.y, strokeOf(alpha(CORE_COLOR, 210), CORE_W).apply {
            strokeCap = PaintStrokeCap.ROUND
        })
        // the one warm note: ember spark on the very newest tips
        if (node.birth >= maxBirth - 2) {
            c.drawCircle(node.x, node.y, SPARK_R, fillOf(alpha(CyanotypeColors.accent.toInt(), 190)))
        }
    }
}
