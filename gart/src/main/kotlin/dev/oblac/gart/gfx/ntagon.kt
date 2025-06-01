package dev.oblac.gart.gfx

import dev.oblac.gart.math.PIf
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

/**
 * Creates an N-sided polygon point path.
 *
 * @param n Number of sides (must be >= 3)
 * @param centerX X coordinate of the center
 * @param centerY Y coordinate of the center
 * @param radius Distance from center to vertices
 * @param startAngle Starting angle in radians (default: 0 starts from right side)
 * @param clockwise Direction of vertex generation (default: false for counter-clockwise)
 * @return Path object representing the N-gon
 */
fun createNtagonPoints(
    n: Int,
    centerX: Float,
    centerY: Float,
    radius: Float,
    startAngle: Float = 0f,
    clockwise: Boolean = false
): List<Point> {
    require(n >= 3) { "Number of sides must be at least 3" }

    val path = mutableListOf<Point>()
    val angleStep = (2 * PIf / n)

    val firstAngle = startAngle
    val firstX = centerX + radius * cos(firstAngle)
    val firstY = centerY + radius * sin(firstAngle)

    // first vertex
    path.add(Point(firstX, firstY))

    // remaining vertices
    for (i in 1 until n) {
        val angle = if (clockwise) {
            startAngle - i * angleStep
        } else {
            startAngle + i * angleStep
        }

        val x = centerX + radius * cos(angle)
        val y = centerY + radius * sin(angle)
        path.add(Point(x, y))
    }

    return path
}
