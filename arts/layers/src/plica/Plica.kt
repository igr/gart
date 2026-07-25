package plica

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.argb
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.pl
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.HALF_PIf
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.between
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Shader
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * stack of ribbons, but cool
 */
private const val W = 900
private const val H = 1350
private const val HARMONICS = 3

private const val OVERLAP = 3f // to prevent AA hairline can show.

private data class Params(
    val seed: Long,
    val out: String,
    val ss: Int,
    val bands: Int,
    val fold: Float,
    val foldJitter: Float,
    val wave: Float,
    val freq: Float,
    val skew: Float,
    val arc: Float,
    val env: Float,
    val drift: Float,
    val stray: Float,
    val taper: Float,
    val runLength: Int,
    val cool: Int,
    val shade: Float,
    val shadowY: Float,
    val shadowBlur: Float,
    val shadowAlpha: Float,
    val grain: Float,
    val vignette: Float,
)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val p = resolveParams()
    val gart = Gart.of("plica", W, H)

    println(gart)
    println(
        "seed=${p.seed} bands=${p.bands} fold=${p.fold}±${p.foldJitter} wave=${p.wave} " +
            "freq=${p.freq} skew=${p.skew} arc=${p.arc} env=${p.env} drift=${p.drift} stray=${p.stray} " +
            "run=${p.runLength} cool=${p.cool} shade=${p.shade}",
    )

    val big = Gartvas(Dimension(W * p.ss, H * p.ss))
    big.canvas.scale(p.ss.toFloat(), p.ss.toFloat())
    render(big.canvas, p)

    val g = gart.gartvas()
    val snapshot = big.snapshot()
    g.canvas.drawImageRect(
        snapshot,
        Rect.makeWH((W * p.ss).toFloat(), (H * p.ss).toFloat()),
        Rect.makeWH(W.toFloat(), H.toFloat()),
        SamplingMode.MITCHELL,
        null,
        true,
    )
    snapshot.close()

    if (p.vignette > 0f) g.canvas.drawVignette(g.d, p.vignette, radius = 0.88f, innerStop = 0.55f)
    if (p.grain > 0f) addGrain(g, p.grain, p.seed.toInt())

    val output = if (p.out.endsWith(".png", ignoreCase = true)) p.out else "${p.out}.png"
    gart.saveImage(g, output)
    if (!headless) gart.window().showImage(g)
}

private fun resolveParams(): Params {
    val p = Params(
        seed = pl("seed", 2024L),
        out = ps("out", "work/plica"),
        ss = pi("ss", 2),
        bands = pi("bands", 10),
        fold = pf("fold", 0.50f),
        foldJitter = pf("foldjitter", 0.22f),
        wave = pf("wave", 180f),
        freq = pf("freq", 0.85f),
        skew = pf("skew", 0.40f),
        arc = pf("arc", 0.45f),
        env = pf("env", 0.35f),
        drift = pf("drift", 0.09f),
        stray = pf("stray", 0.30f),
        taper = pf("taper", 0.45f),
        runLength = pi("run", 2),
        cool = pi("cool", 0),
        shade = pf("shade", 0.30f),
        shadowY = pf("shadowy", 14f),
        shadowBlur = pf("shadowblur", 20f),
        shadowAlpha = pf("shadowalpha", 0.62f),
        grain = pf("grain", 0.012f),
        vignette = pf("vignette", 0.10f),
    )

    require(p.ss in 1..4) { "ss must be between 1 and 4" }
    require(p.bands in 4..48) { "bands must be between 4 and 48" }
    require(p.fold in 0.05f..0.95f) { "fold must be between 0.05 and 0.95" }
    require(p.foldJitter in 0f..0.5f) { "foldjitter must be between 0 and 0.5" }
    require(p.wave in 0f..420f) { "wave must be between 0 and 420" }
    require(p.freq in 0.25f..4f) { "freq must be between 0.25 and 4" }
    require(p.skew in -0.9f..0.9f) { "skew must be between -0.9 and 0.9" }
    require(p.arc in 0f..1f) { "arc must be between 0 and 1" }
    require(p.env in 0f..0.9f) { "env must be between 0 and 0.9" }
    require(p.drift in -0.2f..0.2f) { "drift must be between -0.2 and 0.2" }
    require(p.stray in 0f..1f) { "stray must be between 0 and 1" }
    require(p.taper in 0f..0.8f) { "taper must be between 0 and 0.8" }
    require(p.runLength in 1..8) { "run must be between 1 and 8" }
    require(p.cool in 0..173) { "cool must be between 0 and 173" }
    require(p.shade in 0f..0.6f) { "shade must be between 0 and 0.6" }
    require(p.shadowY in 0f..50f && p.shadowBlur in 0f..50f) { "shadowy/shadowblur must be between 0 and 50" }
    require(p.shadowAlpha in 0f..0.85f) { "shadowalpha must be between 0 and 0.85" }
    require(p.grain in 0f..0.2f) { "grain must be between 0 and 0.2" }
    require(p.vignette in 0f..1.2f) { "vignette must be between 0 and 1.2" }
    return p
}

