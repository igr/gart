package dev.oblac.gart.harmongraph

import org.jetbrains.skia.Point
import kotlin.math.exp
import kotlin.math.sin

fun harmongraph2(
    iterations: Int,
    delta: Float,
    a: Float,
    b: Float,
    f1: Float,
    f2: Float,
    p1: Float,
    p2: Float,
    d1: Float,
    d2: Float,
): List<Point> {
    return generateSequence(0.0f) { it + delta }
        .take(iterations)
        .map { t ->
            val x = a * sin(f1 * t + p1) * exp(d1 * t)
            val y = b * sin(f2 * t + p2) * exp(d2 * t)
            Point(x, y)
        }
        .toList()
}

fun harmongraph4(
    iterations: Int,
    delta: Float,
    a1: Float,
    a2: Float,
    b1: Float,
    b2: Float,
    f1: Float,
    f2: Float,
    g1: Float,
    g2: Float,
    p1: Float,
    p2: Float,
    q1: Float,
    q2: Float,
    d1: Float,
    d2: Float,
    e1: Float,
    e2: Float,
): List<Point> {
    return generateSequence(0.0f) { it + delta }
        .take(iterations)
        .map { t ->
            val x = a1 * sin(f1 * t + p1) * exp(d1 * t) + a2 * sin(f2 * t + p2) * exp(d2 * t)
            val y = b1 * sin(g1 * t + q1) * exp(e1 * t) + b2 * sin(g2 * t + q2) * exp(e2 * t)
            Point(x, y)
        }
        .toList()
}
