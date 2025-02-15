package dev.oblac.gart.math

import org.jetbrains.skia.Point

class Lissajous(
    private val center: Point,
    private val A: Float,
    private val B: Float,
    private val a: Float,
    private val b: Float,
    private val dx: Float = 0f,
    private val dy: Float = 0f
) {
    private var p = Point(center.x, center.y)
    private var t = 0f

    fun step(delta: Float): Point {
        val x = A * kotlin.math.sin(a * t + dx)
        val y = B * kotlin.math.sin(b * t + dy)
        p = Point(x + center.x, y + center.y)
        t += delta
        return p
    }

    fun position() = p
}
