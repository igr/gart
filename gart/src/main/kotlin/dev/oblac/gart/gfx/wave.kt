package dev.oblac.gart.gfx

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.angle.cos
import org.jetbrains.skia.Point

/**
 * Creates a Wave path between two points.
 * The wave starts at the first point and gradually transitions to the second point,
 * spiraling around the line connecting them.
 * @param start the starting point of the spiral
 * @param end the ending point of the spiral
 * @param steps the number of steps in the spiral
 * @param maxRadius the maximum radius of the spiral (at the midpoint)
 * @param loops the number of complete rotations around the center line
 * @param offset the initial angular offset
 */
fun createWaveBetweenPoints(
    start: Point,
    end: Point,
    steps: Int,
    maxRadius: Float,
    loops: Int = 3,
    offset: Angle = Degrees.of(0f)
): List<Point> {
    val points = mutableListOf<Point>()

    // Calculate the direction and length between points
    val dx = end.x - start.x
    val dy = end.y - start.y
    val baseAngle = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat()

    // Perpendicular direction for spiral offset
    val perpAngle = baseAngle + kotlin.math.PI.toFloat() / 2

    var angle = offset
    val deltaAngle = Degrees.of(loops * 360f) / steps

    for (i in 0 until steps) {
        // Progress along the line from start to end (0.0 to 1.0)
        val t = i.toFloat() / (steps - 1)

        // Linear interpolation between start and end points
        val baseX = start.x + dx * t
        val baseY = start.y + dy * t

        // Radius varies: starts at 0, grows to maxRadius at middle, shrinks back to 0
        val radiusFactor = kotlin.math.sin((t * kotlin.math.PI).toFloat())
        val r = maxRadius * radiusFactor

        // Spiral offset perpendicular to the line
        val spiralOffset = r * cos(angle)
        val x = baseX + spiralOffset * kotlin.math.cos(perpAngle.toDouble()).toFloat()
        val y = baseY + spiralOffset * kotlin.math.sin(perpAngle.toDouble()).toFloat()

        points.add(Point(x, y))
        angle += deltaAngle
    }

    return points
}

