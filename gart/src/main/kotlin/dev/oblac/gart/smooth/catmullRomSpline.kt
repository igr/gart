package dev.oblac.gart.smooth

import dev.oblac.gart.gfx.toPath
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point

fun List<Point>.toCatmullRomSpline(segments: Int = 20) = catmullRomSpline(this, segments)

/**
 * Creates a smooth Catmull-Rom spline through the given points.
 * @param points The list of points to create the spline through.
 * @param segments The number of segments between each pair of points.
 * @return A Path representing the smooth Catmull-Rom spline.
 */
fun catmullRomSpline(points: List<Point>, segments: Int = 20): Path {
    if (points.size < 2) return points.toPath()

    val path = PathBuilder()
    path.moveTo(points[0].x, points[0].y)

    for (i in 0 until points.size - 1) {
        val p0 = if (i > 0) points[i - 1] else points[i]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else points[i + 1]

        for (j in 1..segments) {
            val t = j.toFloat() / segments
            val point = catmullRomPoint(p0, p1, p2, p3, t)
            path.lineTo(point.x, point.y)
        }
    }
    return path.detach()
}

private fun catmullRomPoint(p0: Point, p1: Point, p2: Point, p3: Point, t: Float): Point {
    val t2 = t * t
    val t3 = t2 * t

    val x = 0.5f * (
        2 * p1.x +
            (-p0.x + p2.x) * t +
            (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 +
            (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3
        )

    val y = 0.5f * (
        2 * p1.y +
            (-p0.y + p2.y) * t +
            (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 +
            (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3
        )

    return Point(x, y)
}
