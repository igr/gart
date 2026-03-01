package dev.oblac.gart.gfx

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.math.toRadians
import org.jetbrains.skia.*
import kotlin.math.cos
import kotlin.math.sin

typealias DrawRing = (Canvas, Paint) -> Unit

/**
 * Creates two functions to draw a ring.
 * The first function draws the back part of the ring and should be used first.
 * The second function draws the front part of the ring and should be used second.
 * @param center The center point of the ring.
 * @param radius The first radius of the ellipse (horizontal).
 * @param radius2 The second radius of the ellipse (vertical).
 * @param width1 The width of the right & left parts of the ring line.
 * @param width2 The width of the closer part of the ring line.
 * @param width3 The width of the distant part of the ring line.
 * @param angle The angle to rotate the ring.
 */
fun createDrawRing(
    center: Point,
    radius: Float,
    radius2: Float,
    width1: Float,
    width2: Float,
    width3: Float,
    angle: Angle,
): Pair<DrawRing, DrawRing> {
    // Draw back part first (bottom half)
    val fnBack = { canvas: Canvas, paint: Paint ->
        drawRingPart(
            canvas, center, radius, radius2, width1, width3, angle, paint,
            startAngle = 0f, sweepAngle = 180f, isBack = true
        )
    }

    val fnFront = { canvas: Canvas, paint: Paint ->
        drawRingPart(
            canvas, center, radius, radius2, width1, width2, angle, paint,
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
    paint: Paint,
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
    val path = PathBuilder()

    // Add the outer arc
    path.addArc(outerRect, startAngle, sweepAngle)

    // Add lines to connect to inner arc
    val startAngleRad = startAngle.toRadians()
    val outerStartX = center.x + outerRadiusX * cos(startAngleRad)
    val outerStartY = center.y + outerRadiusY * sin(startAngleRad)
    val innerStartX = center.x + innerRadiusX * cos(startAngleRad)
    val innerStartY = center.y + innerRadiusY * sin(startAngleRad)

    val endAngleRad = (startAngle + sweepAngle).toRadians()
    val outerEndX = center.x + outerRadiusX * cos(endAngleRad)
    val outerEndY = center.y + outerRadiusY * sin(endAngleRad)
    val innerEndX = center.x + innerRadiusX * cos(endAngleRad)
    val innerEndY = center.y + innerRadiusY * sin(endAngleRad)

    path.lineTo(innerEndX, innerEndY)

    // Add the inner arc (in reverse direction)
    path.addArc(innerRect, startAngle + sweepAngle, -sweepAngle)

    // Connect back to the start of the outer arc, otherwise there will be dragons (!)
    path.lineTo(outerStartX, outerStartY)
    path.closePath()

    c.drawPath(path.detach(), paint)

    // draw the lines where ring parts connect, as the crack may be visible due to antialiasing
    val linePaint = paint.apply { this.mode = PaintMode.STROKE; this.strokeWidth = 1f }
    c.drawLine(outerStartX, outerStartY, innerStartX, innerStartY, linePaint)
    c.drawLine(outerEndX, outerEndY, innerEndX, innerEndY, linePaint)

    c.restore()
}


