package work.nervure

import dev.oblac.gart.Gart
import dev.oblac.gart.color.CyanotypeColors
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.*
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
 * Open venation, space-colonization engine written from scratch. One seed, a
 * field of food points, each food tugs the nearest branch one step toward it and
 * gets eaten once a branch arrives. The tree, forks and all, falls out of that.
 *
 * What makes it more than a skeleton: the growth is PHOTOTROPIC and FORAGING.
 * The food field is anisotropic (FBM thickets + a macro density gradient that
 * piles food toward the light) and shaped by an envelope that fades food out
 * near the top + side frame, so the organism crowns organically instead of
 * combing straight into the edge. Every step is biased toward the light and bent
 * by a coherent-noise curl, so branches wander and fork like they're foraging
 * rather than streaming in parallel.
 *
 * And it keeps its own history. Each node remembers its birth step, so we render
 * the TRACE OF GROWTH in three layers:
 *   - tissue bloom : veins redrawn as an additive blurred halo + core, so the
 *                    thing reads as backlit living tissue with depth.
 *   - isochrones   : the arrival-time field contoured (marching squares) into
 *                    faint time-rings - where the growth front stood at each step.
 *   - veins        : the crisp cyanotype skeleton, deep prussian trunk -> bright tips.
 *
 * Presets weight those layers (and the engine) differently: `tissue` (membrane-led),
 * `palimpsest` (isochrone-led), `reach` (hard phototropic diagonal lean). Every
 * knob is a -D override.
 *
 * family: rugae, nervure, corona, areola.
 */

private const val W = 1200
private const val H = 1200

// growth
private const val MARGIN = 60f
private const val ATTRACTOR_MINDIST = 9.0
private const val INFLUENCE_RADIUS = 46f
private const val KILL_RADIUS = 16f
private const val STEP_LEN = 6f
private const val WOBBLE = 0.09f             // residual random jitter (coherent curl does most of the bending)
private const val CURL_SCALE = 0.0040        // simplex frequency for the coherent meander
private const val NOISE_S1 = 0.0026          // food-field FBM, low octave
private const val NOISE_S2 = 0.0061          // food-field FBM, high octave
private const val KEEP_FLOOR = 0.14f         // min food-keep prob (inside the envelope) so growth never stalls
private const val IGNITE_R = 210f            // dense food wad around the seed so every seed ignites a full fan
private const val IGNITE_STRENGTH = 0.92f
private const val TOP_FADE0 = 0.10f          // food fully gone above the top 10% -> organic crown, no top-edge comb
private const val TOP_FADE1 = 0.34f          // ...fading back to full food by 34% down
private const val SIDE_FADE = 95f            // food fades within this distance of the left/right frame
private const val MAX_NODES = 150_000
private const val MAX_STEPS = 3000

// render
private const val GRAD_STEPS = 512
private const val AGE_LO = 0.26f              // oldest veins sink deep into prussian -> wide value range = depth
private const val MIN_W = 0.6f
private const val MAX_W = 9.0f
private const val THICKNESS_EXP = 2.1f
private const val EDGE_BAND = 26
private const val GLOW_W = 5.5f
private const val GLOW_BLUR = 6f
private const val CORE_W = 1.1f
private const val SPARK_R = 2.2f
private val BG_EDGE = 0xFF070C16.toInt()
private val GLOW_COLOR = 0xFF9ACDE0.toInt()
private val CORE_COLOR = 0xFFEFF7FA.toInt()

// bloom (additive backlit membrane: a glowing halo + hot core hugging the veins)
private val BLOOM_HALO = 0xFF8FC8DE.toInt()
private val BLOOM_CORE = 0xFFD6EEF6.toInt()
private const val BLOOM_W_HALO = 2.4f
private const val BLOOM_SIGMA_HALO = 9f
private const val BLOOM_ALPHA_HALO = 74
private const val BLOOM_W_CORE = 1.1f
private const val BLOOM_SIGMA_CORE = 3f
private const val BLOOM_ALPHA_CORE = 60

// isochrone field
private const val FS = 4                      // arrival-field downscale (1200 -> 300)
private const val BIG = 1e9f                  // "never reached" sentinel for unreached cells
private const val ISO_W = 1.0f

