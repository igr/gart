package dev.oblac.gart.math

import org.jetbrains.skia.Point
import kotlin.math.sqrt

/**
 * Length of the vector ([x], [y]) with the exact [sqrt]: the same bits as a written-out
 * `sqrt(x * x + y * y)`, so it can replace one without moving a pixel. [dist] and [hypotFast]
 * go through [fastSqrt] and are only close.
 */
fun length(x: Float, y: Float): Float = sqrt(x * x + y * y)

fun length(x: Double, y: Double): Double = sqrt(x * x + y * y)

/**
 * Fast distance calculation using fast square root approximation.
 */
fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return fastSqrt(dx * dx + dy * dy)
}

fun dist(p1: Point, p2: Point): Float {
    return dist(p1.x, p1.y, p2.x, p2.y)
}

fun distSquared(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return dx * dx + dy * dy
}

fun distSquared(p1: Point, p2: Point): Float {
    return distSquared(p1.x, p1.y, p2.x, p2.y)
}
