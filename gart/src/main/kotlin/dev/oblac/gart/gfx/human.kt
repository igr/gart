package dev.oblac.gart.gfx

import dev.oblac.gart.math.rndf
import dev.oblac.gart.smooth.chaikinSmooth
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

/**
 * Creates a human-like line between two points.
 * The line is not straight and has some random offsets.
 * Returns a list of points that should be interpolated.
 *
 * @param x Start point.
 * @param y End point.
 * @param delta Distance between points. The total number of inner points is calculated as the distance between x and y divided by delta.
 * @param maxOffset Maximum offset from the line for each of the "inner" points.
 */
fun humanLinePoints(x: Point, y: Point, delta: Int = 50, maxOffset: Float = 5f): List<Point> {
    val pointsCount = x.distanceTo(y).toInt() / delta
    val p = pathOf(x, y)
    return pointsOn(p, pointsCount).mapIndexed { index, it ->
        if (index == 0 || index == pointsCount - 1) {
            it
        } else {
            val deltaX = rndf(-maxOffset, maxOffset)
            val deltaY = rndf(-maxOffset, maxOffset)
            Point(it.x + deltaX, it.y + deltaY)
        }
    }
}

fun humanLinePoints(line: Line, delta: Int = 50, maxOffset: Float = 5f) =
    humanLinePoints(line.a, line.b, delta, maxOffset)


fun Canvas.drawHumanRect(r: Rect, color: Paint) {
    val p = r.points()
    val path =
        humanLinePoints(p[0], p[1]) +
            humanLinePoints(p[1], p[2]) +
            humanLinePoints(p[2], p[3]) +
            humanLinePoints(p[3], p[0])

    val s = chaikinSmooth(path).toClosedPath()

    this.drawPath(s, color)
}
