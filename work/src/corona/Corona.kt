package work.corona

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.math.smoothstep
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Shader.Companion.makeRadialGradient
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * CORONA
 *
 * Chaotic 2d map iterated a silly number of times (hundreds of millions). The
 * orbit never repeats but it hangs around some spots more then others, and those
 * dense spots ARE the picture. Invariant measure if you want the fancy term.
 *
 * 3rd in the trilogy after rugae + nervure. This one flips the temperature -
 * warm plasma / dying star, one cool blue note on the fastest moving filaments.
 * the others do it the other way round.
 */

private const val W = 1200
private const val H = 1200
private val OUT = System.getProperty("out") ?: "work/corona.png"

private val VOID = 0xFF0C0604.toInt()

// engine
private const val MARGIN = 90f
private const val START_X = 0.1
private const val START_Y = 0.1
private const val WARMUP = 1000              // discard the transient before the orbit settles
private const val BOUNDS_SAMPLES = 1_000_000
private val DEFAULT_SEED = 4L                // locked: the luminous plasma egg (Clifford -1.8,-2.0,-0.5,-0.9)

private const val SAMPLES = 240_000_000L     // total orbit iterations across all threads
private const val THREADS = 8                // fixed for reproducible partitioning

// render
private const val GRAD_STEPS = 512
private const val GAMMA = 0.45f              // lifts the smoke into the visible range
private val COOL = 0xFF6FB7FF.toInt()        // the one cool note (inverted spark)
private const val ACCENT_CUTOFF = 0.6f       // velocity fraction where the cool note begins
private const val ACCENT_STRENGTH = 0.65f    // max cool blend
private const val BLOOM_SIGMA = 9f           // glow radius

/** solar ember ramp, dark void up to near-white hot. */
private val SOLAR = Palette(
    0xFF0C0604L, // void (matches VOID)
    0xFF2A0B06L, // smoulder
    0xFF6E1E0AL, // deep maroon
    0xFFB54417L, // ember
    0xFFE87A1EL, // amber-orange
    0xFFFBC56AL, // hot amber
    0xFFFFF6E8L, // incandescent near-white
)

/** handpicked attractor params that actually look good. seed picks one. */
private val PARAMS = listOf(
    // Clifford: x' = sin(a*y) + c*cos(a*x), y' = sin(b*x) + d*cos(b*y)
    Params(true, -1.4, 1.6, 1.0, 0.7),
    Params(true, 1.6, -0.6, -1.2, 1.6),
    Params(true, 1.7, 1.7, 0.6, 1.2),
    Params(true, -1.7, 1.3, -0.1, -1.21),
    Params(true, -1.8, -2.0, -0.5, -0.9),
    // De Jong: x' = sin(a*y) - cos(b*x), y' = sin(c*x) - cos(d*y)
    Params(false, -2.0, -2.0, -1.2, 2.0),
    Params(false, 1.4, -2.3, 2.4, -2.1),
    Params(false, -2.7, -0.09, -0.86, -2.2),
    Params(false, 2.01, -2.53, 1.61, -0.33),
    Params(false, -0.827, -1.637, 1.659, -0.943),
)

fun main() {
    val headless = System.getProperty("headless") != null

    val gart = Gart.of("corona", W, H)
    println(gart)

    val g = gart.gartvas()

    val p = selectParams()
    val fit = fitOf(computeBounds(p))
    println("params: clifford=${p.clifford} a=${p.a} b=${p.b} c=${p.c} d=${p.d}")

    val t0 = System.currentTimeMillis()
    val field = grow(p, fit)
    var maxD = 0f; var nonZero = 0; var maxV = 0f
    for (i in field.density.indices) {
        val dd = field.density[i]
        if (dd > 0f) { nonZero++; if (dd > maxD) maxD = dd; val v = field.velSum[i] / dd; if (v > maxV) maxV = v }
    }
    println("grow: maxD=$maxD nonZero=$nonZero maxV=$maxV in ${System.currentTimeMillis() - t0}ms")

    val map = Gartmap(g.d)
    colorize(field, map)
    applyAccent(field, map)
    map.drawToCanvas(g)

    val c = g.canvas
    drawBloom(c, map)
    drawVignette(c)

    gart.saveImage(g, OUT)
    if (!headless) gart.window().showImage(g)
}