// a few soft glowing white orbs the veins wander over
private const val CIRCLE_R_MIN = 16f
private const val CIRCLE_R_MAX = 66f
private const val CIRCLE_ALPHA = 215         // soft body
private const val CIRCLE_GLOW_ALPHA = 58     // outer aura
private const val CIRCLE_BODY_BLUR = 0.16f   // feather, x radius
private const val CIRCLE_GLOW_BLUR = 0.45f   // aura spread, x radius

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val p = resolveParams()

    val gart = Gart.of("nervure", W, H)
    println(gart)
    println("preset=${p.preset} palette=${p.palette} seed=${p.seed} light=${p.lightDeg} pull=${p.pull} macro=${p.macro} curl=${p.curl} bloom=${p.bloom} iso=${p.isoLevels} splat=${p.splat}")

    val g = gart.gartvas()
    val c = g.canvas

    val rng = Random(p.seed)
    val venation = Venation(rng, p)
    println("attractors: ${venation.attractorCount}")
    venation.grow()
    println("nodes: ${venation.nodeCount}, steps: ${venation.lastStep}")

    val nodes = venation.nodes
    val tips = computeTips(nodes)
    val cyan = p.palette == "cyanotype"
    val gradient = paletteOf(p.palette).expand(GRAD_STEPS)
    // bloom tint follows the palette: a bright mid-high hue halo + a near-white-hot core
    val haloC = if (cyan) BLOOM_HALO else gradient.safe((0.70f * (GRAD_STEPS - 1)).toInt())
    val coreC = if (cyan) BLOOM_CORE else lerpColor(gradient.safe(GRAD_STEPS - 1), 0xFFFFFFFF.toInt(), 0.45f)

    drawBackground(c, gradient, cyan)
    if (p.bloom > 0f) drawBloom(c, nodes, tips, p, haloC, coreC, venation.lastStep)
    drawCircles(c, p)
    if (p.isoLevels > 0 && p.isoAlpha > 0f) {
        val field = buildArrivalField(nodes, p.splat)
        drawIsochrones(c, field, venation.lastStep, p, gradient)
    }
    drawVeins(c, nodes, tips, venation.lastStep, gradient, p.ageLo, p.ageWeight)

    gart.saveImage(g, "${p.out}.png")
    if (!headless) gart.window().showImage(g)
}

// PARAMS

private class Params(
    val seed: Long,
    val out: String,
    val preset: String,
    val seedX: Float,
    val seedY: Float,
    val lightDeg: Float,
    val pull: Float,
    val curl: Float,
    val macro: Float,
    val thicketLo: Float,
    val thicketHi: Float,
    val bloom: Float,
    val isoLevels: Int,
    val isoAlpha: Float,
    val splat: Int,
    val palette: String,
    val ageLo: Float,
    val ageWeight: Float,
    val circles: Int,
) {
    private val lightRad = lightDeg * PIf / 180f
    val lx = cos(lightRad)              // light unit vector, screen coords (y down)
    val ly = -sin(lightRad)            // +deg => up; 90 = straight up, 0 = right
}

private data class Pre(
    val bloom: Float, val iso: Int, val isoa: Float,
    val pull: Float, val light: Float, val macro: Float,
    val curl: Float, val splat: Int, val seedfx: Float, val seedfy: Float, val agew: Float,
)

private fun resolveParams(): Params {
    val preset = ps("preset", "reach")
    val palette = ps("palette", "inferno")
    val pre = when (preset) {
        //                bloom  iso  isoa  pull  light  macro  curl  splat  seedfx seedfy agew
        "palimpsest" -> Pre(0.8f, 26, 0.42f, 0.16f, 80f, 0.15f, 0.48f, 6, 0.50f, 0.95f, 0.0f)
        "tissue" -> Pre(1.8f, 8, 0.10f, 0.15f, 82f, 0.12f, 0.48f, 4, 0.50f, 0.95f, 0.0f)
        else -> Pre(1.6f, 0, 0.16f, 0.85f, 36f, 0.28f, 0.40f, 4, 0.20f, 0.92f, 1.0f)   // reach: no outlines, older=thicker
    }
    return Params(
        seed = pi("seed", 9).toLong(),
        out = ps("out", "nervure"),
        preset = preset,
        seedX = pf("seedx", pre.seedfx) * W,
        seedY = pf("seedy", pre.seedfy) * H,
        lightDeg = pf("light", pre.light),
        pull = pf("pull", pre.pull),
        curl = pf("curl", pre.curl),
        macro = pf("macro", pre.macro),
        thicketLo = pf("tlo", 0.40f),
        thicketHi = pf("thi", 0.60f),
        bloom = pf("bloom", pre.bloom),
        isoLevels = pi("iso", pre.iso),
        isoAlpha = pf("isoa", pre.isoa),
        splat = pi("splat", pre.splat),
        palette = palette,
        ageLo = pf("agelo", if (palette == "cyanotype") AGE_LO else 0.32f),
        ageWeight = pf("agew", pre.agew),   // 0 = thickness by flow only; 1 = thickness by age (old=thick)
        circles = pi("circles", -1),        // -1 = random 5..8 white discs; 0 = none; N = exactly N
    )
}

