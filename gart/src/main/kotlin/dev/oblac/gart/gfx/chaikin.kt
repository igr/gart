package dev.oblac.gart.gfx

import dev.oblac.gart.openrndr.toOpenrndrVector2
import dev.oblac.gart.openrndr.toSkikoPoint
import org.jetbrains.skia.Point

fun chaikinSmooth(
    polyline: List<Point>, iterations: Int = 1, closed: Boolean = false, bias: Double = 0.25
): List<Point> {

    return polyline
        .map { it.toOpenrndrVector2() }
        .let { org.openrndr.math.chaikinSmooth(it, iterations, closed, bias) }
        .map { it.toSkikoPoint() }
}
