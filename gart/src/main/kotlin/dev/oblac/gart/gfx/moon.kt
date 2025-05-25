package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import dev.oblac.gart.Draw
import org.jetbrains.skia.*
import kotlin.math.abs

/**
 * Moon phase drawing.
 * @param moonPhase -1.0f to 1.0f, where -1.0f is new moon, 0.0f is full moon, and 1.0f is new moon again.
 */
data class Moon(
    val circle: Circle,
    val shadowPaint: Paint,
    val moonPaint: Paint,
    val moonPhase: Float = 0.5f
) : Draw {
    override fun invoke(canvas: Canvas, dimension: Dimension) {
        val cx = circle.center.x
        val cy = circle.center.y
        val radius = circle.radius
        val moonPhaseAbs = abs(moonPhase)
        val sign = if (moonPhase < 0) -1f else 1f

        when {
            moonPhaseAbs == 0f -> { // full moon
                canvas.drawCircle(circle, moonPaint)
            }

            moonPhaseAbs < 0.5f -> {
                canvas.drawCircle(circle, shadowPaint)
                drawMoonPhase(canvas, cx, cy, radius, moonPhaseAbs, sign, ClipMode.INTERSECT)
            }

            moonPhaseAbs > 0.5f -> {
                canvas.drawCircle(circle, shadowPaint)
                drawMoonPhase(canvas, cx, cy, radius, moonPhaseAbs, sign, ClipMode.DIFFERENCE)
            }

            else -> {
                canvas.drawCircle(circle, shadowPaint)

                // half moon
                canvas.save()

                val mainPath = Path()
                mainPath.addRect(Rect(cx - radius, cy - radius, cx, cy + radius))
                canvas.clipPath(mainPath, ClipMode.DIFFERENCE)
                canvas.drawCircle(circle, moonPaint)

                canvas.restore()

            }
        }
    }

    private fun drawMoonPhase(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        moonPhaseAbs: Float,
        sign: Float,
        clipMode: ClipMode
    ) {
        // left side: arc. right side: full
        val top = Point(cx, cy - radius)
        val left = Point(cx + sign * (moonPhaseAbs - 0.5f) * 2 * radius, cy)
        val bottom = Point(cx, cy + radius)
        val c3 = circleFrom3Points(top, left, bottom)

        canvas.save()

        val mainPath = Path()
        mainPath.addCircle(c3)
        canvas.clipPath(mainPath, clipMode)
        canvas.drawCircle(circle, moonPaint)

        canvas.restore()
    }
}
