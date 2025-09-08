package dev.oblac.gart.vector

import dev.oblac.gart.angle.*
import kotlin.math.atan2
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
    val magnitude by lazy { length() }

    fun normalize(): Vector2 {
        return if (magnitude == 0f) this
        else this / magnitude
    }

    /**
     * Returns a new vector that is the result of rotating this vector by the given angle.
     */
    fun rotate(angle: Angle) = Vector2(
        x * cos(angle) - y * sin(angle),
        x * sin(angle) + y * cos(angle)
    )

    /**
     * Returns the angle of the vector in radians.
     */
    val angle by lazy { Radians.of(atan2(y, x)) }

    companion object {
        val ZERO = Vector2(0f, 0f)

        fun of(angle: Angle): Vector2 {
            return Vector2(cosf(angle), sinf(angle))
        }
    }
}
