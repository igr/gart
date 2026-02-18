package dev.oblac.gart.smooth

import dev.oblac.gart.gfx.toPath
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point

fun List<Point>.toSmoothQuadraticPath() = drawSmoothQuadratic(this)

/**
 * Draws a smooth quadratic Bezier curve through the given points.
 * Each segment between points is represented as a quadratic curve,
 * using the current point as the control point and the midpoint to the next point as the end point.
 *
 * @param points The list of points to create the smooth quadratic curve through.
 * @return A Path representing the smooth quadratic Bezier curve.
 */
fun drawSmoothQuadratic(points: List<Point>): Path {
    if (points.size < 2) return points.toPath()

    val path = PathBuilder()
    path.moveTo(points[0].x, points[0].y)

    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
    } else {
        // Start with line to midpoint between first two points
        val midX = (points[0].x + points[1].x) / 2
        val midY = (points[0].y + points[1].y) / 2
        path.lineTo(midX, midY)

        // For each point, draw quadratic curve through it
        for (i in 1 until points.size - 1) {
            val nextMidX = (points[i].x + points[i + 1].x) / 2
            val nextMidY = (points[i].y + points[i + 1].y) / 2

            // Use current point as control point
            path.quadTo(
                points[i].x, points[i].y,  // control point
                nextMidX, nextMidY         // end point
            )
        }

        // Line to the last point
        path.lineTo(points.last().x, points.last().y)
    }

    return path.detach()
}