/** the age->color ramp. `cyanotype` keeps the monochrome original; the rest turn growth-time
 *  into a multi-hue spectrum (old trunk -> bright young tips). */
private fun paletteOf(name: String): Palette = when (name) {
    "plasma" -> Palettes.colormap085
    "magma" -> Palettes.colormap084
    "inferno" -> Palettes.colormap083
    "turbo" -> Palettes.colormap073
    "viridis" -> Palettes.colormap087
    "twilight" -> Palettes.colormap086
    "cividis" -> Palettes.colormap082
    "sunset" -> Palettes.colormap026     // CARTO SunsetDark
    "tealrose" -> Palettes.colormap029   // CARTO TealRose
    else -> CyanotypeColors.palette2      // "cyanotype"
}

// SIMULATION

private class Node(val x: Float, val y: Float, val parent: Int, val birth: Int)

private class Venation(private val rng: Random, private val p: Params) {
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

    private val cx = W * 0.5
    private val cy = H * 0.5

    var lastStep = 0; private set
    val nodeCount get() = nodes.size
    val attractorCount get() = ax.size

    init {
        seedAttractors()
        addNode(Node(p.seedX, p.seedY, -1, 0))
    }

    /** food density: FBM thickets, a macro gradient toward the light, an envelope that fades
     *  food near the top/side frame, and a dense ignition wad around the seed. */
    private fun keepProb(x: Float, y: Float): Float {
        val n1 = SimplexNoise.noise(x * NOISE_S1, y * NOISE_S1)
        val n2 = SimplexNoise.noise(x * NOISE_S2 + 137.0, y * NOISE_S2 + 59.0)
        val base = map(n1 * 0.65 + n2 * 0.35, -1.0, 1.0, 0.0, 1.0)     // 0..1 thickets
        val proj = ((x - cx) * p.lx + (y - cy) * p.ly) / (W * 0.5)     // -1..1 along the light axis
        val v = (base + proj * p.macro).toFloat()
        val contrast = smoothstep(p.thicketLo, p.thicketHi, v)         // thickets (1) vs sparse (0)
        val prob = lerp(KEEP_FLOOR, 1.0f, contrast)                    // never fully barren (inside envelope)
        // ignition wad: pile food around the seed so the sprout always fans out and catches the field
        val ds = hypotFast(x - p.seedX, y - p.seedY)
        val ignite = 1f - smoothstep(IGNITE_R * 0.35f, IGNITE_R, ds)
        val lit = lerp(prob, 0.95f, ignite * IGNITE_STRENGTH)
        return lit * envelope(x, y)
    }

