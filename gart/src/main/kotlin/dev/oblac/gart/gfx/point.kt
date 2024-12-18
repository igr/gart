package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

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

fun Point.Companion.random(d: Dimension): Point = randomPoint(d)

fun Pair<Number, Number>.toPoint(): Point = Point(first.toFloat(), second.toFloat())
fun pointOf(x: Number, y: Number): Point = Point(x.toFloat(), y.toFloat())

fun Point.isCloseTo(other: Point, tolerance: Float): Boolean {
    return distanceTo(other) < tolerance
}
