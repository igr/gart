package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import dev.oblac.gart.math.Vector
import org.jetbrains.skia.Point

fun Point.isInside(dimension: Dimension) =
    ((x >= 0f) && (x < dimension.w)) && ((y >= 0f) && (y < dimension.h))

fun Point.offset(vec: Vector) = this.offset(vec.x, vec.y)

fun Point.fromCenter(d: Dimension, fl: Float = 1f): Point {
    val x = d.cx + x * fl
    val y = d.cy + y * fl
    return Point(x, y)
}
