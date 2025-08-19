package dev.oblac.gart.color.space

import dev.oblac.gart.color.*
import kotlin.math.roundToInt

data class RGBA(
	val r: Int,
	val g: Int,
	val b: Int,
	val a: Int = 255
) {
    val value: Int = argb(a.coerceIn(0, 255), r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))

    operator fun plus(other: RGBA) = RGBA(
        r = r + other.r,
        g = g + other.g,
        b = b + other.b,
        a = a
    )

    operator fun minus(other: RGBA) = RGBA(
        r = r - other.r,
        g = g - other.g,
        b = b - other.b,
        a = a
    )

    operator fun times(factor: Double) = RGBA(
        r = (r * factor).roundToInt(),
        g = (g * factor).roundToInt(),
        b = (b * factor).roundToInt(),
        a = a
    )

    operator fun div(divisor: Double) = RGBA(
        r = (r / divisor).roundToInt(),
        g = (g / divisor).roundToInt(),
        b = (b / divisor).roundToInt(),
        a = a
    )

    fun quantize(stepSize: Int) = RGBA(
        r = ((r + stepSize / 2) / stepSize) * stepSize,
        g = ((g + stepSize / 2) / stepSize) * stepSize,
        b = ((b + stepSize / 2) / stepSize) * stepSize,
        a = a
    )

    fun coerce() = RGBA(
        r = r.coerceIn(0, 255),
        g = g.coerceIn(0, 255),
        b = b.coerceIn(0, 255),
        a = a.coerceIn(0, 255),
    )

    companion object {
        fun of(value: Long) = of(value.toInt())
        fun of(value: Int) = RGBA(
            a = alpha(value),
            r = red(value),
            g = green(value),
            b = blue(value)
        )
        fun of(r: Int, g: Int, b: Int, a: Int = 255) = RGBA(
            r = r.coerceIn(0, 255),
            g = g.coerceIn(0, 255),
            b = b.coerceIn(0, 255),
            a = a.coerceIn(0, 255)
        )

        val WHITE = RGBA(255, 255, 255)
        val BLACK = RGBA(0, 0, 0)
        val YELLOW = RGBA(255, 255, 0)
        val CYAN = RGBA(0, 255, 255)
        val MAGENTA = RGBA(255, 0, 255)
	}

}
