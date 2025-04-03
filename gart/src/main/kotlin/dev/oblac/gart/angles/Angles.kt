package dev.oblac.gart.angles

import dev.oblac.gart.math.DOUBLE_PIf
import dev.oblac.gart.math.HALF_PIf
import dev.oblac.gart.math.PIf

@JvmInline
value class Radians(internal val value: Float) {
    fun degrees() = Degrees(value * 180 / PIf)

    operator fun plus(r: Radians) =
        Radians(value + r.value)

    operator fun plus(n: Number) =
        Radians(value + n.toFloat())

    operator fun minus(r: Radians) =
        Radians(value - r.value)

    operator fun minus(n: Number) =
        Radians(value - n.toFloat())

    operator fun times(n: Number): Radians =
        Radians(n.toFloat() * this.value)

    operator fun div(n: Number): Radians =
        Radians(this.value / n.toFloat())

    operator fun unaryMinus(): Radians = Radians(-value)

    fun toFloat() = value

    fun normalize(): Radians {
        var result = value
        while (result < 0) {
            result += DOUBLE_PIf
        }
        while (result > DOUBLE_PIf) {
            result -= DOUBLE_PIf
        }
        return Radians(result)
    }

    companion object {
        val ZERO = Radians(0f)
        val PI_HALF = Radians(HALF_PIf)
        val PI = Radians(PIf)
        val TWO_PI = Radians(DOUBLE_PIf)
        fun of(value: Number): Radians = Radians(value.toFloat())
    }
}

fun cos(r: Radians) =
    kotlin.math.cos(r.value.toDouble())
fun cosf(r: Radians) =
    kotlin.math.cos(r.value.toDouble()).toFloat()

fun sin(r: Radians) =
    kotlin.math.sin(r.value.toDouble())
fun sinf(r: Radians) =
    kotlin.math.sin(r.value.toDouble()).toFloat()

/**
 * Calculates the middle angle between two angles.
 * The result is always in the range of: -PI..PI.
 * This means that the result is always the shortest angle between the two angles.
 */
fun middleAngle(a: Radians, b: Radians): Radians {
    return when (val diff = (b - a).value) {
        in -PIf..PIf -> (a + diff / 2).normalize()
        in PIf..DOUBLE_PIf -> (a + diff / 2 - PIf).normalize()
        in -DOUBLE_PIf..-PIf -> (a + diff / 2 + PIf).normalize()
        in DOUBLE_PIf..2 * DOUBLE_PIf -> (a + (diff - DOUBLE_PIf) / 2).normalize()
        in -2 * DOUBLE_PIf..-DOUBLE_PIf -> (a + (diff + DOUBLE_PIf) / 2).normalize()
        else -> throw IllegalStateException("Unexpected angle difference: $diff")
    }
}


// --- DEGREES ---


@JvmInline
value class Degrees(internal val value: Float) {
    fun radians() = Radians(value * PIf / 180)

    operator fun plus(delta: Degrees): Degrees =
        Degrees(value + delta.value)

    operator fun minus(delta: Degrees): Degrees =
        Degrees(value - delta.value)

    operator fun times(n: Number): Degrees =
        Degrees(n.toFloat() * this.value)

    fun normalize(): Degrees {
        var result = value
        while (result < 0) {
            result += 360
        }
        while (result > 360) {
            result -= 360
        }
        return Degrees(result)
    }

    operator fun unaryMinus(): Degrees = Degrees(-value)

    companion object {
        val ZERO = Degrees(0f)
        val D90 = Degrees(90f)
        val D180 = Degrees(180f)
        val D360 = Degrees(360f)
        fun of(value: Number): Degrees = Degrees(value.toFloat())
    }
}

fun cos(d: Degrees): Double =
    kotlin.math.cos(d.radians().value.toDouble())
fun cosf(d: Degrees): Float =
    kotlin.math.cos(d.radians().value.toDouble()).toFloat()


fun sin(d: Degrees): Double =
    kotlin.math.sin(d.radians().value.toDouble())
fun sinf(d: Degrees): Float =
    kotlin.math.sin(d.radians().value.toDouble()).toFloat()
