package dev.oblac.gart.color.space

import org.jetbrains.skia.Color4f

data class ColorHSL(val h: Float, val s: Float, val l: Float, val a: Float) {

    fun toColor4f(): Color4f {
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

        return Color4f(r, g, b, a)
    }

    fun mix(other: ColorHSL, f: Float = 0.5f): ColorHSL {
        val hue = mixHue(h, other.h, f)
        return ColorHSL(
            h = hue,
            s = s + f * (other.s - s),
            l = l + f * (other.l - l),
            a = a + f * (other.a - a)
        )
    }

    fun shade(factor: Float) = copy(l = l * factor)

    fun saturate(factor: Float) = copy(s = s * factor)

    companion object {
        fun of(color: Color4f): ColorHSL {
            val r = color.r
            val g = color.g
            val b = color.b
            val a = color.a

            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min

            val l = (max + min) / 2f

            val s: Float
            var h: Float

            if (delta == 0f) {
                s = 0f
                h = 0f // Float.NaN
            } else {
                s = if (l < 0.5f) {
                    delta / (max + min)
                } else {
                    delta / (2f - max - min)
                }

                h = when (max) {
                    r -> (g - b) / delta
                    g -> 2f + (b - r) / delta
                    b -> 4f + (r - g) / delta
                    else -> 0f
                }

                h *= 60f
                if (h < 0f) h += 360f
            }

            return ColorHSL(
                h = h,
                s = s,
                l = l,
                a = a
            )
        }

    }
}
