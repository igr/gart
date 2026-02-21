package dev.oblac.gart.gfx

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Radians
import dev.oblac.gart.math.fastSqrt
import dev.oblac.gart.vector.Vector2
import org.jetbrains.skia.*
import kotlin.math.*

data class Line(val a: Point, val b: Point) {
    val x1 get() = a.x
    val y1 get() = a.y
    val x2 get() = b.x
    val y2 get() = b.y

    fun reversed() = Line(b, a)

    fun centerPoint() = Point((a.x + b.x) / 2, (a.y + b.y) / 2)

    fun length() = fastSqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y))

    fun pointFromStart(t: Float): Point {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return Point(a.x + t * dx, a.y + t * dy)
    }

    /**
     * Returns a new line that is shorter or longer for the given length.
     * Positive length means shorter (!), negative longer.
     * (to match the other functions)
     */
    fun shortenByLen(len: Float) =
        Line(this.pointFromStartLen(len), this.pointFromEndLen(len))

    fun extendByLen(len: Float) =
        Line(this.a, this.pointFromEndLen(-len))

    fun extendBy(f: Float) =
        Line(this.a, this.pointFromEnd(-f))

    fun pointFromStartLen(len: Float): Point {
        val ratio = len / length()
        return pointFromStart(ratio)
    }

    fun pointFromEnd(t: Float): Point {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return Point(b.x - t * dx, b.y - t * dy)
    }

    fun pointFromEndLen(len: Float): Point {
        val ratio = len / length()
        return pointFromEnd(ratio)
    }

    fun toBoundingRectangle(gapW: Float, gapH: Float): Rect {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val length = sqrt(dx * dx + dy * dy)  // Manual hypot calculation

        // Avoid division by zero
        if (length == 0f) return Rect(a.x, a.y, a.x + gapW, a.y + gapH)

        // Normalize perpendicular vector
        val perpX = -dy / length * (gapH / 2)
        val perpY = dx / length * (gapH / 2)

        // Compute 4 corners
        val xA = a.x + perpX
        val yA = a.y + perpY
        val xB = a.x - perpX
        val yB = a.y - perpY
        val xC = b.x + perpX
        val yC = b.y + perpY
        val xD = b.x - perpX
        val yD = b.y - perpY

        // Bounding box
        val left = minOf(xA, xB, xC, xD) - gapW / 2
        val top = minOf(yA, yB, yC, yD) - gapW / 2
        val right = maxOf(xA, xB, xC, xD) + gapW / 2
        val bottom = maxOf(yA, yB, yC, yD) + gapW / 2

        return Rect(left, top, right, bottom)
    }

    fun toWrappingRectangle(gapW: Float): Poly4 {
        val x1 = a.x
        val y1 = a.y
        val x2 = b.x
        val y2 = b.y

        val dx = x2 - x1
        val dy = y2 - y1
        val length = sqrt(dx * dx + dy * dy)  // Compute the line length

        // Avoid division by zero
        if (length == 0f) throw IllegalArgumentException("Line length cannot be zero")

        // Normalize perpendicular vector (rotated by 90 degrees)
        val perpX = -dy / length * (gapW / 2)
        val perpY = dx / length * (gapW / 2)

        // Compute 4 corners of the rectangle
        val xA = x1 + perpX
        val yA = y1 + perpY
        val xB = x1 - perpX
        val yB = y1 - perpY
        val xC = x2 + perpX
        val yC = y2 + perpY
        val xD = x2 - perpX
        val yD = y2 - perpY

        // Create a rectangle around the line
        return Poly4(
            Point(xA, yA),
            Point(xB, yB),
            Point(xD, yD),
            Point(xC, yC)
        )
    }

    fun toPath(): Path {
        val path = PathBuilder()
        path.moveTo(a.x, a.y)
        path.lineTo(b.x, b.y)
        return path.detach()
    }

    /**
     * Converts this line to a DLine (directional line) representation.
     */
    fun toDline(): DLine {
        val angle = angle()
        return DLine(a, Vector2.of(angle))
    }


    fun points(count: Int): List<Point> {
        val step = this.length() / (count - 1)
        return (0 until count).map { i ->
            val t = i * step
            this.pointFromStartLen(t)
        }
    }

    companion object {
        fun of(x1: Float, y1: Float, x2: Float, y2: Float): Line {
            return Line(Point(x1, y1), Point(x2, y2))
        }

        fun parallelTo(target: Line, point: Point): Line {
            val dx = target.b.x - target.a.x
            val dy = target.b.y - target.a.y
            val lengthSquared = dx * dx + dy * dy

            if (lengthSquared == 0f) {
                return Line(point, point)   // Target line is a point, return a point
            }

            // Create a parallel line by using the same direction vector as the target line
            // The parallel line passes through the given point and has the same direction
            val endPoint = Point(
                point.x - dx,
                point.y - dy
            )

            return Line(point, endPoint)
        }

        /**
         * Creates a line from a point to the closest point on the given line segment.
         * If the line segment is a point, it returns a line from the point to itself.
         */
        fun fromPointToLine(p: Point, it: Line): Line {
            val dx = it.b.x - it.a.x
            val dy = it.b.y - it.a.y
            val lengthSquared = dx * dx + dy * dy

            if (lengthSquared == 0f) {
                return Line(p, p)   // Line is a point
            }

            val t = ((p.x - it.a.x) * dx + (p.y - it.a.y) * dy) / lengthSquared
            val clampedT = t.coerceIn(0f, 1f)

            val closestPoint = Point(
                it.a.x + clampedT * dx,
                it.a.y + clampedT * dy
            )
            return Line(p, closestPoint)
        }

        /**
         * Creates a line at a given angle from a starting point with a specified length.
         */
        fun fromPointAtAngle(startingPoint: Point, angle: Angle, length: Float): Line {
            val a = startingPoint
            val b = org.jetbrains.skia.Point(
                a.x + length * cos(angle.radians),
                a.y + length * sin(angle.radians)
            )
            return Line(a, b)
        }

    }

    fun isPointOnLine(point: Point, tolerance: Float = 1f): Boolean {
        val crossProduct = (point.y - a.y) * (b.x - a.x) - (point.x - a.x) * (b.y - a.y)
        if (abs(crossProduct) > tolerance) return false

        val dotProduct = (point.x - a.x) * (b.x - a.x) + (point.y - a.y) * (b.y - a.y)
        if (dotProduct < 0) return false

        val squaredLength = this.length() * this.length()
        if (dotProduct > squaredLength) return false

        return true
    }

    /**
     * Returns the angle of the line.
     * The angle is measured from the positive x-axis, counter-clockwise.
     */
    fun angle(): Angle {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return Radians(atan2(dy.toDouble(), dx.toDouble()).toFloat())
    }

    fun toFatLine(thickness: Float): Path {
        return fatLine(a.x, a.y, b.x, b.y, thickness)
    }

    /**
     * Returns the midpoint of the line.
     */
    fun midPoint() = Point((a.x + b.x) / 2, (a.y + b.y) / 2)

    fun lineFromStartLen(length: Float): Line {
        val newEnd = pointFromStartLen(length)
        return Line(a, newEnd)
    }

    /**
     * Returns the smallest angle between this line and another line.
     * The result is in the range [-π, π].
     */
    fun angleTo(line2: Line): Angle {
        val angle1 = this.angle()
        val angle2 = line2.angle()
        var angleDiff = (angle2 - angle1)
        while (angleDiff <= -Radians.PI) angleDiff = (angleDiff + Radians.TWO_PI)
        while (angleDiff > Radians.PI) angleDiff = (angleDiff - Radians.TWO_PI)
        return angleDiff
    }
}

fun Canvas.drawLine(line: Line, color: Paint) = drawLine(line.a, line.b, color)

// https://www.forbes.com/asap/2002/0624/044.html
/**
 * Creates a path that represents a fat line.
 */
fun fatLine(x0: Float, y0: Float, x1: Float, y1: Float, thickness: Float): Path {
    var dx = x1 - x0
    var dy = y1 - y0
    val linelength = sqrt(dx * dx + dy * dy)
    dx /= linelength
    dy /= linelength
    // (dx, dy) is now a unit vector pointing in the direction of the line
    // A perpendicular vector is given by (-dy, dx)
    val px = 0.5f * thickness * (-dy) // perpendicular vector with length thickness * 0.5
    val py = 0.5f * thickness * dx

    return closedPathOf(
        Point(x0 + px, y0 + py),
        Point(x0 - px, y0 - py),
        Point(x1 - px, y1 - py),
        Point(x1 + px, y1 + py),
    )
}

fun Canvas.drawLine(p1: Point, p2: Point, stroke: Paint) = this.drawLine(p1.x, p1.y, p2.x, p2.y, stroke)
