package dev.oblac.gart.gfx

import org.jetbrains.skia.Point
import kotlin.math.exp
import kotlin.math.sin

fun harmongraph(
    iterations: Int,
    delta: Float,
    A: Float,
    B: Float,
    a: Float,
    b: Float,
    p1: Float,
    p2: Float,
    d1: Float,
    d2: Float,
): List<Point> {
    return generateSequence(0.0f) { it + delta }
        .take(iterations)
        .map { t ->
            val x = A * sin(a * t + p1) * exp(d1 * t)
            val y = B * sin(b * t + p2) * exp(d2 * t)
            Point(x, y)
        }
        .toList()
}
