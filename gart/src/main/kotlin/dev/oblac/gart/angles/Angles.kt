package dev.oblac.gart.angles

import dev.oblac.gart.math.PIf

@JvmInline
value class Radians(internal val value: Float) {
    fun degrees() = Degrees(value * 180 / PIf)

    operator fun plus(delta: Radians) =
        Radians(value + delta.value)

    operator fun times(value: Number): Radians =
        Radians(value.toFloat() * this.value)

    operator fun div(value: Number): Radians =
        Radians(this.value / value.toFloat())

    fun toFloat() = value

    companion object {
        val ZERO: Radians = Radians(0f)
    }
}

//operator fun Number.times(radians: Radians) = Radians(this.toFloat() * radians.value)

fun cos(r: Radians): Double {
    return kotlin.math.cos(r.value.toDouble())
}

fun sin(r: Radians): Double {
    return kotlin.math.sin(r.value.toDouble())
}

@JvmInline
value class Degrees(internal val value: Float) {
    fun radians() = Radians(value * PIf / 180)

    operator fun plus(delta: Degrees): Degrees {
        return Degrees(value + delta.value)
    }
}

//operator fun Number.times(degrees: Degrees) = Degrees(this.toFloat() * degrees.value)

fun cos(d: Degrees): Double =
    kotlin.math.cos(d.radians().value.toDouble())

fun sin(d: Degrees): Double =
    kotlin.math.sin(d.radians().value.toDouble())
