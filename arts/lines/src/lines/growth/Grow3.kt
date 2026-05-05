package lines.growth

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.alpha
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.toDegrees
import dev.oblac.gart.math.toRadians
import dev.oblac.gart.painter.SprayPainter
import org.jetbrains.skia.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

fun main() {
    val clBack = NipponColors.col187_NOSHIMEHANA
    val clAccent = NipponColors.col044_BENIHI
    val clLine = NipponColors.col233_SHIRONERI
//    val clBack = RetroColors.black01
//    val clAccent = RetroColors.red01
//    val clLine = RetroColors.white01

    val gart = Gart.of("growth3", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    val d = g.d
    val c = g.canvas
    c.clear(clBack)

    val rng = Random(173)

    val sp = SprayPainter(
        d.w,
        d.h,
        bg = 0x00000000,
        fg = clLine.alpha(100),
        rng = rng,
    )

    val s = Point(820f, 220f)

    val scale = 0.4f
    val arcCount = 100
    val pointsPerArc = 40

    var paths = List(arcCount) {
        val ex0 = 560f + rng.nextFloat() * (980f - 560f)
        val ey0 = 560f + rng.nextFloat() * (980f - 560f)
        val ex = s.x + scale * (ex0 - s.x)
        val ey = s.y + scale * (ey0 - s.y)

        val gapDeg = 20f + rng.nextFloat() * 10f
        val gapRad = gapDeg.toRadians()

        val dx = ex - s.x
        val dy = ey - s.y
        val chordLen = sqrt(dx * dx + dy * dy)
        val mx = (s.x + ex) / 2f
        val my = (s.y + ey) / 2f

        val radius = chordLen / (2f * sin(gapRad / 2f))
        val perpDist = radius * cos(gapRad / 2f)

        val perpX = -dy / chordLen
        val perpY = dx / chordLen
        val cx = mx + perpDist * perpX
        val cy = my + perpDist * perpY

        val startAngleDeg = atan2(s.y - cy, s.x - cx).toDegrees()
        val sweepDeg = -(360f - gapDeg)

        List(pointsPerArc) { i ->
            val t = i.toFloat() / (pointsPerArc - 1)
            val angleRad = (startAngleDeg + sweepDeg * t).toRadians()
            Point(cx + radius * cos(angleRad), cy + radius * sin(angleRad))
        }
    }

    repeat(180) {
        paths = paths.map { path ->
            val noisy = path.withVaryingSplineNoise(
                minNoise = 0.4f,
                maxNoise = 5.5f,
                random = rng,
                preserveEnds = true,
            )
            sp.path(noisy, n = noisy.size * 2)
            noisy
        }
    }

    val arcPaint = strokeOf(clLine, 2.6f).roundStroke().alpha(180).apply {
        this.pathEffect = PathEffect.makeDiscrete(30f, 3f, 100)
        this.maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, 2f)
    }

    repeat(20) {
        val ex0 = 560f + rng.nextFloat() * (980f - 560f)
        val ey0 = 560f + rng.nextFloat() * (980f - 560f)
        val ex = s.x + scale * (ex0 - s.x)
        val ey = s.y + scale * (ey0 - s.y)

        val gapDeg = 20f + rng.nextFloat() * 10f
        val gapRad = gapDeg.toRadians()

        val dx = ex - s.x
        val dy = ey - s.y
        val chordLen = sqrt(dx * dx + dy * dy)
        val mx = (s.x + ex) / 2f
        val my = (s.y + ey) / 2f

        val radius = chordLen / (2f * sin(gapRad / 2f))
        val perpDist = radius * cos(gapRad / 2f)

        val perpX = -dy / chordLen
        val perpY = dx / chordLen
        val cx = mx + perpDist * perpX
        val cy = my + perpDist * perpY

        val startAngleDeg = atan2(s.y - cy, s.x - cx).toDegrees()
        val sweepDeg = -(360f - gapDeg)

        c.drawCircleArc(cx, cy, radius, arcPaint, Degrees(startAngleDeg), Degrees(sweepDeg))
    }

    sp.drawTo(c)

//    c.drawCircle(Circle(220f, d.hf - 160f, 120f), fillOf(clBack).apply {
//        this.blendMode = BlendMode.MODULATE
//    })

    c.drawCircle(Circle(s.offset(-22f, -50f), 140f), fillOf(clAccent).apply {
        this.blendMode = BlendMode.MULTIPLY
    })

    gart.window().showImage(g)
    gart.saveImage(g)
}
