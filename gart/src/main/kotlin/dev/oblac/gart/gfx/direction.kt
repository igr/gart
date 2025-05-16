package dev.oblac.gart.gfx

import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.angles.Radians
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point

/**
 * A vector that represents a direction and distance.
 */
data class DirectionVector(val dx: Float, val dy: Float) {
    val length: Float
        get() = kotlin.math.sqrt(dx * dx + dy * dy)

    fun normalize(): DirectionVector {
        val len = length
        return if (len == 0f) {
            this
        } else {
            DirectionVector(dx / len, dy / len)
        }
    }

    fun toDegrees(): Degrees {
        return Degrees(kotlin.math.atan2(dy, dx) * (180f / kotlin.math.PI.toFloat()))
    }

    fun toRadians(): Radians {
        return Radians(kotlin.math.atan2(dy, dx))
    }
}

data class DLine(val a: Point, val dvec: DirectionVector) {
    fun pointFromStart(t: Float): Point {
        return Point(a.x + t * dvec.dx, a.y + t * dvec.dy)
    }

    fun pointFromEnd(t: Float): Point {
        return Point(a.x - t * dvec.dx, a.y - t * dvec.dy)
    }

    fun perpendicularDLine(): DLine {
        val perpDVec = DirectionVector(
            dx = -dvec.dy,
            dy = dvec.dx
        )
        return DLine(a = a, dvec = perpDVec)
    }

    fun toLine(start: Point, distance: Float): Line {
        val vectorLength = dvec.length
        if (vectorLength == 0f) {
            throw IllegalArgumentException("Direction vector cannot be zero.")
        }

        val ux = dvec.dx / vectorLength
        val uy = dvec.dy / vectorLength

        return Line(
            start,
            Point(
                x = start.x + ux * distance,
                y = start.y + uy * distance
            )
        )
    }
}

fun Canvas.drawDLine(dLine: DLine, len: Float, paint: Paint) {
    val start = dLine.pointFromEnd(len / 2)
    val end = dLine.pointFromStart(len / 2)
    drawLine(start.x, start.y, end.x, end.y, paint)
}
