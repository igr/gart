package work.strata

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.argb
import dev.oblac.gart.color.darken
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.color.lighten
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.pl
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.between
import dev.oblac.gart.smooth.bSpline
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Strata — a seeded stack of softly shadowed paper contours parting around a luminous cut-paper disc.
 */
private const val W = 900
private const val H = 1100
private const val TAU = (2.0 * PI).toFloat()
private const val ANCHORS = 10

private data class Params(
    val seed: Long,
    val out: String,
    val ss: Int,
    val cool: Int,
    val upper: Int,
    val lower: Int,
    val gap: Float,
    val wave: Float,
    val crest: Float,
    val drift: Float,
    val bend: Float,
    val jitter: Float,
    val opening: Float,
    val orbX: Float,
    val orbY: Float,
    val orbRx: Float,
    val orbRy: Float,
    val orbTilt: Float,
    val orbJitter: Float,
    val shade: Float,
    val shadowY: Float,
    val shadowBlur: Float,
    val shadowAlpha: Float,
    val grain: Float,
    val vignette: Float,
)

private data class PaperPalette(
    val upper: IntArray,
    val lower: IntArray,
    val ground: IntArray,
    val orb: IntArray,
)

private enum class Family(val salt: Long) {
    UPPER(0x5A17L),
    LOWER(0x2C91L),
}

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val params = resolveParams()
    val palette = paperPalette(params.cool)
    val gart = Gart.of("strata", W, H)

    println(gart)
    println(
        "seed=${params.seed} cool=${params.cool} upper=${params.upper} lower=${params.lower} " +
            "gap=${params.gap} wave=${params.wave} opening=${params.opening}",
    )

    val big = Gartvas(Dimension(W * params.ss, H * params.ss))
    big.canvas.scale(params.ss.toFloat(), params.ss.toFloat())
    render(big.canvas, params, palette)

    val g = gart.gartvas()
    val snapshot = big.snapshot()
    g.canvas.drawImageRect(
        snapshot,
        Rect.makeWH((W * params.ss).toFloat(), (H * params.ss).toFloat()),
        Rect.makeWH(W.toFloat(), H.toFloat()),
        SamplingMode.MITCHELL,
        null,
        true,
    )
    snapshot.close()

    if (params.vignette > 0f) g.canvas.drawVignette(g.d, params.vignette, radius = 0.82f, innerStop = 0.52f)
    if (params.grain > 0f) addGrain(g, params.grain, params.seed.toInt())

    val output = if (params.out.endsWith(".png", ignoreCase = true)) params.out else "${params.out}.png"
    gart.saveImage(g, output)
    if (!headless) gart.window().showImage(g)
}

private fun resolveParams(): Params {
    val p = Params(
        seed = pl("seed", 2420L),
        out = ps("out", "work/strata"),
        ss = pi("ss", 2),
        cool = pi("cool", 19), // palette sweep render 076
        upper = pi("upper", 5),
        lower = pi("lower", 4),
        gap = pf("gap", 105f),
        wave = pf("wave", 215f),
        crest = pf("crest", 0.24f),
        drift = pf("drift", 0.014f),
        bend = pf("bend", 0.16f),
        jitter = pf("jitter", 18f),
        opening = pf("opening", 0.47f),
        orbX = pf("orbx", 0.46f),
        orbY = pf("orby", 0.45f),
        orbRx = pf("orbrx", 0.235f),
        orbRy = pf("orbry", 0.185f),
        orbTilt = pf("orbtilt", -18f),
        orbJitter = pf("orbjitter", 10f),
        shade = pf("shade", 0.07f),
        shadowY = pf("shadowy", 18f),
        shadowBlur = pf("shadowblur", 18f),
        shadowAlpha = pf("shadowalpha", 0.46f),
        grain = pf("grain", 0.014f),
        vignette = pf("vignette", 0.12f),
    )

    require(p.ss in 1..4) { "ss must be between 1 and 4" }
    require(p.cool in 1..173) { "cool must be between 1 and 173" }
    require(p.upper in 2..8) { "upper must be between 2 and 8" }
    require(p.lower in 2..8) { "lower must be between 2 and 8" }
    require(p.gap in 45f..180f) { "gap must be between 45 and 180" }
    require(p.wave in 40f..280f) { "wave must be between 40 and 280" }
    require(p.crest in -0.25f..1.25f) { "crest must be between -0.25 and 1.25" }
    require(p.drift in -0.08f..0.08f) { "drift must be between -0.08 and 0.08" }
    require(p.bend in 0f..0.45f) { "bend must be between 0 and 0.45" }
    require(p.jitter in 0f..70f) { "jitter must be between 0 and 70" }
    require(p.opening in 0.30f..0.68f) { "opening must be between 0.30 and 0.68" }
    require(p.orbX in 0.1f..0.9f && p.orbY in 0.1f..0.9f) { "orb position must stay inside the canvas" }
    require(p.orbRx in 0.08f..0.42f && p.orbRy in 0.08f..0.42f) { "orb radii must be between 0.08 and 0.42" }
    require(p.orbTilt.isFinite() && p.orbTilt in -180f..180f) { "orbtilt must be between -180 and 180" }
    require(p.orbJitter in 0f..60f) { "orbjitter must be between 0 and 60" }
    require(p.shade in 0f..0.25f) { "shade must be between 0 and 0.25" }
    require(p.shadowY in 0f..50f && p.shadowBlur in 0f..50f) { "shadowy/shadowblur must be between 0 and 50" }
    require(p.shadowAlpha in 0f..0.85f) { "shadowalpha must be between 0 and 0.85" }
    require(p.grain in 0f..0.2f) { "grain must be between 0 and 0.2" }
    require(p.vignette in 0f..1.2f) { "vignette must be between 0 and 1.2" }
    return p
}