    /** deterministic jittered-grid attractors (one jittered point per cell), thinned by the food
     *  field. Self-contained on the seeded rng -> fully reproducible. (gart's PoissonDiskSamplingNoise
     *  seeds its first sample from the un-seedable global rnd(), so it is NOT reproducible.) */
    private fun seedAttractors() {
        val step = ATTRACTOR_MINDIST
        val lo = MARGIN.toDouble()
        val hiX = (W - MARGIN).toDouble()
        val hiY = (H - MARGIN).toDouble()
        var gy = lo
        while (gy < hiY) {
            var gx = lo
            while (gx < hiX) {
                val x = (gx + rng.rnd(0.0, step)).toFloat()
                val y = (gy + rng.rnd(0.0, step)).toFloat()
                if (rng.rndf() < keepProb(x, y)) {
                    ax.add(x); ay.add(y); alive.add(true); aliveCount++
                }
                gx += step
            }
            gy += step
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
        for (cyy in (gy - 1).coerceAtLeast(0)..(gy + 1).coerceAtMost(rows - 1)) {
            for (cxx in (gx - 1).coerceAtLeast(0)..(gx + 1).coerceAtMost(cols - 1)) {
                for (i in grid[cyy * cols + cxx]) {
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

                // phototropism: bias the step toward the light, then renormalize
                ux += p.lx * p.pull
                uy += p.ly * p.pull
                val l2 = hypotFast(ux, uy)
                if (l2 < 1e-4f) continue
                ux /= l2; uy /= l2

                // coherent curl (flow-field meander) + a touch of random wobble
                val nx = nodes[ni].x
                val ny = nodes[ni].y
                val curl = SimplexNoise.noise(nx * CURL_SCALE, ny * CURL_SCALE).toFloat() * p.curl
                val angle = curl + rng.rndf(-WOBBLE, WOBBLE)
                val ca = cos(angle); val sa = sin(angle)
                val rx = ux * ca - uy * sa
                val ry = ux * sa + uy * ca
                addNode(Node(nx + rx * STEP_LEN, ny + ry * STEP_LEN, ni, step))
            }
        }
        lastStep = step
    }
}

/** food envelope: fades food out near the top + side frame so the organism crowns organically. */
private fun envelope(x: Float, y: Float): Float {
    val top = smoothstep(H * TOP_FADE0, H * TOP_FADE1, y)              // 0 at the top, 1 lower down
    val sideL = smoothstep(MARGIN, MARGIN + SIDE_FADE, x)
    val sideR = smoothstep(MARGIN, MARGIN + SIDE_FADE, W - x)
    return top * sideL * sideR
}

// ARRIVAL-TIME FIELD (for isochrones)

/** splat each node's birth step into a coarse grid, keeping the earliest arrival per cell. */
private fun buildArrivalField(nodes: List<Node>, splat: Int): FloatArray {
    val fw = W / FS
    val fh = H / FS
    val field = FloatArray(fw * fh) { BIG }
    val r2 = splat * splat
    for (node in nodes) {
        val gx = (node.x / FS).toInt()
        val gy = (node.y / FS).toInt()
        val b = node.birth.toFloat()
        for (dy in -splat..splat) {
            val yy = gy + dy
            if (yy < 0 || yy >= fh) continue
            for (dx in -splat..splat) {
                if (dx * dx + dy * dy > r2) continue
                val xx = gx + dx
                if (xx < 0 || xx >= fw) continue
                val idx = yy * fw + xx
                if (b < field[idx]) field[idx] = b
            }
        }
    }
    return field
}

/** marching squares: emit the iso-line segments of `field` at level `lv` (grid coords). */
private inline fun contourAt(field: FloatArray, fw: Int, fh: Int, lv: Float, emit: (Float, Float, Float, Float) -> Unit) {
    for (gy in 0 until fh - 1) {
        for (gx in 0 until fw - 1) {
            val a = field[gy * fw + gx]                 // top-left
            val b = field[gy * fw + gx + 1]             // top-right
            val cc = field[(gy + 1) * fw + gx + 1]      // bottom-right
            val d = field[(gy + 1) * fw + gx]           // bottom-left
            var case = 0
            if (a > lv) case = case or 8
            if (b > lv) case = case or 4
            if (cc > lv) case = case or 2
            if (d > lv) case = case or 1
            if (case == 0 || case == 15) continue

            val fgx = gx.toFloat()
            val fgy = gy.toFloat()
            val topX = fgx + (lv - a) / (b - a); val topY = fgy           // top edge a-b
            val rightX = fgx + 1f; val rightY = fgy + (lv - b) / (cc - b) // right edge b-c
            val botX = fgx + (lv - d) / (cc - d); val botY = fgy + 1f     // bottom edge d-c
            val leftX = fgx; val leftY = fgy + (lv - a) / (d - a)         // left edge a-d

            when (case) {
                1, 14 -> emit(leftX, leftY, botX, botY)
                2, 13 -> emit(botX, botY, rightX, rightY)
                3, 12 -> emit(leftX, leftY, rightX, rightY)
                4, 11 -> emit(topX, topY, rightX, rightY)
                6, 9 -> emit(topX, topY, botX, botY)
                7, 8 -> emit(topX, topY, leftX, leftY)
                5 -> { emit(topX, topY, rightX, rightY); emit(leftX, leftY, botX, botY) }
                10 -> { emit(topX, topY, leftX, leftY); emit(botX, botY, rightX, rightY) }
            }
        }
    }
}

// RENDER

private fun drawBackground(c: Canvas, gradient: Palette, cyan: Boolean) {
    if (cyan) {
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
        return
    }
    // colorful mode: a near-black vignette subtly tinted by the palette so the glow sits in its own world.
    // darken by scaling RGB channels (preserves hue) - lerpColor toward black drifts through olive.
    val center = darken(gradient.safe((0.45f * (GRAD_STEPS - 1)).toInt()), 0.22f)
    val deep = darken(gradient.safe((0.30f * (GRAD_STEPS - 1)).toInt()), 0.10f)
    c.clear(deep)
    c.drawPaint(Paint().apply {
        shader = makeRadialGradient(
            W * 0.5f, H * 0.62f, W * 0.92f,
            gradientOf(intArrayOf(center, deep, 0xFF050507.toInt()), floatArrayOf(0f, 0.55f, 1f))
        )
    })
}

/** scale a color's RGB channels by [f] (keeps hue, unlike lerp-to-black). */
private fun darken(color: Int, f: Float): Int {
    val r = ((color ushr 16 and 0xFF) * f).toInt().coerceIn(0, 255)
    val g = ((color ushr 8 and 0xFF) * f).toInt().coerceIn(0, 255)
    val b = ((color and 0xFF) * f).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}

/** few random white discs sat under the veins, so the nervatures cross over them here n there.
 *  own rng off the seed so it doesnt touch the growth, but still reproducible. */
private fun drawCircles(c: Canvas, p: Params) {
    if (p.circles == 0) return
    val rng = Random(p.seed * 7919L + 17L)
    val n = if (p.circles > 0) p.circles else rng.nextInt(5, 9)   // 5..8
    val white = 0xFFFFFFFF.toInt()
    repeat(n) {
        val x = rng.rndf(MARGIN, W - MARGIN)
        val y = rng.rndf(MARGIN, H - MARGIN)
        val r = rng.rndf(CIRCLE_R_MIN, CIRCLE_R_MAX)
        // soft outer aura so it glows like the rest of the scene
        c.drawCircle(x, y, r, fillOf(alpha(white, CIRCLE_GLOW_ALPHA)).apply {
            maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, r * CIRCLE_GLOW_BLUR)
        })
        // soft feathered body - reads like a moon, not a sticker
        c.drawCircle(x, y, r * 0.74f, fillOf(alpha(white, CIRCLE_ALPHA)).apply {
            maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, r * CIRCLE_BODY_BLUR)
        })
    }
}

