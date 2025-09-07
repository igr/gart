package dev.oblac.gart.gfx

import dev.oblac.gart.angle.*
import dev.oblac.gart.vector.Vector2
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point

/**
 * A vector that represents a direction and distance.
 */
@Deprecated("Use Vector2 instead", ReplaceWith("Vector2(dx, dy)"))
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

/**
 * This is a parametric line defined by a point and a direction vector.
 * The line extends infinitely in both directions from the point `p`.
 */
data class DLine(val p: Point, val dvec: Vector2) {
    fun pointFromStart(t: Float): Point {
        val dir = dvec.normalize()
        return Point(p.x + t * dir.x, p.y + t * dir.y)
    }

    fun pointFromEnd(t: Float): Point {
        val dir = dvec.normalize()
        return Point(p.x - t * dir.x, p.y - t * dir.y)
    }

    fun perpendicularDLine(): DLine {
        val perpDVec = Vector2(
            x = -dvec.y,
            y = dvec.x
        )
        return DLine(p = p, dvec = perpDVec)
    }

    fun toLine(start: Point, distance: Float): Line {
        val vectorLength = dvec.length()
        if (vectorLength == 0f) {
            throw IllegalArgumentException("Direction vector cannot be zero.")
        }

        val ux = dvec.x / vectorLength
        val uy = dvec.y / vectorLength

        return Line(
            start,
            Point(
                x = start.x + ux * distance,
                y = start.y + uy * distance
            )
        )
    }

    companion object {
        /**
         * Creates a DLine that passes through `current` point and is directed from `prev` to `next` point.
         */
        fun of(prev: Point, current: Point, next: Point): DLine {
            return DLine(
                p = current,
                dvec = Vector2(
                    x = (next.x - prev.x) / 2,
                    y = (next.y - prev.y) / 2
                )
            )
        }

        /**
         * Creates a DLine that starts at `point` and goes in the direction of `current` angle.
         */
        fun of(point: Point, current: Angle): DLine {
            return DLine(
                p = point,
                dvec = Vector2(
                    x = cos(current),
                    y = sin(current)
                )
            )
        }
    }
}

/**
 * Draws a line segment from the DLine's point `p` in both directions by `len/2`.
 */
fun Canvas.drawDLine(dLine: DLine, len: Float, paint: Paint) {
    val start = dLine.pointFromEnd(len / 2)
    val end = dLine.pointFromStart(len / 2)
    drawLine(start.x, start.y, end.x, end.y, paint)
}