private fun render(c: Canvas, p: Params) {
    val rng = Random(p.seed)
    val ribbons = Ribbons(p, rng)
    val duotone = duotone(p.cool)

    // Ribbons are grouped into runs of one colour family, as the reference does
    val family = IntArray(p.bands)
    var current = if (rng.nextBoolean()) 1 else 0
    var left = 0
    for (i in 0 until p.bands) {
        if (left == 0) {
            current = 1 - current
            left = max(1, p.runLength + rng.nextInt(-1, 2))
        }
        family[i] = current
        left--
    }

    // A seeded walk down the ramp:
    val tone = FloatArray(p.bands)
    var t = rng.between(0.25f, 0.75f)
    for (i in 0 until p.bands) {
        t = reflect(t + rng.between(-0.46f, 0.46f))
        tone[i] = t
    }

    val shadowColor = argb((p.shadowAlpha * 255f).roundToInt(), 3, 8, 12)

    for (i in p.bands - 1 downTo 0) {
        val colors = duotone[family[i]]
        val litTop = sample(colors, tone[i] - 0.15f * p.shade)
        val litBottom = sample(colors, tone[i] + 0.65f * p.shade)
        val darkTop = sample(colors, tone[i])
        val darkBottom = sample(colors, tone[i] - 1.10f * p.shade)

        val top = ribbons.tops[i]
        val crease = ribbons.creases[i]
        val bottom = ribbons.edges[i]

        // Silhouette pass
        val band = ribbonPath(ribbons, bottom)
        val silhouette = Paint().apply {
            isAntiAlias = true
            color = darkBottom
            imageFilter = ImageFilter.makeDropShadow(
                0f,
                p.shadowY,
                p.shadowBlur,
                p.shadowBlur,
                shadowColor,
            )
        }
        c.drawPath(band, silhouette)
        silhouette.close()

        // Lower face: ramps darker toward the ribbon's own bottom edge
        drawFace(c, ribbons, band, crease, bottom, darkTop, darkBottom)

        // Upper face: ramps lighter toward the crease, and stops there,, the hard fold step
        val litBand = ribbonPath(ribbons, crease)
        drawFace(c, ribbons, litBand, top, crease, litTop, litBottom)
    }
}

/**
 * Paints one face as vertical strips, one per supersampled column.
 * Strips run from the top of the canvas down to [gradBottom] and let [clip] cut them. Above
 * [gradTop] the gradient clamps to [colorTop], which is what the ribbon in front covers.
 */
private fun drawFace(
    c: Canvas,
    r: Ribbons,
    clip: Path,
    gradTop: FloatArray,
    gradBottom: FloatArray,
    colorTop: Int,
    colorBottom: Int,
) {
    val shader = Shader.makeLinearGradient(0f, 0f, 0f, 1f, gradientOf(intArrayOf(colorTop, colorBottom)))
    val paint = Paint().apply {
        isAntiAlias = false
        isDither = true
        this.shader = shader
    }
    // One device pixel of slack, so the strips always reach past the antialiased clip edge
    val slack = 1f / r.ssf

    c.save()
    c.clipPath(clip, ClipMode.INTERSECT, true)
    for (j in 0 until r.cols - 1) {
        val g0 = gradTop[j]
        val height = gradBottom[j] - g0
        if (height < 0.25f) continue
        c.save()
        c.translate(0f, g0)
        c.scale(1f, height)
        c.drawRect(
            Rect.makeLTRB(
                r.x(j),
                (-OVERLAP - g0) / height,
                r.x(j + 1),
                1f + slack / height,
            ),
            paint,
        )
        c.restore()
    }
    c.restore()

    paint.close()
    shader.close()
}

/**
 * Everything above [bottom], closed off at the top of the canvas.
 */
private fun ribbonPath(r: Ribbons, bottom: FloatArray): Path {
    val step = r.ss
    val last = r.cols - 1
    val pb = PathBuilder()
    pb.moveTo(r.x(0), -OVERLAP)
    pb.lineTo(r.x(last), -OVERLAP)
    var j = last
    while (j >= 0) {
        pb.lineTo(r.x(j), bottom[j])
        j -= step
    }
    pb.lineTo(r.x(0), bottom[0])
    return pb.closePath().detach()
}

/**
 * The edge curves, sampled once per supersampled column, plus the crease inside each ribbon.
 */
private class Ribbons(p: Params, rng: Random) {
    val ss = p.ss
    val ssf = p.ss.toFloat()
    val cols = W * p.ss + 1

    val edges: Array<FloatArray>    // `[i]` is ribbon `i`'s own bottom edge - ribbons no longer share boundaries
    val tops: Array<FloatArray> /// `[i]` is [edges] `[i]` lifted by the ribbon's own thickness
    val creases: Array<FloatArray>

