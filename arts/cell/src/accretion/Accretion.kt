package accretion

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palette
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.noise.PoissonDiskSamplingNoise
import org.jetbrains.skia.*
import kotlin.math.pow
import kotlin.random.Random

/**
 * Diffusion-limited aggregation, packed colonies. A handful of seeds are scattered at
 * random; a bath of thousands of wanderers random-walks a toroidal lattice and freezes the
 * instant it touches frozen matter. Each seed grows a branching dendritic fern; colonies
 * collide and tile the whole frame, the black channels between them the boundaries where
 * they competed for walkers. Nothing is placed - every fern, every channel, emerges from
 * random-walk plus contact-freezing alone.
 */

private const val W = 1200
private const val H = 1200


private val SCALE = pi("scale", 1)             // 1 = fine detail (slower); 2 = quick draft
private val GW = W / SCALE
private val GH = H / SCALE

private val SEED = pi("seed", 21)
private val SEED_COUNT = pi("seeds", 8)
private val SEEDMODE = ps("seedmode", "scatter")  // "scatter" (clumpy random) or "poisson" (evenly)
private val WALKERS = pi("walkers", 30000)
private val STICKINESS = pf("stick", 0.8f)     // < 1 walkers penetrate "fjords" : denser, less stringy
private val TARGET_FILL = pf("fill", 0.46f)    // fraction of cells frozen; lower leaves the black channels
private val GAMMA = pf("gamma", 1.0f)          // >1 widens teal area, ember for the tips
private val SMOOTH = pi("smooth", 4)           // masked age-field blur radius in cells (0 = raw/speckled)
private val RANGE = pf("range", 0.12f)         // bilateral range as a fraction of maxAge (0 = plain box blur)
private val DOT = pf("dot", 0.9f)              // dot radius. fraction of SCALE
private val BLOOM = pi("bloom", 0x66)          // bloom strength 0..255 (0 = off)
private const val MAX_TICKS = 400_000

private const val EMPTY = -1

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val out = ps("out", "accretion")
    val gart = Gart.of("accretion", W, H)
    println(gart)

    val dla = Dla(Random(SEED.toLong()))
    val frozen = dla.run()
    println("frozen: $frozen / ${GW * GH} (${(100f * frozen / (GW * GH)).toInt()}%), maxAge: ${dla.maxAge}")

    val g = gart.gartvas()
    paint(g, dla)

    gart.saveImage(g, "$out.png")
    if (!headless) gart.window().showImage(g)
}

// SIMULATION

private class Dla(private val rng: Random) {
    // EMPTY, or freeze order (>=0): the global tick-counter when the cell froze. Backbone freezes
    // early (low), infill pockets late (high) - that contrast traces the branches. Rendered through
    // a masked spatial smoothing so the per-cell speckle is denoised but the branch structure stays.
    val state = IntArray(GW * GH) { EMPTY }
    private val wx = IntArray(WALKERS)
    private val wy = IntArray(WALKERS)
    private val walkLen = IntArray(WALKERS)
    private var counter = 0
    val maxAge get() = counter

