package work.corona

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.smoothstep
import org.jetbrains.skia.*
import org.jetbrains.skia.Shader.Companion.makeRadialGradient
import kotlin.math.*

/**
 * CORONA
 *
 * Chaotic 2d map iterated a silly number of times (hundreds of millions). The
 * orbit never repeats but it hangs around some spots more then others, and those
 * dense spots ARE the picture. Invariant measure if you want the fancy term.
 *
 * 3rd in the trilogy after rugae + nervure. This one flips the temperature -
 * warm plasma / dying star. two emergent colour cues, both read off the field:
 *   - density paints a temperature gradient, cold faint outer corona -> white-hot core
 *   - the fastest filament edges pick up a cool electric glint (the inverted spark)
 * the others do it the other way round.
 *
 * rendered at SSx supersample so the filaments come out as crisp light, not smoke,
 * then a multi-octave bloom blows the corona out into the void like the name says.
 */

private const val W = 1200
private const val H = 1200

// knobs (sweepable via -Dkey=value, recorded by the io helpers)
private val OUT = ps("out", "work/corona")
private val SEED = pi("seed", 4)                 // which attractor (locked 4 = the luminous plasma egg)
private val SS = pi("ss", 3)                      // supersample factor: render SSx then box-shrink (1=fast preview)
private val PAL = pi("pal", 0)                    // palette / temperature toe

private val GAMMA = pf("gamma", 0.52f)           // less lift -> hot filaments over a cool ground (temperature reads)
private val BLACK = pf("black", 0.05f)           // black point - clips the faint noise floor to clean void
private val ACCENT_CUTOFF = pf("acut", 0.74f)    // velocity fraction where the cool glints begin
private val ACCENT_STRENGTH = pf("astr", 0.6f)   // max cool glint blend
private val BLOOM = pf("bloom", 0.8f)            // master glow strength
private val BLOOM_SIGMA = pf("bsig", 7f)         // base glow radius

// engine
private const val MARGIN = 90f
private const val START_X = 0.1
private const val START_Y = 0.1
private const val WARMUP = 1000                  // discard the transient before the orbit settles
private const val BOUNDS_SAMPLES = 1_000_000
private const val SAMPLES = 240_000_000L         // total orbit iterations across all threads
private const val THREADS = 8                    // fixed for reproducible partitioning

// supersampled render resolution
private val RW = W * SS
private val RH = H * SS
private val RMARGIN = MARGIN * SS

// render
private const val GRAD_STEPS = 512
private val VOID = 0xFF060510.toInt()            // cool near-black void
private val COOL = 0xFF9FD8FF.toInt()            // bright electric glint on the fastest filaments

/**
 * cold -> hot temperature ramps, void up to incandescent. the bottom of each ramp is the
 * cool note: faint (low density) regions read cold, dense cores read white-hot. PAL picks one.
 */
private val PALETTES = listOf(
    // 0: steel-blue cold corona -> ember -> incandescent (the default trilogy look)
    Palette(0xFF060510L, 0xFF132640L, 0xFF2B3346L, 0xFF6E1E0AL, 0xFFB54417L, 0xFFE87A1EL, 0xFFFBC56AL, 0xFFFFF6E8L),
    // 1: violet dusk toe -> ember -> white (moodier, nebula-ish)
    Palette(0xFF080510L, 0xFF231A4EL, 0xFF4A2140L, 0xFF8A2A18L, 0xFFC25320L, 0xFFEE8A26L, 0xFFFCD080L, 0xFFFFF8EEL),
    // 2: pure warm ember, no cool toe (the cool note then comes only from the velocity glints)
    Palette(0xFF0C0604L, 0xFF2A0B06L, 0xFF6E1E0AL, 0xFFB54417L, 0xFFE87A1EL, 0xFFFBC56AL, 0xFFFFF6E8L),
    // 3: teal cold -> hot white core ("blue corona", coldest reading)
    Palette(0xFF040810L, 0xFF0E2E3AL, 0xFF1C5A55L, 0xFF8A3A14L, 0xFFD06A1CL, 0xFFF2A23AL, 0xFFFBD68EL, 0xFFFFFAF0L),
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

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)

    val gart = Gart.of("corona", W, H)
    println(gart)

    val g = gart.gartvas()

    val p = selectParams()
    val fit = fitOf(computeBounds(p))
    println("params: clifford=${p.clifford} a=${p.a} b=${p.b} c=${p.c} d=${p.d}  ss=$SS pal=$PAL")

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
    drawBloom(c, map.image())
    drawVignette(c)

    gart.saveImage(g, "$OUT.png")
    if (!headless) gart.window().showImage(g)
}

// SIMULATION

private class Params(val clifford: Boolean, val a: Double, val b: Double, val c: Double, val d: Double)

