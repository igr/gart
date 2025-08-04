package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import dev.oblac.gart.angles.Radians
import dev.oblac.gart.angles.cosf
import dev.oblac.gart.angles.sinf
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Point
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

fun Point.copy() = Point(this.x, this.y)

fun randomPoint(d: Dimension) = Point(rndf(0f, d.wf), rndf(0f, d.hf))

fun randomPoints(d: Dimension, count: Int) = List(count) { randomPoint(d) }

/**
 * Random point in radius.
 */
fun randomPoint(cx: Float, cy: Float, rmax: Float, rmin: Float = rmax): Point {
    val angle = rndf(0f, 360f)
    val r = rndf(rmin, rmax)
    val x = cx + r * cos(angle)
    val y = cy + r * sin(angle)
    return Point(x, y)
}

fun randomPoint(min: Point, max: Point) = Point(rndf(min.x, max.x), rndf(min.y, max.y))

fun Point.Companion.random(d: Dimension): Point = randomPoint(d)
fun Point.Companion.random(w: Number, h: Number): Point = Point(rndf(0f, w.toFloat()), rndf(0f, h.toFloat()))

fun Pair<Number, Number>.toPoint(): Point = Point(first.toFloat(), second.toFloat())
fun pointOf(x: Number, y: Number): Point = Point(x.toFloat(), y.toFloat())

fun Point.isCloseTo(other: Point, tolerance: Float): Boolean {
    return distanceTo(other) < tolerance
}

fun Point(x: Number, y: Number) = Point(x.toFloat(), y.toFloat())


/**
 * Move point towards destination by given amount.
 */
fun Point.moveTowards(destination: Point, amount: Float): Point {
    val dx = destination.x - x
    val dy = destination.y - y
    val angle = atan2(dy.toDouble(), dx.toDouble())
    val x = this.x + cos(angle) * amount
    val y = this.y + sin(angle) * amount
    return Point(x.toFloat(), y.toFloat())
}

fun Point.rotate(angle: Radians, rx: Float, ry: Float): Point {
    val cos = cosf(angle)
    val sin = sinf(angle)

    val translatedX = this.x - rx
    val translatedY = this.y - ry

    val x = translatedX * cos - translatedY * sin + rx
    val y = translatedX * sin + translatedY * cos + ry

    return Point(x, y)
}

fun Point.isInside(circle: Circle) = circle.center.distanceTo(this) < circle.radius
fun Point.isOnLine(line: Line) = line.isPointOnLine(this)

operator fun Point.plus(other: Point): Point {
    return Point(this.x + other.x, this.y + other.y)
}

operator fun Point.minus(other: Point): Point {
    return Point(this.x - other.x, this.y - other.y)
}

operator fun Point.times(number: Number): Point {
    return Point(this.x * number.toFloat(), this.y * number.toFloat())
}
