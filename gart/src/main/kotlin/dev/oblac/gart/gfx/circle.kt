package dev.oblac.gart.gfx

import dev.oblac.gart.angles.Radians
import dev.oblac.gart.angles.cos
import dev.oblac.gart.angles.sin
import org.jetbrains.skia.Point

fun createCircle(center: Point, radius: Float, steps: Int): List<Point> {
    val points = mutableListOf<Point>()
    val deltaAngle = Radians(360f / steps)
    for (i in 0 until steps) {
        val angle = deltaAngle * i
        val x = center.x + radius * cos(angle)
        val y = center.y - radius * sin(angle)
        points.add(Point(x, y))
    }
    return points
}
