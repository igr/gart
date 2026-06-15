package rugae

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.argb
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.gfx.toClosedPath
import dev.oblac.gart.math.*
import dev.oblac.gart.noise.SimplexNoise
import org.jetbrains.skia.*
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * RUGAE
 *
 * Differential growth, custom engine. A small ring of nodes grows by inserting new
 * nodes where a noise field feeds it; nodes pull on their neighbours and
 * push away from everyone nearby. Nothing else. The folding, the lobes,
 * the coral-like anatomy all emerge from those two opposing forces - the
 * image is simply every 10th state of the curve, oldest to newest, each
 * layer colored by a palette expanded into a gradient: the process itself
 * is the picture, aging from deep teal to ember.
 */

private const val W = 1200
private const val H = 1200

private const val ITERATIONS = 1200
private const val SNAPSHOT_EVERY = 5
private const val MAX_NODES = 12_000

private const val SPLIT_LEN = 9f         // edges longer than this subdivide
private const val REPULSE_RADIUS = 44f   // personal space of a node
private const val ATTRACT = 0.45f        // pull toward neighbour midpoint
private const val REPULSE = 1.2f         // push from nearby nodes
private const val MAX_STEP = 1.9f
private const val MARGIN = 40f
private const val NOISE_SCALE = 0.0042

fun main() {
    val gart = Gart.of("rugae", W, H)
    println(gart)

    val g = gart.gartvas()
    val c = g.canvas
    val rng = Random(47)

    val growth = Growth(rng)
    val snapshots = mutableListOf<List<Point>>()
    repeat(ITERATIONS) { iter ->
        growth.step()
        if (iter % SNAPSHOT_EVERY == 0) snapshots += growth.snapshot()
    }
    snapshots += growth.snapshot()
    println("nodes: ${growth.size}, snapshots: ${snapshots.size}")

    drawBackground(c)
    drawGrowth(c, snapshots)

    gart.saveImage(g)
    gart.window().showImage(g)
}

// SIMULATION!

private class Node(var x: Float, var y: Float)

private class Growth(private val rng: Random) {
    private var nodes = ArrayList<Node>(MAX_NODES)
    private val cols = ceil(W / REPULSE_RADIUS).toInt()
    private val rows = ceil(H / REPULSE_RADIUS).toInt()
    private val grid = Array(cols * rows) { ArrayList<Int>(8) }

    val size get() = nodes.size

    init {
        // seed: a small wobbly ring
        val n = 50
        repeat(n) { k ->
            val a = k * TWO_PIf / n
            val r = rng.rndf(46f, 50f)
            nodes.add(Node(W * 0.5f + cos(a) * r, H * 0.52f + sin(a) * r))
        }
    }

    fun snapshot(): List<Point> = nodes.map { Point(it.x, it.y) }

    fun step() {
        rebuildGrid()
        applyForces()
        splitStretchedEdges()
        feedFromNoise()
    }

    private fun rebuildGrid() {
        grid.forEach { it.clear() }
        nodes.forEachIndexed { i, node -> grid[cellOf(node.x, node.y)].add(i) }
    }

    private fun cellOf(x: Float, y: Float): Int {
        val gx = (x / REPULSE_RADIUS).toInt().coerceIn(0, cols - 1)
        val gy = (y / REPULSE_RADIUS).toInt().coerceIn(0, rows - 1)
        return gy * cols + gx
    }

    private fun applyForces() {
        val n = nodes.size
        val fx = FloatArray(n)
        val fy = FloatArray(n)

        for (i in 0 until n) {
            val node = nodes[i]
            val prev = nodes[wrap(i - 1, n)]
            val next = nodes[wrap(i + 1, n)]

            // attraction to the neighbour midpoint smooths and holds the chain together
            fx[i] += ((prev.x + next.x) * 0.5f - node.x) * ATTRACT
            fy[i] += ((prev.y + next.y) * 0.5f - node.y) * ATTRACT

            // repulsion from every node nearby, except the chain neighbours
            val gx = (node.x / REPULSE_RADIUS).toInt().coerceIn(0, cols - 1)
            val gy = (node.y / REPULSE_RADIUS).toInt().coerceIn(0, rows - 1)
            for (cy in (gy - 1).coerceAtLeast(0)..(gy + 1).coerceAtMost(rows - 1)) {
                for (cx in (gx - 1).coerceAtLeast(0)..(gx + 1).coerceAtMost(cols - 1)) {
                    for (j in grid[cy * cols + cx]) {
                        if (j == i) continue
                        val ringDist = chainDistance(i, j, n)
                        if (ringDist <= 2) continue
                        val other = nodes[j]
                        val dx = node.x - other.x
                        val dy = node.y - other.y
                        val d = hypotFast(dx, dy)
                        if (d !in 0.001f..<REPULSE_RADIUS) continue
                        val f = REPULSE * (1f - d / REPULSE_RADIUS) / d
                        fx[i] += dx * f
                        fy[i] += dy * f
                    }
                }
            }

            // brownian nudge, so buckling starts on its own
            fx[i] += rng.rndf(-0.12f, 0.12f)
            fy[i] += rng.rndf(-0.12f, 0.12f)

            // soft walls
            if (node.x < MARGIN) fx[i] += (MARGIN - node.x) * 0.05f
            if (node.x > W - MARGIN) fx[i] -= (node.x - (W - MARGIN)) * 0.05f
            if (node.y < MARGIN) fy[i] += (MARGIN - node.y) * 0.05f
            if (node.y > H - MARGIN) fy[i] -= (node.y - (H - MARGIN)) * 0.05f
        }

        for (i in 0 until n) {
            val l = hypotFast(fx[i], fy[i])
            val s = if (l > MAX_STEP) MAX_STEP / l else 1f
            nodes[i].x += fx[i] * s
            nodes[i].y += fy[i] * s
        }
    }

