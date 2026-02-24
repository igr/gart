package dev.oblac.gart.color.space

import dev.oblac.gart.color.*
import org.jetbrains.skia.Color4f
import kotlin.math.roundToInt

/**
 * Integer RGBA color representation with components in the range [0, 255].
 */
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

    fun mix(other: RGBA, f: Float = 0.5f) = RGBA(
        r = (r + f * (other.r - r)).roundToInt(),
        g = (g + f * (other.g - g)).roundToInt(),
        b = (b + f * (other.b - b)).roundToInt(),
        a = (a + f * (other.a - a)).roundToInt()
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

    fun toColor4f() = Color4f(
        r = r / 255f,
        g = g / 255f,
        b = b / 255f,
        a = a / 255f
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

        fun of(c4f: Color4f): RGBA {
            val r = c4f.r
            val g = c4f.g
            val b = c4f.b
            val a = c4f.a
            return RGBA((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), (a * 255).toInt())
        }

        val WHITE = RGBA(255, 255, 255)
        val BLACK = RGBA(0, 0, 0)
        val YELLOW = RGBA(255, 255, 0)
        val CYAN = RGBA(0, 255, 255)
        val MAGENTA = RGBA(255, 0, 255)
    }

}
