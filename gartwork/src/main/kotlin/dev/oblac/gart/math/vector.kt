package dev.oblac.gart.math

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class Vector(val x: Float, val y: Float) {
    operator fun plus(other: Vector) = Vector(x + other.x, y + other.y)
    operator fun minus(other: Vector) = Vector(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector(x * scalar, y * scalar)
    operator fun div(scalar: Float) = Vector(x / scalar, y / scalar)
    fun dot(other: Vector) = x * other.x + y * other.y
    fun cross(other: Vector) = x * other.y - y * other.x
    fun length() = fastSqrt(x * x + y * y)
    fun magnitude() = length()
    fun normalize() = this / length()
    fun rotate(angle: Float) = Vector(
        x * cos(angle) - y * sin(angle),
        x * sin(angle) + y * cos(angle)
    )

    /**
     * Returns the angle of the vector in radians.
     */
    fun angle() = atan2(y, x)

    companion object {
        val ZERO = Vector(0f, 0f)
    }
}
