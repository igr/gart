import dev.oblac.gart.angles.Angle
import dev.oblac.gart.gfx.fillOf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.cos
import kotlin.math.sin

typealias RingDrawFns = Pair<() -> Unit, () -> Unit>

fun createDrawRing(
    c: Canvas,
    center: Point,
    radius: Float,
    radius2: Float,
    width1: Float,
    width2: Float,
    angle: Angle,
    colorBold: Int,
): RingDrawFns {
    // Draw back part first (bottom half)
    val fnBack = {
        drawRingPart(
            c, center, radius, radius2, width1, width2, angle, colorBold,
            startAngle = 0f, sweepAngle = 180f, isBack = true
        )
    }

    // Draw front part second (top half)
    val fnFront = {
        drawRingPart(
            c, center, radius, radius2, width1, width2, angle, colorBold,
            startAngle = 180f, sweepAngle = 180f, isBack = false
        )
    }
    return Pair(fnBack, fnFront)
}

private fun drawRingPart(
    c: Canvas,
    center: Point,
    radius: Float,
    radius2: Float,
    width1: Float,
    width2: Float,
    angle: Angle,
    colorBold: Int,
    startAngle: Float,
    sweepAngle: Float,
    isBack: Boolean
) {
    // Calculate the inner and outer radii for the ellipse
    val outerRadiusX = radius
    val outerRadiusY = radius2

    // Create the outer oval rect
    val outerRect = Rect.makeXYWH(
        center.x - outerRadiusX,
        center.y - outerRadiusY,
        outerRadiusX * 2,
        outerRadiusY * 2
    )

    // Calculate the inner radii, subtracting the width
    val innerRadiusX = outerRadiusX - width1
    val innerRadiusY = outerRadiusY - width2

    // Create the inner oval rect
    val innerRect = Rect.makeXYWH(
        center.x - innerRadiusX,
        center.y - innerRadiusY,
        innerRadiusX * 2,
        innerRadiusY * 2
    )

    c.save()
    c.rotate(angle.degrees, center.x, center.y)

    // Create a path for the ring segment
    val path = Path()

    // Add the outer arc
    path.addArc(outerRect, startAngle, sweepAngle)

    // Add lines to connect to inner arc
    val outerEndX = center.x + outerRadiusX * cos(Math.toRadians((startAngle + sweepAngle).toDouble())).toFloat()
    val outerEndY = center.y + outerRadiusY * sin(Math.toRadians((startAngle + sweepAngle).toDouble())).toFloat()
    val innerEndX = center.x + innerRadiusX * cos(Math.toRadians((startAngle + sweepAngle).toDouble())).toFloat()
    val innerEndY = center.y + innerRadiusY * sin(Math.toRadians((startAngle + sweepAngle).toDouble())).toFloat()

    path.lineTo(innerEndX, innerEndY)

    // Add the inner arc (in reverse direction)
    path.addArc(innerRect, startAngle + sweepAngle, -sweepAngle)

    path.closePath()

    c.drawPath(path, fillOf(colorBold))

    c.restore()
}
