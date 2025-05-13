package dev.oblac.gart.gfx

import dev.oblac.gart.angles.Angle
import dev.oblac.gart.math.rnd
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

data class Poly4(
    val a: Point,
    val b: Point,
    val c: Point,
    val d: Point
) {
    val path = Path().apply {
        moveTo(a.x, a.y)
        lineTo(b.x, b.y)
        lineTo(c.x, c.y)
        lineTo(d.x, d.y)
        closePath()
    }

    companion object {
        fun squareAroundPoint(c: Point, sideLength: Float, angle: Angle): Poly4 {
            val centerX = c.x
            val centerY = c.y

            val radians = angle.radians()

            val halfSize = sideLength / 2

            // Calculate the 4 corners of the rotated square
            val x1 = centerX + cos(radians) * halfSize - sin(radians) * halfSize
            val y1 = centerY + sin(radians) * halfSize + cos(radians) * halfSize

            val x2 = centerX + cos(radians) * halfSize - sin(radians) * -halfSize
            val y2 = centerY + sin(radians) * halfSize + cos(radians) * -halfSize

            val x3 = centerX + cos(radians) * -halfSize - sin(radians) * -halfSize
            val y3 = centerY + sin(radians) * -halfSize + cos(radians) * -halfSize

            val x4 = centerX + cos(radians) * -halfSize - sin(radians) * halfSize
            val y4 = centerY + sin(radians) * -halfSize + cos(radians) * halfSize

            return Poly4(
                Point(x1, y1),
                Point(x2, y2),
                Point(x3, y3),
                Point(x4, y4)
            )
        }

    }
}

fun Canvas.drawPoly4(poly: Poly4, paint: Paint) = drawPath(poly.path, paint)

fun randomSquareAroundPoint(c: Point, sideLength: Float): Poly4 {
    val centerX = c.x
    val centerY = c.y

    val angle = rnd(0.0, 360.0) // Random rotation in degrees
    val radians = Math.toRadians(angle).toFloat()

    val halfSize = sideLength / 2

    // Calculate the 4 corners of the rotated square
    val x1 = centerX + cos(radians) * halfSize - sin(radians) * halfSize
    val y1 = centerY + sin(radians) * halfSize + cos(radians) * halfSize

    val x2 = centerX + cos(radians) * halfSize - sin(radians) * -halfSize
    val y2 = centerY + sin(radians) * halfSize + cos(radians) * -halfSize

    val x3 = centerX + cos(radians) * -halfSize - sin(radians) * -halfSize
    val y3 = centerY + sin(radians) * -halfSize + cos(radians) * -halfSize

    val x4 = centerX + cos(radians) * -halfSize - sin(radians) * halfSize
    val y4 = centerY + sin(radians) * -halfSize + cos(radians) * halfSize

    return Poly4(
        Point(x1, y1),
        Point(x2, y2),
        Point(x3, y3),
        Point(x4, y4)
    )
}