private fun render(c: Canvas, p: Params, colors: PaperPalette) {
    drawGround(c, colors.ground)
    val field = WaveField(p)
    val lowerStart = H * p.opening + p.gap * 0.28f

    // Back sheets: deepest first. The sun is laid over this family.
    for (layer in p.lower - 1 downTo 0) {
        val t = layer.fractionOf(p.lower)
        val meanY = lowerStart + layer * p.gap
        val amplitude = p.wave * (0.64f + 0.12f * t)
        val edge = field.edge(meanY, amplitude, layer, p.lower, Family.LOWER)
        val path = closeToTop(edge)
        drawPaper(c, path, ramp(colors.lower, t), p)
    }

    drawOrb(c, p, colors.orb)

    // Front sheets: the deepest contour cuts across the sun; shallower contours build the fan.
    val upperDeepY = H * p.opening
    for (layer in p.upper - 1 downTo 0) {
        val t = layer.fractionOf(p.upper)
        val meanY = upperDeepY - (p.upper - 1 - layer) * p.gap
        val amplitude = p.wave * (0.34f + 0.66f * t)
        val edge = field.edge(meanY, amplitude, layer, p.upper, Family.UPPER)
        val path = closeToTop(edge)
        drawPaper(c, path, ramp(colors.upper, t), p)
    }
}

private class WaveField(private val p: Params) {
    private val margin = W * 0.18f
    private val shared = FloatArray(ANCHORS) {
        Random(p.seed + it * 3571L + 19L).nextFloat() * 2f - 1f
    }

    fun edge(meanY: Float, amplitude: Float, layer: Int, count: Int, family: Family): Path {
        val rng = Random(p.seed + family.salt + layer * 7919L)
        val familyShift = if (family == Family.UPPER) 0f else -0.022f
        val centeredLayer = layer - (count - 1) * 0.5f
        val effectiveCrest = p.crest + familyShift + centeredLayer * p.drift
        val secondaryPhase = rng.nextFloat() * 0.16f - 0.08f + if (family == Family.LOWER) 0.12f else 0f
        val span = W + margin * 2f
        val points = ArrayList<Point>(ANCHORS)

        repeat(ANCHORS) { anchor ->
            val x = -margin + span * anchor / (ANCHORS - 1)
            val u = x / W
            val dominant = -cos(TAU * (u - effectiveCrest))
            val secondary = sin(TAU * 2f * (u - p.crest * 0.35f + secondaryPhase))
            val own = rng.nextFloat() * 2f - 1f
            val anchorDrift = p.jitter * (shared[anchor] * 0.68f + own * 0.32f)
            val y = meanY + amplitude * (dominant + p.bend * secondary) + anchorDrift
            points += Point(x, y)
        }
        val clamped = buildList(points.size + 4) {
            repeat(2) { add(points.first()) }
            addAll(points)
            repeat(2) { add(points.last()) }
        }
        return bSpline(clamped, 24)
    }
}

private fun closeToTop(edge: Path): Path = PathBuilder(edge)
    .lineTo(W * 1.18f, -H * 0.12f)
    .lineTo(-W * 0.18f, -H * 0.12f)
    .closePath()
    .detach()

private fun drawGround(c: Canvas, colors: IntArray) {
    c.drawPaint(Paint().apply {
        isAntiAlias = true
        isDither = true
        shader = Shader.makeLinearGradient(
            0f,
            H * 0.72f,
            W.toFloat(),
            H.toFloat(),
            gradientOf(colors, FloatArray(colors.size) { it.fractionOf(colors.size) }),
        )
    })
}

