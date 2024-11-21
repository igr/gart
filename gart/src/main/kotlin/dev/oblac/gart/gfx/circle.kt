package dev.oblac.gart.gfx

import dev.oblac.gart.angles.Radians
import dev.oblac.gart.angles.cos
import dev.oblac.gart.angles.sin
import org.jetbrains.skia.Point

data class Circle(val x: Float, val y: Float, val radius: Float) {
    val center = Point(x, y)

    companion object {
        fun of(center: Point, radius: Number): Circle = Circle(center.x, center.y, radius.toFloat())
    }
}

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