    init {
        // The lead harmonic carries the shape
        val amplitude = floatArrayOf(
            p.wave * rng.between(0.62f, 0.85f),
            p.wave * rng.between(0.11f, 0.22f),
            p.wave * rng.between(0.04f, 0.09f),
        )
        val frequency = floatArrayOf(
            p.freq * rng.between(0.75f, 1.35f),
            p.freq * rng.between(1.70f, 2.70f),
            p.freq * rng.between(3.00f, 4.50f),
        )
        val phase = FloatArray(HARMONICS) { rng.nextFloat() }
        val drift = FloatArray(HARMONICS) { p.drift * rng.between(0.6f, 1.5f) * (it + 1) }

        val envSign = if (rng.nextBoolean()) 1f else -1f

        // Uneven spacing
        val taperPhase = rng.nextFloat()
        val gaps = FloatArray(p.bands) { i ->
            1f + p.taper * sin(TAUf * (1.7f * (i + 0.5f) / p.bands + taperPhase))
        }
        val margin = p.wave * 1.3f + 40f
        val scale = (H + 2f * margin) / gaps.sum()
        val thickness = FloatArray(p.bands) { gaps[it] * scale }

        // AAAAAAAAAAAAAA
        val base = FloatArray(p.bands)
        base[0] = -margin + thickness[0]
        for (i in 1 until p.bands) base[i] = base[i - 1] + thickness[i]

        // FAMILY
        val strayRng = Random(p.seed xor 0x51A7L)

        fun scaled(v: FloatArray, spread: Float) =
            FloatArray(v.size) { v[it] * (1f + p.stray * strayRng.between(-spread, spread)) }

        fun shifted(v: FloatArray, spread: Float) =
            FloatArray(v.size) { v[it] + p.stray * strayRng.between(-spread, spread) }

        edges = Array(p.bands) { i ->
            val amp = scaled(amplitude, 0.85f)
            val frq = scaled(frequency, 0.35f)
            val phs = shifted(phase, 0.25f)
            FloatArray(cols) { j ->
                val u = j / ssf / W
                val swing = 1f + envSign * p.env * (2f * u - 1f)
                var y = base[i]
                for (k in 0 until HARMONICS) {
                    val theta = TAUf * (frq[k] * u + phs[k]) + i * drift[k]
                    val shaped = if (k == 0) theta + p.skew * sin(theta) else theta
                    val wave = sin(shaped)
                    val curved = if (k == 0) wave + p.arc * (sin(HALF_PIf * wave) - wave) else wave
                    y += amp[k] * swing * curved
                }
                y
            }
        }
        tops = Array(p.bands) { i -> FloatArray(cols) { j -> edges[i][j] - thickness[i] } }

        creases = Array(p.bands) { i ->
            val f = (p.fold + rng.between(-p.foldJitter, p.foldJitter)).coerceIn(0.05f, 0.95f)
            val sway = p.foldJitter * 0.5f * rng.between(0.4f, 1f)
            val swayFrequency = rng.between(0.4f, 0.9f)
            val swayPhase = rng.nextFloat()
            val bottom = edges[i]
            FloatArray(cols) { j ->
                val u = j / ssf / W
                val fx = (f + sway * sin(TAUf * (swayFrequency * u + swayPhase))).coerceIn(0.05f, 0.95f)
                bottom[j] - thickness[i] * (1f - fx)
            }
        }
    }

    fun x(j: Int) = j / ssf
}

/** The two ramps, each dark to light, that ribbons alternate between in runs. */
private class Duotone(private val even: Palette, private val odd: Palette) {
    /** [family] is a ribbon's family index, handed out a run at a time rather than per ribbon. */
    operator fun get(family: Int): Palette = if (family % 2 == 0) even else odd
}

/** `cool = 0` is the pair measured off the reference; any other index splits a stock ramp. */
private fun duotone(cool: Int): Duotone {
    if (cool == 0) {
        return Duotone(
            Palette(0xFF031818, 0xFF042F2F, 0xFF0C4E4C, 0xFF1D706C, 0xFF359A96, 0xFF49B3AE, 0xFF52C3BC, 0xFF65D4CE),
            Palette(0xFF2B0B04, 0xFF671E13, 0xFF9C2A18, 0xFFDA2D12, 0xFFF1572F, 0xFFFA6838, 0xFFFD985A, 0xFFD2A471),
        )
    }
    val stock = Palettes.coolPalette(cool).expand(256)
    return Duotone(
        Palette(IntArray(8) { stock.bound(it / 7f * 0.46f * 255f) }),
        Palette(IntArray(8) { stock.bound((0.54f + it / 7f * 0.46f) * 255f) }),
    )
}

private fun sample(colors: Palette, t: Float): Int {
    val x = t.coerceIn(0f, 1f) * (colors.size - 1)
    val i = floor(x).toInt().coerceAtMost(colors.size - 2)
    return lerpColor(colors[i], colors[i + 1], x - i)
}

/** Folds a walk back inside `0.10..0.90` instead of letting it stick to the ramp ends. */
private fun reflect(v: Float): Float {
    var x = v
    if (x < 0.10f) x = 0.20f - x
    if (x > 0.90f) x = 1.80f - x
    return x.coerceIn(0.10f, 0.90f)
}
