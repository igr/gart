package dev.oblac.gart.angles

import dev.oblac.gart.math.DOUBLE_PIf
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
        val ZERO: Radians = Radians(0f)
    }
}

fun cos(r: Radians): Double {
    return kotlin.math.cos(r.value.toDouble())
}

fun sin(r: Radians): Double {
    return kotlin.math.sin(r.value.toDouble())
}

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
}

fun cos(d: Degrees): Double =
    kotlin.math.cos(d.radians().value.toDouble())

fun sin(d: Degrees): Double =
    kotlin.math.sin(d.radians().value.toDouble())