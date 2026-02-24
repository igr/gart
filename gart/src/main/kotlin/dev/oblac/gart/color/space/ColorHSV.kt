package dev.oblac.gart.color.space

import org.jetbrains.skia.Color4f
import kotlin.math.floor

/**
 * HSV color representation (Hue, Saturation, Value).
 * H is in degrees (0-360), S and V are normalized to 0.0-1.0 range.
 */
data class ColorHSV(val h: Float, val s: Float, val v: Float, val a: Float = 1f) {

    fun toColor4f(): Color4f {
        var r: Float
        var g: Float
        var b: Float

        if (s == 0f) {
            r = v
            g = v
            b = v
        } else {
            var hue = h
            if (hue == 360f) hue = 0f
            if (hue > 360f) hue -= 360f
            if (hue < 0f) hue += 360f
            hue /= 60f

            val i = floor(hue).toInt()
            val f = hue - i
            val p = v * (1f - s)
            val q = v * (1f - s * f)
            val t = v * (1f - s * (1f - f))

            when (i) {
                0 -> { r = v; g = t; b = p }
                1 -> { r = q; g = v; b = p }
                2 -> { r = p; g = v; b = t }
                3 -> { r = p; g = q; b = v }
                4 -> { r = t; g = p; b = v }
                5 -> { r = v; g = p; b = q }
                else -> { r = 0f; g = 0f; b = 0f }
            }
        }

        return Color4f(r, g, b, a)
    }

    fun mix(other: ColorHSV, f: Float = 0.5f): ColorHSV {
        val hue = mixHue(h, other.h, f)
        return ColorHSV(
            h = hue,
            s = s + f * (other.s - s),
            v = v + f * (other.v - v),
            a = a + f * (other.a - a)
        )
    }

    companion object {
        fun of(color4f: Color4f): ColorHSV {
            val r = color4f.r
            val g = color4f.g
            val b = color4f.b

            val min = minOf(r, g, b)
            val max = maxOf(r, g, b)
            val delta = max - min

            val v = max

            val h: Float
            val s: Float

            if (max == 0f) {
                h = 0f // Float.NaN
                s = 0f
            } else {
                s = delta / max
                h = when (max) {
                    r -> (g - b) / delta
                    g -> 2f + (b - r) / delta
                    b -> 4f + (r - g) / delta
                    else -> 0f
                }.let {
                    var hue = it * 60f
                    if (hue < 0f) hue += 360f
                    hue
                }
            }

            return ColorHSV(h = h, s = s, v = v, a = color4f.a)
        }
    }
}
