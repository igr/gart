package dev.oblac.gart.gfx

import dev.oblac.gart.angles.Radians
import dev.oblac.gart.angles.cos
import dev.oblac.gart.angles.sin
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

data class Circle(val x: Float, val y: Float, val radius: Float) {
    val center = Point(x, y)

    companion object {
        fun of(center: Point, radius: Number): Circle = Circle(center.x, center.y, radius.toFloat())
    }

    fun contains(x: Float, y: Float): Boolean {
        val dx = x - this.x
        val dy = y - this.y
        return dx * dx + dy * dy <= radius * radius
    }

    fun contains(p: Point): Boolean {
        val dx = p.x - this.x
        val dy = p.y - this.y
        return dx * dx + dy * dy <= radius * radius
    }

    fun rect() = Rect(x - radius, y - radius, x + radius, y + radius)
}

fun Canvas.drawCircle(circle: Circle, paint: Paint) = drawCircle(circle.center.x, circle.center.y, circle.radius, paint)

fun createCircle(center: Point, radius: Float, steps: Int): List<Point> {
    val points = mutableListOf<Point>()
    val deltaAngle = Radians(360f / steps)
    for (i in 0 until steps) {
        val angle = deltaAngle * i
        val x = center.x + radius * cos(angle)
        val y = center.y - radius * sin(angle)
        points.add(pointOf(x, y))
    }
    return points
}