private fun selectParams(): Params {
    val idx = (SEED % PARAMS.size).let { if (it < 0) it + PARAMS.size else it }
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

/** squeeze the bbox into the (supersampled) frame, keep aspect + center it. */
private fun fitOf(bounds: DoubleArray): Fit {
    val (minX, maxX, minY, maxY) = bounds
    val spanX = (maxX - minX).coerceAtLeast(1e-9)
    val spanY = (maxY - minY).coerceAtLeast(1e-9)
    val scale = min((RW - 2 * RMARGIN) / spanX, (RH - 2 * RMARGIN) / spanY)
    val offX = RW / 2.0 - scale * (minX + maxX) / 2.0
    val offY = RH / 2.0 - scale * (minY + maxY) / 2.0
    return Fit(scale, offX, offY)
}

private fun splat(px: Double, py: Double, w: Float, density: FloatArray, velSum: FloatArray) {
    val x0 = floor(px).toInt()
    val y0 = floor(py).toInt()
    if (x0 < 0 || y0 < 0 || x0 >= RW - 1 || y0 >= RH - 1) return
    val fx = (px - x0).toFloat()
    val fy = (py - y0).toFloat()
    val w00 = (1 - fx) * (1 - fy)
    val w10 = fx * (1 - fy)
    val w01 = (1 - fx) * fy
    val w11 = fx * fy
    val i = y0 * RW + x0
    density[i] += w00; velSum[i] += w00 * w
    density[i + 1] += w10; velSum[i + 1] += w10 * w
    density[i + RW] += w01; velSum[i + RW] += w01 * w
    density[i + RW + 1] += w11; velSum[i + RW + 1] += w11 * w
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
            val dens = FloatArray(RW * RH)
            val vel = FloatArray(RW * RH)
            // distinct fixed start per worker -> reproducible, same invariant measure
            accumulate(p, fit, START_X + t * 0.0001, perThread, dens, vel)
            parts[t] = Field(dens, vel)
        }
    }
    threads.forEach { it.start() }
    threads.forEach { it.join() }

    val density = FloatArray(RW * RH)
    val velSum = FloatArray(RW * RH)
    for (t in 0 until THREADS) {                  // merge in fixed worker order
        val f = parts[t]!!
        for (i in density.indices) { density[i] += f.density[i]; velSum[i] += f.velSum[i] }
    }
    return downsample(Field(density, velSum))
}

/** box-shrink the supersampled field down to WxH by summing each SSxSS block (thats the AA). */
private fun downsample(hi: Field): Field {
    if (SS == 1) return hi
    val d = FloatArray(W * H)
    val v = FloatArray(W * H)
    for (y in 0 until H) {
        val by = y * SS
        for (x in 0 until W) {
            val bx = x * SS
            var sd = 0f;
            var sv = 0f
            for (yy in 0 until SS) {
                var row = (by + yy) * RW + bx
                for (xx in 0 until SS) {
                    sd += hi.density[row]; sv += hi.velSum[row]; row++
                }
            }
            val o = y * W + x
            d[o] = sd; v[o] = sv
        }
    }
    return Field(d, v)
}

// RENDER

private fun colorize(field: Field, map: Gartmap) {
    val density = field.density
    val px = map.pixels
    var maxD = 0f
    for (dd in density) if (dd > maxD) maxD = dd
    val logMax = ln(1.0 + maxD).coerceAtLeast(1e-9)
    val ramp = PALETTES[PAL.coerceIn(0, PALETTES.size - 1)].expand(GRAD_STEPS)
    for (i in density.indices) {
        val dd = density[i]
        if (dd <= 0f) { px[i] = VOID; continue }
        var t = (ln(1.0 + dd) / logMax).toFloat()
        // black point clips the noise floor, then we renormalize + lift -> clean darks, visible smoke
        t = ((t - BLACK) / (1f - BLACK)).coerceIn(0f, 1f)
        t = t.pow(GAMMA)
        px[i] = ramp.safe((t * (GRAD_STEPS - 1)).toInt())
    }
}

/**
 * cool electric glints on the fastest filaments (the inverted spark). gated so they ride the
 * thin fast edges and DON'T pool into a flat wash over the dense slow core.
 */
private fun applyAccent(field: Field, map: Gartmap) {
    val density = field.density
    val velSum = field.velSum
    val px = map.pixels
    var maxV = 0f;
    var maxD = 0f
    for (i in density.indices) if (density[i] > 0f) {
        val v = velSum[i] / density[i]; if (v > maxV) maxV = v
        if (density[i] > maxD) maxD = density[i]
    }
    if (maxV <= 0f) return
    val logMax = ln(1.0 + maxD).coerceAtLeast(1e-9)
    for (i in density.indices) {
        val dd = density[i]
        if (dd <= 0f) continue
        val vn = (velSum[i] / dd / maxV).coerceIn(0f, 1f)
        val dn = (ln(1.0 + dd) / logMax).toFloat()
        val edge = 1f - smoothstep(0.62f, 0.92f, dn)            // fade out where its dense + bright
        val cool = smoothstep(ACCENT_CUTOFF, 1f, vn) * ACCENT_STRENGTH * edge
        if (cool > 0f) px[i] = lerpColor(px[i], COOL, cool)
    }
}

/** multi-octave bloom: tight core glow + a couple of wide additive halos = the corona. */
private fun drawBloom(c: Canvas, img: Image) {
    bloomOctave(c, img, BLOOM_SIGMA * 0.7f, 0.55f * BLOOM, BlendMode.SCREEN)  // tight core glow
    bloomOctave(c, img, BLOOM_SIGMA * 2.0f, 0.40f * BLOOM, BlendMode.PLUS)    // mid halo, additive
    bloomOctave(c, img, BLOOM_SIGMA * 5.0f, 0.22f * BLOOM, BlendMode.PLUS)    // wide corona into the void
}

private fun bloomOctave(c: Canvas, img: Image, sigma: Float, strength: Float, blend: BlendMode) {
    c.drawImage(img, 0f, 0f, Paint().apply {
        imageFilter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP)
        blendMode = blend
        alpha = (strength.coerceIn(0f, 1f) * 255).toInt()
    })
}

/** darken the edges a bit for depth. */
private fun drawVignette(c: Canvas) {
    c.drawPaint(Paint().apply {
        shader = makeRadialGradient(
            W * 0.5f, H * 0.5f, W * 0.74f,
            gradientOf(
                intArrayOf(0x00000000, 0x00000000, 0xDD050409.toInt()),
                floatArrayOf(0f, 0.6f, 1f)
            )
        )
    })
}
