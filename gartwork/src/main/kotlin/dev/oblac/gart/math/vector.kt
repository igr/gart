package dev.oblac.gart.math

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class Vector(val x: Float, val y: Float) {
    constructor(x: Number, y: Number) : this(x.toFloat(), y.toFloat())
    operator fun plus(other: Vector) = Vector(x + other.x, y + other.y)
    operator fun minus(other: Vector) = Vector(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector(x * scalar, y * scalar)
    operator fun div(scalar: Float) = Vector(x / scalar, y / scalar)
    fun dot(other: Vector) = x * other.x + y * other.y
    fun cross(other: Vector) = x * other.y - y * other.x

    val magnitude by lazy { fastSqrt(x * x + y * y) }
    fun normalize() = this / magnitude
    fun rotate(angle: Float) = Vector(
        x * cos(angle) - y * sin(angle),
        x * sin(angle) + y * cos(angle)
    )

    /**
     * Returns the angle of the vector in radians.
     */
    val angle by lazy { atan2(y, x) }

    companion object {
        val ZERO = Vector(0f, 0f)
    }
}
