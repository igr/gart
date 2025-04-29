package dev.oblac.gart.gfx

import dev.oblac.gart.angles.Degrees
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


fun Canvas.drawCircleArc(
    x: Float,
    y: Float,
    radius: Float,
    paint: Paint,
    start: Degrees = Degrees.ZERO,
    sweep: Degrees = Degrees.D180
) {
    val left = x - radius
    val top = y - radius
    val right = x + radius
    val bottom = y + radius

    // Draw a arc starting at `angleDegrees`
    this.drawArc(
        left,
        top,
        right,
        bottom,
        start.value,    // start angle in degrees
        sweep.value,    // sweep angle
        false,          // don't create PIE shape with center
        paint
    )
}

fun Canvas.drawCirclePie(
    x: Float,
    y: Float,
    radius: Float,
    paint: Paint,
    start: Degrees = Degrees.ZERO,
    sweep: Degrees = Degrees.D180
) {
    val left = x - radius
    val top = y - radius
    val right = x + radius
    val bottom = y + radius

    // Draw a arc starting at `angleDegrees`
    this.drawArc(
        left,
        top,
        right,
        bottom,
        start.value,    // start angle in degrees
        sweep.value,    // sweep angle
        true,           // create PIE shape with center
        paint
    )
}
