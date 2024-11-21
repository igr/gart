package dev.oblac.gart.gfx

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import kotlin.math.sqrt

data class Line(val a: Point, val b: Point)

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