    // 8 lattice directions
    private val dirX = intArrayOf(1, 1, 0, -1, -1, -1, 0, 1)
    private val dirY = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)

    fun run(): Int {
        var frozen = seed()
        counter = 1                                  // seeds age 0; first walker freeze is age 1
        for (i in 0 until WALKERS) respawn(i)

        val target = (TARGET_FILL * GW * GH).toInt()
        val maxWalk = 8 * (GW + GH)
        val stallWindow = 600
        val stallDelta = (0.0003f * GW * GH).toInt().coerceAtLeast(1)  // min new cells per window
        val warmup = (0.12f * GW * GH).toInt()                          // ignore stalls before this (slow start)
        var ticks = 0
        var lastCheck = frozen
        while (frozen < target && ticks < MAX_TICKS) {
            for (i in 0 until WALKERS) {
                if (step(i, maxWalk)) frozen++
            }
            ticks++
            if (ticks % stallWindow == 0) {
                if (frozen > warmup && frozen - lastCheck < stallDelta) break  // saturated, not slow start
                lastCheck = frozen
            }
        }
        println("ticks: $ticks")
        return frozen
    }

    /** Scatter seeds as little discs; returns the number of frozen cells. */
    private fun seed(): Int {
        val seeds = if (SEEDMODE == "poisson") {
            PoissonDiskSamplingNoise(SEED.toLong())
                .generate(0.0, 0.0, GW.toDouble(), GH.toDouble(), SEED_COUNT)
                .map { it.x.toInt() to it.y.toInt() }
        } else {
            // clumpy random scatter: uneven spacing > some big colonies, some small (scale variation)
            List(SEED_COUNT) { rng.nextInt(GW) to rng.nextInt(GH) }
        }
        var n = 0
        for ((cx, cy) in seeds) n += stampDisc(cx, cy, 2)
        return n
    }

    private fun stampDisc(cx: Int, cy: Int, r: Int): Int {
        var n = 0
        for (oy in -r..r) for (ox in -r..r) {
            if (ox * ox + oy * oy > r * r) continue
            val x = wrap(cx + ox, GW)
            val y = wrap(cy + oy, GH)
            val idx = y * GW + x
            if (state[idx] == EMPTY) {
                state[idx] = 0
                n++
            }
        }
        return n
    }

    // micro step for walker [i]; returns true iff it froze a new cell
    private fun step(i: Int, maxWalk: Int): Boolean {
        val idx = wy[i] * GW + wx[i]
        if (state[idx] != EMPTY) { // another walker froze under us
            respawn(i)
            return false
        }
        val dir = rng.nextInt(8)
        val nx = wrap(wx[i] + dirX[dir], GW)
        val ny = wrap(wy[i] + dirY[dir], GH)
        val nidx = ny * GW + nx

        if (state[nidx] != EMPTY) {  // bumped the cluster
            if (rng.nextFloat() < STICKINESS) {
                state[idx] = counter++
                respawn(i)
                return true
            }
            // didn't stick: bounce, occasionally relocate so we don't loop! in a pocket
            if (++walkLen[i] > maxWalk) respawn(i)
            return false
        }
        wx[i] = nx
        wy[i] = ny
        if (++walkLen[i] > maxWalk) respawn(i)
        return false
    }

    private fun respawn(i: Int) {
        repeat(64) {
            val x = rng.nextInt(GW)
            val y = rng.nextInt(GH)
            if (state[y * GW + x] == EMPTY) {
                wx[i] = x; wy[i] = y; walkLen[i] = 0
                return
            }
        }
        wx[i] = rng.nextInt(GW); wy[i] = rng.nextInt(GH); walkLen[i] = 0
    }

    private fun wrap(v: Int, m: Int) = if (v < 0) v + m else if (v >= m) v - m else v
}


private val PAL = ps("pal", "reef")
private val palette = when (PAL) {
    "deep" -> Palette.of(
        "#03070a", "#051418", "#08312f", "#0c5b52", "#149083",
        "#52b89a", "#ecc25a", "#e2802e", "#d24a26", "#c2331f",
    )
    "ember" -> Palette.of(
        "#06141a", "#0a4742", "#179079", "#7cc59a", "#e7c873",
        "#e69a36", "#df6f29", "#d2401f", "#b8281a",
    )
    "lumen" -> Palette.of(
        "#15605c", "#1f8f82", "#34b394", "#83cf9c", "#dfd089",
        "#ecab43", "#e3812f", "#d24a26", "#c23320",
    )
    "reef" -> Palette.of(
        "#0a4a46", "#0d7468", "#149083", "#1fb39a", "#56c8a4",
        "#9bd6a0", "#e9b24a", "#e2802e", "#d24a26", "#c0301d",
    )
    else -> Palette.of(
        "#06181c", "#0c5450", "#1f9e86", "#79c9a6", "#f0cf8e",
        "#e89b34", "#df6a2a", "#cf3b27",
    )
}.expand(1024)

private const val BG = 0xFF08090C.toInt()
private const val FRONTIER_T = 0.965f    // top fraction lit as the living edge
private const val CREAM = 0xFFF6EFE0.toInt()

