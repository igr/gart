package dev.oblac.gart.color.space

import org.jetbrains.skia.Color4f
import kotlin.math.*

/**
 * CIE LCH color representation (cylindrical form of L*a*b*).
 * L is lightness [0..100], C is chroma [0..~150], H is hue in degrees [0..360].
 */
data class ColorLCH(val l: Float, val c: Float, val h: Float, val alpha: Float = 1f) {

    fun mix(other: ColorLCH, f: Float = 0.5f): ColorLCH {
        val hue = mixHue(h, other.h, f)
        return ColorLCH(
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
        return ColorLAB(l = l, a = a, b = b, alpha = alpha).toColor4f()
    }

    companion object {
        fun of(color4f: Color4f): ColorLCH {
            val lab = ColorLAB.of(color4f)
            val c = sqrt(lab.a * lab.a + lab.b * lab.b)
            var h = (atan2(lab.b, lab.a) * RAD2DEG + 360f) % 360f
            if (round(c * 10000f) == 0f) h = 0f // Float.NaN
            return ColorLCH(l = lab.l, c = c, h = h, alpha = color4f.a)
        }

        private const val DEG2RAD = (PI / 180.0).toFloat()
        private const val RAD2DEG = (180.0 / PI).toFloat()
    }
}
