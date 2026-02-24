package dev.oblac.gart.color.space

import org.jetbrains.skia.Color4f
import kotlin.math.*

/**
 * HSI color representation (Hue, Saturation, Intensity).
 * H is in degrees (0-360), S and I are normalized to 0.0-1.0 range.
 */
data class ColorHSI(val h: Float, val s: Float, val i: Float, val a: Float = 1f) {

    fun toColor4f(): Color4f {
        var hue = h
        var sat = s
        var r: Float
        var g: Float
        var b: Float

        if (hue.isNaN()) hue = 0f
        if (sat.isNaN()) sat = 0f
        if (hue > 360f) hue -= 360f
        if (hue < 0f) hue += 360f
        hue /= 360f

        if (hue < 1f / 3f) {
            b = (1f - sat) / 3f
            r = (1f + (sat * cos(TWOPI * hue)) / cos(PITHIRD - TWOPI * hue)) / 3f
            g = 1f - (b + r)
        } else if (hue < 2f / 3f) {
            hue -= 1f / 3f
            r = (1f - sat) / 3f
            g = (1f + (sat * cos(TWOPI * hue)) / cos(PITHIRD - TWOPI * hue)) / 3f
            b = 1f - (r + g)
        } else {
            hue -= 2f / 3f
            g = (1f - sat) / 3f
            b = (1f + (sat * cos(TWOPI * hue)) / cos(PITHIRD - TWOPI * hue)) / 3f
            r = 1f - (g + b)
        }

        r = (i * r * 3f).coerceIn(0f, 1f)
        g = (i * g * 3f).coerceIn(0f, 1f)
        b = (i * b * 3f).coerceIn(0f, 1f)

        return Color4f(r, g, b, a)
    }

    fun mix(other: ColorHSI, f: Float = 0.5f): ColorHSI {
        val hue = mixHue(h, other.h, f)
        return ColorHSI(
            h = hue,
            s = s + f * (other.s - s),
            i = i + f * (other.i - i),
            a = a + f * (other.a - a)
        )
    }

    companion object {
        fun of(color4f: Color4f): ColorHSI {
            val r = color4f.r
            val g = color4f.g
            val b = color4f.b

            val min_ = min(r, min(g, b))
            val i = (r + g + b) / 3f
            val s = if (i > 0f) 1f - min_ / i else 0f

            val h: Float
            if (s == 0f) {
                h = 0f // Float.NaN
            } else {
                var hue = (r - g + (r - b)) / 2f
                hue /= sqrt((r - g) * (r - g) + (r - b) * (g - b))
                hue = acos(hue)
                if (b > g) {
                    hue = TWOPI - hue
                }
                hue /= TWOPI
                h = hue * 360f
            }

            return ColorHSI(h = h, s = s, i = i, a = color4f.a)
        }

        private const val TWOPI = (2 * PI).toFloat()
        private const val PITHIRD = (PI / 3).toFloat()
    }
}
