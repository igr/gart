package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import dev.oblac.gart.math.Vector2
import dev.oblac.gart.math.dist
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

fun Point.isInside(dimension: Dimension) =
    ((x >= 0f) && (x < dimension.w)) && ((y >= 0f) && (y < dimension.h))

fun Point.ifInside(dimension: Dimension): Point? = if (isInside(dimension)) this else null

fun Point.isInside(rect: Rect) =
    ((x >= rect.left) && (x < rect.right)) && ((y >= rect.top) && (y < rect.bottom))

fun Point.offset(vec: Vector2) = this.offset(vec.x, vec.y)

fun Point.fromCenter(d: Dimension, fl: Float = 1f): Point {
    val x = d.cx + x * fl
    val y = d.cy + y * fl
    return Point(x, y)
}

/**
 * Returns the distance between two points.
 * @see dist
 */
fun Point.distanceTo(p: Point): Float {
    return dist(this, p)
}

/**
 * Returns the point between two points.
 */
fun pointBetween(p1: Point, p2: Point): Point {
    val x = (p1.x + p2.x) / 2
    val y = (p1.y + p2.y) / 2
    return Point(x, y)
}

fun randomPointBetween(p1: Point, p2: Point): Point {
    if (p1.x != p2.x) {
        val slope = (p2.y - p1.y) / (p2.x - p1.x)
        val rndf = rndf(0f, 1f)
        val x = p1.x + rndf * (p2.x - p1.x)
        val y = slope * (x - p1.x) + p1.y
        return Point(x, y)
    }
    return Point(p1.x, rndf(p1.y, p2.y))
}
