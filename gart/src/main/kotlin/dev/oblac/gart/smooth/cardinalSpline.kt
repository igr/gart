package dev.oblac.gart.smooth

import dev.oblac.gart.gfx.toPath
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point

fun List<Point>.toCardinalSpline(tension: Float = 0.5f, segments: Int = 20) =
    cardinalSpline(this, tension, segments)

/**
 * Generates a cardinal spline path through the given points.
 */
fun cardinalSpline(
    points: List<Point>,
    tension: Float = 0.5f,  // 0 = Catmull-Rom, 1 = straight lines
    segments: Int = 20
): Path {
    if (points.size < 2) return points.toPath()

    val path = PathBuilder()
    path.moveTo(points[0].x, points[0].y)

    for (i in 0 until points.size - 1) {
        val p0 = if (i > 0) points[i - 1] else points[i]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else points[i + 1]

        // Calculate tangents with tension parameter
        val m1x = (1 - tension) * (p2.x - p0.x) / 2
        val m1y = (1 - tension) * (p2.y - p0.y) / 2
        val m2x = (1 - tension) * (p3.x - p1.x) / 2
        val m2y = (1 - tension) * (p3.y - p1.y) / 2

        for (j in 1..segments) {
            val t = j.toFloat() / segments
            val t2 = t * t
            val t3 = t2 * t

            // Hermite basis functions
            val h00 = 2 * t3 - 3 * t2 + 1
            val h10 = t3 - 2 * t2 + t
            val h01 = -2 * t3 + 3 * t2
            val h11 = t3 - t2

            val x = h00 * p1.x + h10 * m1x + h01 * p2.x + h11 * m2x
            val y = h00 * p1.y + h10 * m1y + h01 * p2.y + h11 * m2y

            path.lineTo(x, y)
        }
    }

    return path.detach()
}