// SIMULATION

private class Params(val clifford: Boolean, val a: Double, val b: Double, val c: Double, val d: Double)

private fun selectParams(): Params {
    val seed = System.getProperty("seed")?.toLong() ?: DEFAULT_SEED
    val idx = (seed % PARAMS.size).toInt().let { if (it < 0) it + PARAMS.size else it }
    return PARAMS[idx]
}

private fun nextX(p: Params, x: Double, y: Double): Double =
    if (p.clifford) sin(p.a * y) + p.c * cos(p.a * x) else sin(p.a * y) - cos(p.b * x)

private fun nextY(p: Params, x: Double, y: Double): Double =
    if (p.clifford) sin(p.b * x) + p.d * cos(p.b * y) else sin(p.c * x) - cos(p.d * y)

/** run the orbit a while to see how big it gets (bbox). */
private fun computeBounds(p: Params): DoubleArray {
    var x = START_X
    var y = START_Y
    repeat(WARMUP) {
        val nx = nextX(p, x, y); val ny = nextY(p, x, y); x = nx; y = ny
    }
    var minX = Double.MAX_VALUE; var maxX = -Double.MAX_VALUE
    var minY = Double.MAX_VALUE; var maxY = -Double.MAX_VALUE
    repeat(BOUNDS_SAMPLES) {
        val nx = nextX(p, x, y); val ny = nextY(p, x, y)
        if (nx < minX) minX = nx; if (nx > maxX) maxX = nx
        if (ny < minY) minY = ny; if (ny > maxY) maxY = ny
        x = nx; y = ny
    }
    return doubleArrayOf(minX, maxX, minY, maxY)
}

private class Fit(val scale: Double, val offX: Double, val offY: Double)

/** squeeze the bbox into the frame, keep aspect + center it. */
private fun fitOf(bounds: DoubleArray): Fit {
    val (minX, maxX, minY, maxY) = bounds
    val spanX = (maxX - minX).coerceAtLeast(1e-9)
    val spanY = (maxY - minY).coerceAtLeast(1e-9)
    val scale = min((W - 2 * MARGIN) / spanX, (H - 2 * MARGIN) / spanY)
    val offX = W / 2.0 - scale * (minX + maxX) / 2.0
    val offY = H / 2.0 - scale * (minY + maxY) / 2.0
    return Fit(scale, offX, offY)
}

private fun splat(px: Double, py: Double, w: Float, density: FloatArray, velSum: FloatArray) {
    val x0 = floor(px).toInt()
    val y0 = floor(py).toInt()
    if (x0 < 0 || y0 < 0 || x0 >= W - 1 || y0 >= H - 1) return
    val fx = (px - x0).toFloat()
    val fy = (py - y0).toFloat()
    val w00 = (1 - fx) * (1 - fy)
    val w10 = fx * (1 - fy)
    val w01 = (1 - fx) * fy
    val w11 = fx * fy
    val i = y0 * W + x0
    density[i] += w00; velSum[i] += w00 * w
    density[i + 1] += w10; velSum[i + 1] += w10 * w
    density[i + W] += w01; velSum[i + W] += w01 * w
    density[i + W + 1] += w11; velSum[i + W + 1] += w11 * w
}

