package dev.oblac.gart.vector

import kotlin.math.sqrt

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