private fun drawPaper(c: Canvas, path: Path, color: Int, p: Params) {
    val shadowColor = argb((p.shadowAlpha * 255f).roundToInt(), 2, 7, 13)
    c.drawPath(path, Paint().apply {
        isAntiAlias = true
        isDither = true
        shader = Shader.makeLinearGradient(
            0f,
            0f,
            W.toFloat(),
            H.toFloat(),
            gradientOf(
                intArrayOf(lighten(color, p.shade * 0.55f), color, darken(color, p.shade)),
                floatArrayOf(0f, 0.52f, 1f),
            ),
        )
        imageFilter = ImageFilter.makeDropShadow(
            0f,
            p.shadowY,
            p.shadowBlur,
            p.shadowBlur,
            shadowColor,
        )
    })
}

private fun drawOrb(c: Canvas, p: Params, colors: IntArray) {
    val rng = Random(p.seed * 65537L + 31L)
    val cx = W * p.orbX + rng.between(-p.orbJitter, p.orbJitter)
    val cy = H * p.orbY + rng.between(-p.orbJitter, p.orbJitter)
    val rx = W * p.orbRx
    val ry = H * p.orbRy
    val oval = PathBuilder()
        .addOval(Rect.makeXYWH(cx - rx, cy - ry, rx * 2f, ry * 2f))
        .detach()

    c.save()
    c.rotate(p.orbTilt, cx, cy)
    c.drawPath(oval, Paint().apply {
        isAntiAlias = true
        isDither = true
        shader = Shader.makeLinearGradient(
            cx - rx * 0.24f,
            cy - ry,
            cx + rx * 0.18f,
            cy + ry,
            gradientOf(colors, FloatArray(colors.size) { it.fractionOf(colors.size) }),
        )
        imageFilter = ImageFilter.makeDropShadow(
            0f,
            p.shadowY * 0.62f,
            p.shadowBlur * 1.15f,
            p.shadowBlur * 1.15f,
            argb((p.shadowAlpha * 220f).roundToInt(), 5, 7, 12),
        )
    })

    c.save()
    c.clipPath(oval, ClipMode.INTERSECT, true)
    drawOrbArcBand(c, cx, cy - ry * 0.16f, rx, ry, ry * 0.24f, argb(30, 255, 220, 177))
    drawOrbArcBand(c, cx, cy + ry * 0.30f, rx, ry, ry * 0.28f, argb(38, 255, 238, 169))
    c.restore()
    c.restore()
}

private fun drawOrbArcBand(
    c: Canvas,
    cx: Float,
    edgeY: Float,
    rx: Float,
    ry: Float,
    arcRise: Float,
    color: Int,
) {
    val span = rx * 1.34f
    val arcBounds = Rect.makeXYWH(cx - span, edgeY - arcRise, span * 2f, arcRise * 2f)
    val path = PathBuilder()
        .addArc(arcBounds, 180f, 180f)
        .lineTo(cx + span, edgeY + ry * 1.65f)
        .lineTo(cx - span, edgeY + ry * 1.65f)
        .closePath()
        .detach()
    c.drawPath(path, Paint().apply {
        isAntiAlias = true
        this.color = color
    })
}

private fun paperPalette(cool: Int): PaperPalette {
    val stock = Palettes.coolPalette(cool).expand(256)
    fun at(position: Float): Int = stock.bound(position.coerceIn(0f, 1f) * (stock.size - 1))

    return PaperPalette(
        // Reverse the opening span of the selected stock for front-to-back depth
        upper = intArrayOf(at(0.22f), at(0.165f), at(0.11f), at(0.055f), at(0f)),
        lower = intArrayOf(
            darken(at(0.08f), 0.22f),
            darken(at(0.01f), 0.38f),
            darken(at(0.02f), 0.52f),
            darken(at(0.12f), 0.30f),
            darken(at(0.18f), 0.18f),
        ),
        ground = intArrayOf(darken(at(0.12f), 0.48f), darken(at(0.02f), 0.68f), darken(at(0f), 0.82f)),
        // Sample the far half backwards to separate the disc from the surrounding sheets
        orb = intArrayOf(at(1f), at(0.86f), at(0.70f), at(0.58f), at(0.46f)),
    )
}

private fun ramp(colors: IntArray, t: Float): Int {
    if (colors.size == 1) return colors[0]
    val x = t.coerceIn(0f, 1f) * (colors.size - 1)
    val i = floor(x).toInt().coerceAtMost(colors.size - 2)
    return lerpColor(colors[i], colors[i + 1], x - i)
}

private fun Int.fractionOf(count: Int): Float = if (count <= 1) 0f else this.toFloat() / (count - 1)
