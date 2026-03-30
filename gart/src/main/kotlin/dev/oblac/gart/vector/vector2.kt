package dev.oblac.gart.vector

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Radians
import dev.oblac.gart.angle.cosf
import dev.oblac.gart.angle.sinf
import dev.oblac.gart.math.frac
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun vec2(x: Number, y: Number) = Vec2(x, y)

data class Vec2(val x: Float, val y: Float) {
    constructor(x: Number, y: Number) : this(x.toFloat(), y.toFloat())

    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun plus(scalar: Number) = Vec2(x + scalar.toFloat(), y + scalar.toFloat())
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun minus(scalar: Number) = Vec2(x - scalar.toFloat(), y - scalar.toFloat())
    operator fun times(scalar: Number) = Vec2(x * scalar.toFloat(), y * scalar.toFloat())
    operator fun times(other: Vec2) = Vec2(x * other.x, y * other.y)
    operator fun div(scalar: Number) = Vec2(x / scalar.toFloat(), y / scalar.toFloat())
    operator fun div(other: Vec2) = Vec2(x / other.x, y / other.y)

    fun dot(other: Vec2) = x * other.x + y * other.y
    fun cross(other: Vec2) = x * other.y - y * other.x

    fun length() = sqrt(x * x + y * y)
    val magnitude by lazy { length() }

    fun normalize(): Vec2 {
        return if (magnitude == 0f) this
        else this / magnitude
    }

    /**
     * Returns a new vector that is the result of rotating this vector by the given angle.
     */
    fun rotate(angle: Float): Vec2 {
        val s = sin(angle)
        val c = cos(angle)
        return Vec2(
            x * c - y * s,
            x * s + y * c
        )
    }

    /**
     * Returns the angle of the vector in radians.
     */
    val angle by lazy { Radians.of(atan2(y, x)) }

    companion object {
        val ZERO = Vec2(0f, 0f)

        fun of(angle: Angle): Vec2 {
            return Vec2(cosf(angle), sinf(angle))
        }
    }
}

fun sin(v: Vec2) = Vec2(sin(v.x), sin(v.y))
fun frac(v: Vec2) = Vec2(frac(v.x), frac(v.y))
fun length(v: Vec2) = sqrt(v.x * v.x + v.y * v.y)
