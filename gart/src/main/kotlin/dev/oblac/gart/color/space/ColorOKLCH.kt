package dev.oblac.gart.color.space

import org.jetbrains.skia.Color4f
import kotlin.math.*

/**
 * OKLCH color representation (cylindrical form of OKLab).
 * L is lightness [0..1], C is chroma [0..~0.4], H is hue in degrees [0..360].
 */
data class ColorOKLCH(val l: Float, val c: Float, val h: Float, val alpha: Float = 1f) {

    fun mix(other: ColorOKLCH, f: Float = 0.5f): ColorOKLCH {
        val hue = mixHue(h, other.h, f)
        return ColorOKLCH(
            l = l + f * (other.l - l),
            c = c + f * (other.c - c),
            h = hue,
            alpha = alpha + f * (other.alpha - alpha)
        )
    }

    fun toColor4f(): Color4f {
        val hRad = if (h.isNaN()) 0f else h * DEG2RAD
        val a = cos(hRad) * c
        val b = sin(hRad) * c
        return ColorOKLAB(l = l, a = a, b = b, alpha = alpha).toColor4f()
    }

    companion object {
        fun of(color4f: Color4f): ColorOKLCH {
            val oklab = ColorOKLAB.of(color4f)
            val c = sqrt(oklab.a * oklab.a + oklab.b * oklab.b)
            var h = (atan2(oklab.b, oklab.a) * RAD2DEG + 360f) % 360f
            if (round(c * 10000f) == 0f) h = 0f // Float.NaN
            return ColorOKLCH(l = oklab.l, c = c, h = h, alpha = color4f.a)
        }

        private const val DEG2RAD = (PI / 180.0).toFloat()
        private const val RAD2DEG = (180.0 / PI).toFloat()
    }
}
