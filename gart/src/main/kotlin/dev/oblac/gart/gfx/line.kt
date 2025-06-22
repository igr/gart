package dev.oblac.gart.gfx

import dev.oblac.gart.math.fastSqrt
import org.jetbrains.skia.*
import kotlin.math.sqrt

data class Line(val a: Point, val b: Point) {
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
    fun shortenLen(len: Float) =
        Line(this.pointFromStartLen(len), this.pointFromEndLen(len))

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
        val path = Path()
        path.moveTo(a.x, a.y)
        path.lineTo(b.x, b.y)
        return path
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

fun Line.toFatLine(thickness: Float): Path {
    return fatLine(a.x, a.y, b.x, b.y, thickness)
}
