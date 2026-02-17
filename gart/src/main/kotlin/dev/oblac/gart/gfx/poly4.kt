package dev.oblac.gart.gfx

import dev.oblac.gart.angle.Angle
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
    /**
     * Returns the 4 points of the polygon as a list.
     */
    fun points() = listOf(a, b, c, d)

    /**
     * Returns the center point of the polygon.
     * Calculated as the average of the 4 corner points.
     */
    fun center() = Point(
        (a.x + b.x + c.x + d.x) / 4,
        (a.y + b.y + c.y + d.y) / 4
    )

    /**
     * Returns the top point of the polygon.
     * Finds the point with the smallest y-coordinate.
     */
    fun topPoint(): Point = points().minBy { it.y }

    /**
     * Returns the 4 edges of the polygon as lines.
     */
    fun lines(): List<Line> {
        return listOf(
            Line(a, b),
            Line(b, c),
            Line(c, d),
            Line(d, a)
        )
    }

    val path = Path().apply {
        moveTo(a.x, a.y)
        lineTo(b.x, b.y)
        lineTo(c.x, c.y)
        lineTo(d.x, d.y)
        closePath()
    }

    fun shrink(factor: Float): Poly4 {
        val center = center()
        fun shrinkPoint(p: Point): Point {
            val dirX = p.x - center.x
            val dirY = p.y - center.y
            return Point(
                center.x + dirX * factor,
                center.y + dirY * factor
            )
        }
        return Poly4(
            shrinkPoint(a),
            shrinkPoint(b),
            shrinkPoint(c),
            shrinkPoint(d)
        )
    }

    fun move(dx: Float, dy: Float): Poly4 {
        return Poly4(
            Point(a.x + dx, a.y + dy),
            Point(b.x + dx, b.y + dy),
            Point(c.x + dx, c.y + dy),
            Point(d.x + dx, d.y + dy)
        )
    }

    companion object {
        fun rectAroundPoint(c: Point, width: Float, height: Float, angle: Angle): Poly4 {
            val centerX = c.x
            val centerY = c.y
            val radians = angle.radians
            val halfW = width / 2
            val halfH = height / 2
            val cosA = cos(radians)
            val sinA = sin(radians)

            val x1 = centerX + cosA * halfW - sinA * halfH
            val y1 = centerY + sinA * halfW + cosA * halfH

            val x2 = centerX + cosA * halfW - sinA * -halfH
            val y2 = centerY + sinA * halfW + cosA * -halfH

            val x3 = centerX + cosA * -halfW - sinA * -halfH
            val y3 = centerY + sinA * -halfW + cosA * -halfH

            val x4 = centerX + cosA * -halfW - sinA * halfH
            val y4 = centerY + sinA * -halfW + cosA * halfH

            return Poly4(
                Point(x1, y1),
                Point(x2, y2),
                Point(x3, y3),
                Point(x4, y4)
            )
        }

        fun squareAroundPoint(c: Point, sideLength: Float, angle: Angle): Poly4 {
            val centerX = c.x
            val centerY = c.y

            val radians = angle.radians

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
