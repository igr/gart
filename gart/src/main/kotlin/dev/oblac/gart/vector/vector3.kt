package dev.oblac.gart.vector

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

typealias Vec3 = Vector3

data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun plus(scalar: Number) = Vector3(x + scalar.toFloat(), y + scalar.toFloat(), z + scalar.toFloat())
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Number) = Vector3(x * scalar.toFloat(), y * scalar.toFloat(), z * scalar.toFloat())
    operator fun times(other: Vector3) = Vector3(x * other.x, y * other.y, z * other.z)
    fun length() = sqrt(x * x + y * y + z * z)
    fun normalize() = this * (1.0f / length())
    fun dot(other: Vector3) = x * other.x + y * other.y + z * other.z
}

fun sin(v: Vec3) = Vec3(sin(v.x), sin(v.y), sin(v.z))
fun cos(v: Vec3) = Vec3(cos(v.x), cos(v.y), cos(v.z))
