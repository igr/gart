package dev.oblac.gart.vector

import dev.oblac.gart.math.DOUBLE_PIf
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

typealias Vec3 = Vector3

fun vec3(x: Number, y: Number, z: Number) = Vector3(x.toFloat(), y.toFloat(), z.toFloat())

data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun plus(scalar: Number) = Vector3(x + scalar.toFloat(), y + scalar.toFloat(), z + scalar.toFloat())
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Number) = Vector3(x * scalar.toFloat(), y * scalar.toFloat(), z * scalar.toFloat())
    operator fun times(other: Vector3) = Vector3(x * other.x, y * other.y, z * other.z)
    fun length() = sqrt(x * x + y * y + z * z)
    fun normalize() = this * (1.0f / length())
    fun dot(other: Vector3) = x * other.x + y * other.y + z * other.z

    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)
        val TWO_PI = Vector3(DOUBLE_PIf, DOUBLE_PIf, DOUBLE_PIf)
    }
}

fun sin(v: Vec3) = Vec3(sin(v.x), sin(v.y), sin(v.z))
fun cos(v: Vec3) = Vec3(cos(v.x), cos(v.y), cos(v.z))