/** how many tips each node feeds = roughly the flow going thru it. */
private fun computeTips(nodes: List<Node>): IntArray {
    val tips = IntArray(nodes.size)
    val hasChild = BooleanArray(nodes.size)
    for (i in nodes.indices) {
        val parent = nodes[i].parent
        if (parent >= 0) hasChild[parent] = true
    }
    for (i in nodes.indices) if (!hasChild[i]) tips[i] = 1
    // parent index is always < child index, so a reverse pass accumulates each subtree's tips.
    for (i in nodes.indices.reversed()) {
        val parent = nodes[i].parent
        if (parent >= 0) tips[parent] += tips[i]
    }
    return tips
}

/** vein thickness: blends flow-width (trunk thick, twigs thin) with age-width (old thick, young thin)
 *  by [ageWeight]. age is 0 (old) .. 1 (young). */
private fun veinWidth(tip: Int, denom: Float, age: Float, ageWeight: Float): Float {
    val flow = (tip.toFloat().pow(1f / THICKNESS_EXP) / denom).coerceIn(0f, 1f)
    val old = 1f - age
    return lerp(MIN_W, MAX_W, lerp(flow, old, ageWeight)).coerceIn(MIN_W, MAX_W)
}

/** redraw the veins as an additive glowing halo + hot core -> a backlit tissue membrane with depth.
 *  thick trunk veins splat a wide halo, thin twigs a small one, so the glow concentrates on flow. */
