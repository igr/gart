package dev.oblac.gart.vector

import dev.oblac.gart.math.DOUBLE_PIf
import kotlin.math.cos
import kotlin.math.pow
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
    operator fun div(scalar: Number) = Vector3(x / scalar.toFloat(), y / scalar.toFloat(), z / scalar.toFloat())

    fun pow(other: Vector3) = Vector3(x.pow(other.x), y.pow(other.y), z.pow(other.z))

    fun length() = sqrt(x * x + y * y + z * z)
    fun normalize() = this * (1.0f / length())
    fun dot(other: Vector3) = x * other.x + y * other.y + z * other.z

    companion object {
        fun of(a: Float) = Vector3(a, a, a)
        fun of(v: Vector2, a: Float) = Vector3(v.x, v.y, a)
        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)
        val TWO_PI = Vector3(DOUBLE_PIf, DOUBLE_PIf, DOUBLE_PIf)
    }
}

fun sin(v: Vec3) = Vec3(sin(v.x), sin(v.y), sin(v.z))
fun cos(v: Vec3) = Vec3(cos(v.x), cos(v.y), cos(v.z))
fun mix(a: Vec3, b: Vec3, t: Float) = a * (1f - t) + b * t
fun mix(a: Vec3, b: Vec3, t: Vec3) = a * (Vec3.ONE - t) + b * t
fun abs(v: Vec3) = Vec3(kotlin.math.abs(v.x), kotlin.math.abs(v.y), kotlin.math.abs(v.z))
