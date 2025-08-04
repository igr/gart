package dev.oblac.gart.math

import org.jetbrains.skia.Point

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
