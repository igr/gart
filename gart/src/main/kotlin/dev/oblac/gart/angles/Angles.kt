package dev.oblac.gart.angles

import dev.oblac.gart.math.DOUBLE_PIf
import dev.oblac.gart.math.HALF_PIf
import dev.oblac.gart.math.PIf

private const val RADIANS_TO_DEGREES = 180f / PIf

sealed interface Angle {
    /**
     * Returns value in radians.
     */
    fun radians(): Float

    /**
     * Returns value in degrees.
     */
    fun degrees(): Float

    operator fun plus(angle: Angle): Angle {
        return when (this) {
            is Radians -> Radians(radians() + angle.radians())
            is Degrees -> Degrees(degrees() + angle.degrees())
        }
    }

    operator fun minus(angle: Angle): Angle {
        return when (this) {
            is Radians -> Radians(radians() - angle.radians())
            is Degrees -> Degrees(degrees() - angle.degrees())
        }
    }

    operator fun times(scale: Number): Angle {
        return when (this) {
            is Radians -> Radians(radians() * scale.toFloat())
            is Degrees -> Degrees(degrees() * scale.toFloat())
        }
    }

    operator fun div(scale: Number): Angle {
        return when (this) {
            is Radians -> Radians(radians() / scale.toFloat())
            is Degrees -> Degrees(degrees() / scale.toFloat())
        }
    }

    operator fun unaryMinus(): Angle {
        return when (this) {
            is Radians -> Radians(-radians())
            is Degrees -> Degrees(-degrees())
        }
    }

    operator fun compareTo(other: Angle): Int {
        return when (this) {
            is Radians -> radians().compareTo(other.radians())
            is Degrees -> degrees().compareTo(other.degrees())
        }
    }

    fun normalize(): Angle
}

@JvmInline
value class Radians(val value: Float) : Angle {
    override fun radians() = value
    override fun degrees() = value * RADIANS_TO_DEGREES

    override fun normalize(): Radians {
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
        fun of(value: Number): Angle = Radians(value.toFloat())
    }
}

// --- DEGREES ---

private const val DEGREES_TO_RADIANS = PIf / 180f

@JvmInline
value class Degrees(val value: Float) : Angle {
    override fun radians() = value * DEGREES_TO_RADIANS
    override fun degrees() = value

    override fun normalize(): Degrees {
        var result = value
        while (result < 0) {
            result += 360
        }
        while (result > 360) {
            result -= 360
        }
        return Degrees(result)
    }

    companion object {
        val ZERO = Degrees(0f)
        val D90 = Degrees(90f)
        val D180 = Degrees(180f)
        val D270 = Degrees(270f)
        val D360 = Degrees(360f)
        fun of(value: Number): Angle = Degrees(value.toFloat())
    }
}

fun cos(a: Angle): Double =
    kotlin.math.cos(a.radians().toDouble())

fun cosf(a: Angle): Float =
    kotlin.math.cos(a.radians().toDouble()).toFloat()


fun sin(a: Angle): Double =
    kotlin.math.sin(a.radians().toDouble())

fun sinf(a: Angle): Float =
    kotlin.math.sin(a.radians().toDouble()).toFloat()

/**
 * Calculates the middle angle between two angles.
 * The result is always in the range of: -PI..PI.
 * This means that the result is always the shortest angle between the two angles.
 */
fun middleAngle(a: Angle, b: Angle): Angle {
    val diff = (b - a).radians()
    return when (diff) {
        in -PIf..PIf -> (a + (diff / 2).radians()).normalize()
        in PIf..DOUBLE_PIf -> (a + (diff / 2 - PIf).radians()).normalize()
        in -DOUBLE_PIf..-PIf -> (a + (diff / 2 + PIf).radians()).normalize()
        in DOUBLE_PIf..2 * DOUBLE_PIf -> (a + ((diff - DOUBLE_PIf) / 2).radians()).normalize()
        in -2 * DOUBLE_PIf..-DOUBLE_PIf -> (a + ((diff + DOUBLE_PIf) / 2).radians()).normalize()
        else -> throw IllegalStateException("Unexpected angle difference: $diff")
    }
}

fun Number.radians(): Radians = Radians(this.toFloat())
fun Number.degrees(): Degrees = Degrees(this.toFloat())