private fun accumulate(p: Params, fit: Fit, startX: Double, iters: Long, density: FloatArray, velSum: FloatArray) {
    val a = p.a; val b = p.b; val c = p.c; val d = p.d; val cl = p.clifford
    var x = startX; var y = START_Y
    repeat(WARMUP) {
        val nx = if (cl) sin(a * y) + c * cos(a * x) else sin(a * y) - cos(b * x)
        val ny = if (cl) sin(b * x) + d * cos(b * y) else sin(c * x) - cos(d * y)
        x = nx; y = ny
    }
    var i = 0L
    while (i < iters) {
        val nx = if (cl) sin(a * y) + c * cos(a * x) else sin(a * y) - cos(b * x)
        val ny = if (cl) sin(b * x) + d * cos(b * y) else sin(c * x) - cos(d * y)
        val sl = hypot(nx - x, ny - y).toFloat()
        splat(fit.offX + fit.scale * nx, fit.offY + fit.scale * ny, sl, density, velSum)
        x = nx; y = ny
        i++
    }
}

private class Field(val density: FloatArray, val velSum: FloatArray)

private fun grow(p: Params, fit: Fit): Field {
    val perThread = SAMPLES / THREADS
    val parts = arrayOfNulls<Field>(THREADS)
    val threads = (0 until THREADS).map { t ->
        Thread {
            val dens = FloatArray(W * H)
            val vel = FloatArray(W * H)
            // distinct fixed start per worker -> reproducible, same invariant measure
            accumulate(p, fit, START_X + t * 0.0001, perThread, dens, vel)
            parts[t] = Field(dens, vel)
        }
    }
    threads.forEach { it.start() }
    threads.forEach { it.join() }

    val density = FloatArray(W * H)
    val velSum = FloatArray(W * H)
    for (t in 0 until THREADS) {              // merge in fixed worker order
        val f = parts[t]!!
        for (i in density.indices) { density[i] += f.density[i]; velSum[i] += f.velSum[i] }
    }
    return Field(density, velSum)
}

// RENDER

private fun colorize(field: Field, map: Gartmap) {
    val density = field.density
    val px = map.pixels
    var maxD = 0f
    for (dd in density) if (dd > maxD) maxD = dd
    val logMax = ln(1.0 + maxD).coerceAtLeast(1e-9)
    val ramp = SOLAR.expand(GRAD_STEPS)
    for (i in density.indices) {
        val dd = density[i]
        if (dd <= 0f) { px[i] = VOID; continue }
        var t = (ln(1.0 + dd) / logMax).toFloat()
        t = t.pow(GAMMA).coerceIn(0f, 1f)
        px[i] = ramp.safe((t * (GRAD_STEPS - 1)).toInt())
    }
}

/** push the fastest filaments toward cool blue, thats the inverted spark. */
private fun applyAccent(field: Field, map: Gartmap) {
    val density = field.density
    val velSum = field.velSum
    val px = map.pixels
    var maxV = 0f
    for (i in density.indices) if (density[i] > 0f) {
        val v = velSum[i] / density[i]; if (v > maxV) maxV = v
    }
    if (maxV <= 0f) return
    for (i in density.indices) {
        val dd = density[i]
        if (dd <= 0f) continue
        val vn = (velSum[i] / dd / maxV).coerceIn(0f, 1f)
        val cool = smoothstep(ACCENT_CUTOFF, 1f, vn) * ACCENT_STRENGTH
        if (cool > 0f) px[i] = lerpColor(px[i], COOL, cool)
    }
}

/** blurry copy on top so the hot core glows. */
private fun drawBloom(c: Canvas, map: Gartmap) {
    c.drawImage(map.image(), 0f, 0f, Paint().apply {
        imageFilter = ImageFilter.makeBlur(BLOOM_SIGMA, BLOOM_SIGMA, FilterTileMode.CLAMP)
        blendMode = BlendMode.SCREEN
    })
}

/** darken the edges a bit for depth. */
private fun drawVignette(c: Canvas) {
    c.drawPaint(Paint().apply {
        shader = makeRadialGradient(
            W * 0.5f, H * 0.5f, W * 0.72f,
            gradientOf(
                intArrayOf(0x00000000, 0x00000000, 0xCC080402.toInt()),
                floatArrayOf(0f, 0.62f, 1f)
            )
        )
    })
}
