package dev.oblac.gart.smooth

import org.jetbrains.skia.Path
import org.jetbrains.skia.Point

fun List<Point>.toBSpline(segments: Int = 20) = bSpline(this, segments)

/**
 * Creates a B-Spline curve through the given points.
 * Requires at least 4 points to create the spline.
 */
fun bSpline(points: List<Point>, segments: Int = 20): Path {
    if (points.size < 4) {
        throw IllegalArgumentException("The points need to have at least 4 points")
    }

    val path = Path()

    // Calculate first point
    val startPoint = bSplinePoint(points[0], points[0], points[1], points[2], 0f)
    path.moveTo(startPoint.x, startPoint.y)

    for (i in 0 until points.size - 3) {
        for (j in 0..segments) {
            val t = j.toFloat() / segments
            val point = bSplinePoint(
                points[i],
                points[i + 1],
                points[i + 2],
                points[i + 3],
                t
            )
            path.lineTo(point.x, point.y)
        }
    }

    return path
}

private fun bSplinePoint(p0: Point, p1: Point, p2: Point, p3: Point, t: Float): Point {
    val t2 = t * t
    val t3 = t2 * t

    val b0 = (-t3 + 3 * t2 - 3 * t + 1) / 6f
    val b1 = (3 * t3 - 6 * t2 + 4) / 6f
    val b2 = (-3 * t3 + 3 * t2 + 3 * t + 1) / 6f
    val b3 = t3 / 6f

    val x = b0 * p0.x + b1 * p1.x + b2 * p2.x + b3 * p3.x
    val y = b0 * p0.y + b1 * p1.y + b2 * p2.y + b3 * p3.y

    return Point(x, y)
}
