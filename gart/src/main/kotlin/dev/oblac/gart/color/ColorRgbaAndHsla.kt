package dev.oblac.gart.color

import kotlin.math.roundToInt

data class ColorRGBA(val r: Float, val g: Float, val b: Float, val a: Float = 1f) {

    fun toHSLA(): ColorHSLA {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val lightness = (max + min) / 2f

        val saturation: Float
        val hue: Float

        if (delta == 0f) {
            // Achromatic (gray)
            saturation = 0f
            hue = 0f
        } else {
            // Calculate Saturation
            saturation = if (lightness > 0.5f) {
                delta / (2f - max - min)
            } else {
                delta / (max + min)
            }

            // Calculate Hue
            hue = when (max) {
                r -> ((g - b) / delta + if (g < b) 6f else 0f) / 6f
                g -> ((b - r) / delta + 2f) / 6f
                b -> ((r - g) / delta + 4f) / 6f
                else -> 0f
            }
        }

        return ColorHSLA(
            h = hue * 360f, // Convert to degrees (0-360)
            s = saturation,
            l = lightness,
            a = a
        )
    }

    fun toRGBA(): RGBA {
        return RGBA((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), (a * 255).toInt())
    }

    /**
     * Calculates luminance value according to
     * https://www.w3.org/TR/2008/REC-WCAG20-20081211/#relativeluminancedef
     */
    val luminance: Float
        get() = 0.2126f * r + 0.7152f * g + 0.0722f * b

    /**
     * Calculates the contrast value between this color and the given color
     * contrast value is according to
     * http://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef
     */
    fun contrastRatio(other: ColorRGBA): Double {
        val l1 = luminance
        val l2 = other.luminance
        return if (l1 > l2) (l1 + 0.05) / (l2 + 0.05) else (l2 + 0.05) / (l1 + 0.05)
    }

    companion object {
        fun of(rbga: RGBA): ColorRGBA {
            return ColorRGBA(
                rbga.r / 255f,
                rbga.g / 255f,
                rbga.b / 255f,
                rbga.a / 255f
            )
        }

        fun of(color: Int): ColorRGBA {
            return ColorRGBA(
                red(color) / 255f,
                green(color) / 255f,
                blue(color) / 255f,
                alpha(color) / 255f
            )
        }
    }
}

data class ColorHSLA(val h: Float, val s: Float, val l: Float, val a: Float) {

    fun toColorRGBA(): ColorRGBA {
        val h = h / 360f // Convert degrees to 0-1 range

        val r: Float
        val g: Float
        val b: Float

        if (s == 0f) {
            // Achromatic (gray) - no saturation
            r = l
            g = l
            b = l
        } else {
            // Helper function to convert hue to RGB
            fun hueToRgb(p: Float, q: Float, t: Float): Float {
                var hue = t
                if (hue < 0f) hue += 1f
                if (hue > 1f) hue -= 1f

                return when {
                    hue < 1f / 6f -> p + (q - p) * 6f * hue
                    hue < 1f / 2f -> q
                    hue < 2f / 3f -> p + (q - p) * (2f / 3f - hue) * 6f
                    else -> p
                }
            }

            val q = if (l < 0.5f) {
                l * (1f + s)
            } else {
                l + s - l * s
            }
            val p = 2f * l - q

            r = hueToRgb(p, q, h + 1f / 3f)
            g = hueToRgb(p, q, h)
            b = hueToRgb(p, q, h - 1f / 3f)
        }

        return ColorRGBA(r, g, b, a)
    }

    fun shade(factor: Float) = copy(l = l * factor)
    fun saturate(factor: Float) = copy(s = s * factor)
}

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
