package dev.oblac.gart.math

import org.jetbrains.skia.Point

class Lissajous(
    private val center: Point,
    /**
     * Amplitude in X direction
     */
    private val A: Float,
    /**
     * Amplitude in Y direction
     */
    private val B: Float,
    /**
     * Frequency in X direction
     */
    private val a: Float,
    /**
     * Frequency in Y direction
     */
    private val b: Float,
    private val dx: Float = 0f,
    private val dy: Float = 0f,
    var t: Float = 0f
) {
    private var p = Point(center.x, center.y)

    fun step(delta: Float): Point {
        val x = A * kotlin.math.sin(a * t + dx)
        val y = B * kotlin.math.sin(b * t + dy)
        p = Point(x + center.x, y + center.y)
        t += delta
        return p
    }

    fun position() = p
}