    private fun chainDistance(i: Int, j: Int, n: Int): Int {
        val d = if (i > j) i - j else j - i
        return minOf(d, n - d)
    }

    private fun splitStretchedEdges() {
        if (nodes.size >= MAX_NODES) return
        val inserts = mutableListOf<Pair<Int, Node>>() // insert before index
        val n = nodes.size
        for (i in 0 until n) {
            if (n + inserts.size >= MAX_NODES) break
            val a = nodes[i]
            val b = nodes[wrap(i + 1, n)]
            if (dist(a.x, a.y, b.x, b.y) > SPLIT_LEN) {
                inserts += (i + 1) to midpoint(a, b)
            }
        }
        inserts.asReversed().forEach { (at, node) -> nodes.add(at, node) }
    }

    /** Growth hormone: random edges subdivide where the noise field is rich. */
    private fun feedFromNoise() {
        if (nodes.size >= MAX_NODES) return
        repeat((nodes.size / 40).coerceAtLeast(3)) {
            val i = rng.rndi(nodes.size)
            val a = nodes[i]
            val b = nodes[wrap(i + 1, nodes.size)]
            val mx = (a.x + b.x) * 0.5f
            val my = (a.y + b.y) * 0.5f
            val food = map(SimplexNoise.noise(mx * NOISE_SCALE, my * NOISE_SCALE), -1, 1, 0, 1)
            if (rng.rndf() < food * food) {
                nodes.add(i + 1, midpoint(a, b))
            }
        }
    }

    private fun midpoint(a: Node, b: Node) = Node(
        (a.x + b.x) * 0.5f + rng.rndf(-0.3f, 0.3f),
        (a.y + b.y) * 0.5f + rng.rndf(-0.3f, 0.3f),
    )
}

// RENDER

//private val palette = Palettes.cool1
private val palette = Palettes.cool85
private const val CYCLES = 2        // palette cycles

private fun drawBackground(c: Canvas) {
//    c.drawPaint(Paint().apply {
//        shader = makeRadialGradient(
//            W * 0.3f, H * 0.26f, W * 0.85f,
//            gradientOf(
//                intArrayOf(RetroColors.red01, RetroColors.black01),
//                floatArrayOf(0f, 0.5f)
//            )
//        )
//    })
    c.clear(RetroColors.black01)
}

private fun drawGrowth(c: Canvas, snapshots: List<List<Point>>) {
    val gradient = palette.expand(snapshots.size)
    val last = snapshots.size - 1
    snapshots.forEachIndexed { s, pts ->
        val t = s.toFloat() / last
        val path = pts.toClosedPath()
        val color = gradient.safe(s * CYCLES)
        c.drawPath(path, strokeOf(alpha(color, lerp(150f, 255f, t).toInt()), lerp(1.2f, 1.8f, t)).apply {
            strokeCap = PaintStrokeCap.ROUND
            strokeJoin = PaintStrokeJoin.ROUND
        })
//        if (s == 160) {
//            c.drawCircle(400f, 300f, 200f, fillOfBlack())
//        }
    }

    // the living edge, lit
    val path = snapshots.last().toClosedPath()
    c.drawPath(path, strokeOf(alpha(gradient.last(), 110), 5.5f).apply {
        maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, 6f)
    })
    c.drawPath(path, strokeOf(argb(255, 255, 250, 240), 2f).apply {
        strokeCap = PaintStrokeCap.ROUND
        strokeJoin = PaintStrokeJoin.ROUND
    })
}
