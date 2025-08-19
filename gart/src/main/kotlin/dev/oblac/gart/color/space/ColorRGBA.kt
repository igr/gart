package dev.oblac.gart.color.space

import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red

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
