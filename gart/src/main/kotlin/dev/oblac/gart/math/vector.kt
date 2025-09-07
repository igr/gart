package dev.oblac.gart.math

import dev.oblac.gart.angle.Radians
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

typealias Vec2 = Vector2

data class Vector2(val x: Float, val y: Float) {
    constructor(x: Number, y: Number) : this(x.toFloat(), y.toFloat())

    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(scalar: Number) = Vector2(x * scalar.toFloat(), y * scalar.toFloat())
    operator fun times(other: Vector2) = Vector2(x * other.x, y * other.y)
    operator fun div(scalar: Float) = Vector2(x / scalar, y / scalar)
    fun dot(other: Vector2) = x * other.x + y * other.y
    fun cross(other: Vector2) = x * other.y - y * other.x
    fun length() = sqrt(x * x + y * y)
    val magnitude by lazy { fastSqrt(x * x + y * y) }
    fun normalize() = this / magnitude
    fun rotate(angle: Float) = Vector2(
        x * cos(angle) - y * sin(angle),
        x * sin(angle) + y * cos(angle)
    )

    /**
     * Returns the angle of the vector in radians.
     */
    val angle by lazy { Radians.of(atan2(y, x)) }

    companion object {
        val ZERO = Vector2(0f, 0f)
    }
}

typealias Vec3 = Vector3

data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Number) = Vector3(x * scalar.toFloat(), y * scalar.toFloat(), z * scalar.toFloat())
    operator fun times(other: Vector3) = Vector3(x * other.x, y * other.y, z * other.z)
    fun length() = sqrt(x * x + y * y + z * z)
    fun normalize() = this * (1.0f / length())
    fun dot(other: Vector3) = x * other.x + y * other.y + z * other.z
}

typealias Mtx2 = Matrix2

data class Matrix2(val a: Float, val b: Float, val c: Float, val d: Float) {
    operator fun times(v: Vector2) = Vector2(a * v.x + b * v.y, c * v.x + d * v.y)
}

