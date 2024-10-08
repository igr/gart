package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import dev.oblac.gart.math.Vector2
import dev.oblac.gart.math.dist
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
