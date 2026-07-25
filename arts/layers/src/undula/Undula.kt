package undula

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.darken
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.color.lighten
import dev.oblac.gart.color.toIntColor
import dev.oblac.gart.gfx.clear
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.io.detectHeadlessFlags
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Shader

private const val W = 800
private const val H = 1200
private const val SS = 2
private const val BASE_PHASE = 0.56f
private const val ALTERNATE_ROW_OFFSET = 0.5f // half a period
private const val RED = 0xfff4482d
private const val BLUE = 0xff22b7ae

private val backgroundColor = darken(BLUE.toIntColor(), 0.73f)
private val shadowColor = darken(BLUE.toIntColor(), 0.92f).alpha(175)

private fun phaseFor(row: Int): Float =
    BASE_PHASE - if (row % 2 == 1) ALTERNATE_ROW_OFFSET else 0f

private data class Wave(
    val y: Float,
    val height: Float,
    val depth: Float,
    val base: Int,
    val tone: Float,
    val sideShade: Float,
    val phase: Float,
    val amplitude: Float,
    val tilt: Float,
) {
    val top = if (tone >= 0f) lighten(base, tone) else darken(base, -tone)
    val side = darken(base, sideShade)

    constructor(
        y: Float,
        height: Float,
        depth: Float,
        base: Long,
        tone: Float,
        sideShade: Float,
        phase: Float,
        amplitude: Float,
        tilt: Float,
    ) : this(y, height, depth, base.toIntColor(), tone, sideShade, phase, amplitude, tilt)
}

private val waves = listOf(
    Wave(-300f, 225f, 55f, RED, 0.13f, 0.28f, phaseFor(0), 150f, -25f),
    Wave(-20f, 225f, 55f, BLUE, 0.18f, 0.50f, phaseFor(1), 150f, -25f),
    Wave(260f, 225f, 55f, RED, 0f, 0.42f, phaseFor(2), 150f, -25f),
    Wave(540f, 225f, 55f, BLUE, 0f, 0.56f, phaseFor(3), 150f, -25f),
    Wave(820f, 225f, 55f, RED, 0.05f, 0.40f, phaseFor(4), 150f, -25f),
    Wave(1100f, 225f, 55f, BLUE, 0.28f, 0.45f, phaseFor(5), 150f, -25f),
    Wave(1380f, 225f, 55f, RED, 0.13f, 0.28f, phaseFor(6), 150f, -25f),
)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("undula", W, H)

    val large = Gartvas(Dimension(W * SS, H * SS))
    large.canvas.scale(SS.toFloat(), SS.toFloat())
    render(large.canvas)

    val image = gart.gartvas()
    large.snapshot().use { snapshot ->
        image.canvas.drawImageRect(
            snapshot,
            Rect.makeWH((W * SS).toFloat(), (H * SS).toFloat()),
            Rect.makeWH(W.toFloat(), H.toFloat()),
            SamplingMode.MITCHELL,
            null,
            true,
        )
    }

    gart.saveImage(image, "undula.png")
    if (!headless) gart.window().showImage(image)
}

private fun render(canvas: Canvas) {
    canvas.clear(backgroundColor)

    waves.forEach { wave ->
        val topFace = ribbon(wave.y, wave.y + wave.height, wave)
        val recessedFace = below(wave.y + wave.height, wave)

        canvas.drawPath(
            recessedFace,
            gradientPaint(
                lighten(wave.side, 0.08f),
                darken(wave.side, 0.34f),
                wave.y + wave.height,
                wave.y + wave.height + wave.depth,
            ),
        )

        canvas.drawPath(
            ribbon(wave.y + wave.height, wave.y + wave.height + wave.depth, wave),
            fillOf(shadowColor).apply {
                imageFilter = ImageFilter.makeBlur(22f, 22f, FilterTileMode.DECAL)
            },
        )

        canvas.drawPath(
            topFace,
            gradientPaint(lighten(wave.top, 0.22f), darken(wave.top, 0.12f), wave.y, wave.y + wave.height),
        )
    }
}

private fun ribbon(topY: Float, bottomY: Float, wave: Wave): Path {
    val a = wave.amplitude
    val p = wave.phase
    fun y(base: Float, x: Float) = base + lobe(x, p, a) + wave.tilt * x

    return PathBuilder()
        .moveTo(-90f, y(topY, -0.12f))
        .cubicTo(
            95f, y(topY, 0.12f),
            210f, y(topY, 0.31f),
            384f, y(topY, 0.50f),
        )
        .cubicTo(
            545f, y(topY, 0.69f),
            670f, y(topY, 0.88f),
            858f, y(topY, 1.12f),
        )
        .lineTo(858f, y(bottomY, 1.12f))
        .cubicTo(
            670f, y(bottomY, 0.88f),
            545f, y(bottomY, 0.69f),
            384f, y(bottomY, 0.50f),
        )
        .cubicTo(
            210f, y(bottomY, 0.31f),
            95f, y(bottomY, 0.12f),
            -90f, y(bottomY, -0.12f),
        )
        .closePath()
        .detach()
}

private fun below(topY: Float, wave: Wave): Path {
    val a = wave.amplitude
    val p = wave.phase
    fun y(x: Float) = topY + lobe(x, p, a) + wave.tilt * x

    return PathBuilder()
        .moveTo(-90f, y(-0.12f))
        .cubicTo(
            95f, y(0.12f),
            210f, y(0.31f),
            384f, y(0.50f),
        )
        .cubicTo(
            545f, y(0.69f),
            670f, y(0.88f),
            858f, y(1.12f),
        )
        .lineTo(858f, H + 240f)
        .lineTo(-90f, H + 240f)
        .closePath()
        .detach()
}

private fun lobe(x: Float, phase: Float, amplitude: Float): Float =
    (kotlin.math.sin((x + phase) * Math.PI * 2.0) * amplitude).toFloat()

private fun gradientPaint(light: Int, dark: Int, y0: Float, y1: Float) = paint().apply {
    isDither = true
    shader = Shader.makeLinearGradient(0f, y0, 0f, y1, gradientOf(intArrayOf(light, dark)))
}