private fun paint(g: Gartvas, dla: Dla) {
    val c = g.canvas
    c.clear(BG)

    val maxAge = dla.maxAge.toFloat().coerceAtLeast(1f)
    val gradSize = palette.size
    val smoothed = smoothAge(dla.state, maxAge)    // masked blur: denoise speckle, keep branch contrast

    // base layer: every frozen cell as a soft dot, colored by its smoothed freeze-age
    val dot = Paint().apply { isAntiAlias = true }
    val r = SCALE * DOT
    var i = 0
    for (gy in 0 until GH) for (gx in 0 until GW) {
        val a = smoothed[i++]
        if (a < 0f) continue
        val t = (a / maxAge).pow(GAMMA)
        dot.color = palette.bound(t * (gradSize - 1))
        c.drawCircle(gx * SCALE + SCALE * 0.5f, gy * SCALE + SCALE * 0.5f, r, dot)
    }

    // blur a copy of the structure and add it back, lighting from within
    if (BLOOM > 0) {
        val bloom = Paint().apply {
            imageFilter = ImageFilter.makeBlur(9f, 9f, FilterTileMode.CLAMP)
            colorFilter = ColorFilter.makeBlend(Color.makeARGB(BLOOM, 255, 255, 255), BlendMode.MODULATE)
            blendMode = BlendMode.PLUS
        }
        c.drawImage(g.snapshot(), 0f, 0f, bloom)
    }

    // the living frontier: where colonies collide last, the SMOOTHED age is highest and coherent
    // (the ember). Light only those using the smoothed field means isolated late pockets
    // get averaged down and don't sprinkle specks, so the suture glow stays clean
    val halo = Paint().apply {
        isAntiAlias = true
        blendMode = BlendMode.PLUS
        maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, 3f)
    }
    val tip = Paint().apply { isAntiAlias = true }
    var j = 0
    for (gy in 0 until GH) for (gx in 0 until GW) {
        val a = smoothed[j++]
        if (a < 0f) continue
        val t = a / maxAge
        if (t < FRONTIER_T) continue
        val cx = gx * SCALE + SCALE * 0.5f
        val cy = gy * SCALE + SCALE * 0.5f
        val k = (t - FRONTIER_T) / (1f - FRONTIER_T)        // 0..1 across the frontier band
        halo.color = withAlpha(palette.bound((0.92f + 0.08f * k) * (gradSize - 1)), (30 + 90 * k).toInt())
        c.drawCircle(cx, cy, SCALE * (1.1f + 0.7f * k), halo)
        if (t > 0.99f) {
            tip.color = withAlpha(CREAM, (40 + 120 * k).toInt())
            c.drawCircle(cx, cy, SCALE * 0.55f, tip)
        }
    }
}

private fun smoothAge(state: IntArray, maxAge: Float): FloatArray {
    val out = FloatArray(GW * GH)
    val rad = SMOOTH
    if (rad <= 0) {
        for (i in state.indices) out[i] = if (state[i] == EMPTY) -1f else state[i].toFloat()
        return out
    }
    val range = if (RANGE > 0f) RANGE * maxAge else Float.MAX_VALUE
    var idx = 0
    for (gy in 0 until GH) for (gx in 0 until GW) {
        val cv = state[idx]
        if (cv == EMPTY) { out[idx] = -1f; idx++; continue }
        var sum = 0L
        var cnt = 0
        var oy = -rad
        while (oy <= rad) {
            val y = gy + oy
            if (y in 0 until GH) {
                val base = y * GW
                var ox = -rad
                while (ox <= rad) {
                    val x = gx + ox
                    if (x in 0 until GW) {
                        val v = state[base + x]
                        if (v != EMPTY && (v - cv).toFloat().let { if (it < 0) -it else it } <= range) {
                            sum += v; cnt++
                        }
                    }
                    ox++
                }
            }
            oy++
        }
        out[idx] = if (cnt > 0) sum.toFloat() / cnt else cv.toFloat()
        idx++
    }
    return out
}

private fun withAlpha(color: Int, a: Int) =
    Color.makeARGB(a.coerceIn(0, 255), Color.getR(color), Color.getG(color), Color.getB(color))
