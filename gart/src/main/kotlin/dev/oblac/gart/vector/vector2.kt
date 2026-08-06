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
     * Returns the signed angle in radians from this vector to [other], in `(-PI, PI]`.
     * Rotating this vector by the result aligns it with the direction of [other].
     */
    fun angleTo(other: Vec2): Float = atan2(cross(other), dot(other))

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

/**
 * A [Vec2] you can write into: one instance, reused, for loops that would otherwise mint a
 * fresh vector every step.
 *
 * [Vec2] is not as cheap as it looks — `magnitude` and `angle` are `by lazy`, so constructing
 * one allocates five objects (the vector, two lambdas, two synchronized lazy delegates), not
 * one. That is fine anywhere the vector is a result you keep and a rounding error in the
 * allocation count doesn't matter, and it is not fine in a per-pixel or per-step loop.
 *
 * Use this as an out-parameter there, and [toVec2] at the boundary when you want a real value
 * back. It carries no operators that return a new instance on purpose: everything here mutates
 * in place, so there is no way to write `a + b` and quietly get the allocation back.
 */
class MutableVec2(var x: Float = 0f, var y: Float = 0f) {

    fun set(x: Float, y: Float): MutableVec2 {
        this.x = x
        this.y = y
        return this
    }

    fun set(v: Vec2) = set(v.x, v.y)
    fun set(v: MutableVec2) = set(v.x, v.y)
    fun zero() = set(0f, 0f)

    /** Accumulates raw components, so a caller adding up a sum needs no temporary vector. */
    fun add(dx: Float, dy: Float): MutableVec2 {
        x += dx
        y += dy
        return this
    }

    operator fun plusAssign(v: Vec2) { add(v.x, v.y) }
    operator fun minusAssign(v: Vec2) { add(-v.x, -v.y) }
    operator fun timesAssign(scalar: Float) { set(x * scalar, y * scalar) }
    operator fun divAssign(scalar: Float) { set(x / scalar, y / scalar) }

    operator fun component1() = x
    operator fun component2() = y

    fun length() = sqrt(x * x + y * y)

    /** Freezes the current value into an immutable [Vec2]. */
    fun toVec2() = Vec2(x, y)

    override fun toString() = "MutableVec2($x, $y)"
}
