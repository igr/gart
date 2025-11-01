package dev.oblac.gart.gfx

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.angle.cos
import dev.oblac.gart.angle.sin
import org.jetbrains.skia.Point

/**
 * Creates a spiral with the given parameters.
 * @param center the center of the spiral
 * @param radius the maximum radius of the spiral
 * @param steps the number of steps in the spiral
 * @param offset the initial offset of the spiral
 * @param loop the number of times the spiral loops around, default 1
 */
fun createSpiral(center: Point, radius: Float, steps: Int, offset: Angle, loop: Int = 1): List<Point> {
    val points = mutableListOf<Point>()
    var angle = offset
    var r = 0f
    val deltaR = radius / steps
    val deltaAngle = Degrees.of(loop * 360f) / steps
    for (i in 0 until steps) {
        // create spiral with polar coordinates
        val x = center.x + r * cos(angle)
        val y = center.y - r * sin(angle)
        points.add((x to y).toPoint())
        angle += deltaAngle
        r += deltaR
    }

    return points
}