private fun drawBloom(c: Canvas, nodes: List<Node>, tips: IntArray, p: Params, haloColor: Int, coreColor: Int, maxBirth: Int) {
    val maxTips = (tips.maxOrNull() ?: 1).coerceAtLeast(1)
    val denom = maxTips.toFloat().pow(1f / THICKNESS_EXP)
    val mb = maxBirth.coerceAtLeast(1)
    val order = nodes.indices.filter { nodes[it].parent >= 0 }

    fun pass(glow: Int, widthMul: Float, sigma: Float, baseAlpha: Int) {
        val layerAlpha = (baseAlpha * p.bloom).toInt().coerceIn(0, 255)
        if (layerAlpha <= 0) return
        c.saveLayer(null, Paint().apply {
            color = alpha(0xFFFFFFFF.toInt(), layerAlpha)
            blendMode = BlendMode.PLUS
            imageFilter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP)
        })
        for (i in order) {
            val node = nodes[i]
            val parent = nodes[node.parent]
            val age = node.birth.toFloat() / mb
            c.drawLine(parent.x, parent.y, node.x, node.y, strokeOf(glow, veinWidth(tips[i], denom, age, p.ageWeight) * widthMul).apply {
                strokeCap = PaintStrokeCap.ROUND
            })
        }
        c.restore()
    }

    pass(haloColor, BLOOM_W_HALO, BLOOM_SIGMA_HALO, BLOOM_ALPHA_HALO)
    pass(coreColor, BLOOM_W_CORE, BLOOM_SIGMA_CORE, BLOOM_ALPHA_CORE)
}

/** contour the arrival-time field into faint time-rings: where the growth front stood. */
private fun drawIsochrones(c: Canvas, field: FloatArray, maxBirth: Int, p: Params, gradient: Palette) {
    val fw = W / FS
    val fh = H / FS
    val mb = maxBirth.coerceAtLeast(1)
    for (k in 1..p.isoLevels) {
        val frac = k.toFloat() / (p.isoLevels + 1)
        val level = frac * mb
        // older rings (near the seed) deep, younger rings (near the front) bright
        val color = gradient.safe((lerp(p.ageLo, 1f, frac) * (GRAD_STEPS - 1)).toInt())
        val paint = strokeOf(alpha(color, (255 * p.isoAlpha).toInt().coerceIn(0, 255)), ISO_W).apply {
            strokeCap = PaintStrokeCap.ROUND
            blendMode = BlendMode.SCREEN
        }
        contourAt(field, fw, fh, level) { x1, y1, x2, y2 ->
            c.drawLine(x1 * FS, y1 * FS, x2 * FS, y2 * FS, paint)
        }
    }
}

private fun drawVeins(c: Canvas, nodes: List<Node>, tips: IntArray, maxBirth: Int, gradient: Palette, ageLo: Float, ageWeight: Float) {
    val maxTips = (tips.maxOrNull() ?: 1).coerceAtLeast(1)
    val mb = maxBirth.coerceAtLeast(1)
    val denom = maxTips.toFloat().pow(1f / THICKNESS_EXP)

    // oldest first so freshly-grown bright tips layer on top of the dark trunk
    val order = nodes.indices.filter { nodes[it].parent >= 0 }.sortedBy { nodes[it].birth }
    for (i in order) {
        val node = nodes[i]
        val parent = nodes[node.parent]
        val age = node.birth.toFloat() / mb                       // 0 = old, 1 = young
        val color = gradient.safe((lerp(ageLo, 1f, age) * (GRAD_STEPS - 1)).toInt())
        c.drawLine(parent.x, parent.y, node.x, node.y, strokeOf(color, veinWidth(tips[i], denom, age, ageWeight)).apply {
            strokeCap = PaintStrokeCap.ROUND
        })
    }

    drawLivingEdge(c, nodes, tips, mb)
}

private fun drawLivingEdge(c: Canvas, nodes: List<Node>, tips: IntArray, maxBirth: Int) {
    val cutoff = maxBirth - EDGE_BAND
    for (i in nodes.indices) {
        val node = nodes[i]
        if (node.parent < 0) continue
        if (node.birth < cutoff) continue
        if (tips[i] != 1) continue           // only fresh leaf tips
        val parent = nodes[node.parent]
        // soft glow under the tip
        c.drawLine(parent.x, parent.y, node.x, node.y, strokeOf(alpha(GLOW_COLOR, 110), GLOW_W).apply {
            maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, GLOW_BLUR)
        })
        // crisp near-white core
        c.drawLine(parent.x, parent.y, node.x, node.y, strokeOf(alpha(CORE_COLOR, 210), CORE_W).apply {
            strokeCap = PaintStrokeCap.ROUND
        })
        // the one warm note: ember spark on the very newest tips
        if (node.birth >= maxBirth - 2) {
            c.drawCircle(node.x, node.y, SPARK_R, fillOf(alpha(CyanotypeColors.accent.toInt(), 190)))
        }
    }
}
