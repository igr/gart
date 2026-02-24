package dev.oblac.gart.color.space

import dev.oblac.gart.math.f
import org.jetbrains.skia.Color4f
import kotlin.math.pow

/**
 * CIE L*a*b* color representation.
 * L is lightness [0..100], a and b are chromaticity [-100..100].
 *
 * Conversion uses D65 standard illuminant and sRGB chromatic adaptation.
 */
data class ColorLAB(val l: Float, val a: Float, val b: Float, val alpha: Float = 1f) {

    fun mix(other: ColorLAB, f: Float = 0.5f): ColorLAB {
        return ColorLAB(
            l = l + f * (other.l - l),
            a = a + f * (other.a - a),
            b = b + f * (other.b - b),
            alpha = alpha + f * (other.alpha - alpha)
        )
    }

    fun toColor4f(): Color4f {
        val (x, y, z) = lab2xyz(l, a, b)
        val (r, g, b_) = xyz2rgb(x, y, z)
        return Color4f(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b_.coerceIn(0f, 1f), alpha)
    }

    companion object {
        fun of(color4f: Color4f): ColorLAB {
            val (x, y, z) = rgb2xyz(color4f.r, color4f.g, color4f.b)
            val (l, a, b) = xyz2lab(x, y, z)
            return ColorLAB(l = l, a = a, b = b, alpha = color4f.a)
        }

        private const val kE = 216.0 / 24389.0
        private const val kK = 24389.0 / 27.0
        private const val kKE = 8.0

        internal fun xyz2lab(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
            val xr = x.toDouble() / Xn
            val yr = y.toDouble() / Yn
            val zr = z.toDouble() / Zn

            val fx = if (xr > kE) xr.pow(1.0 / 3.0) else (kK * xr + 16.0) / 116.0
            val fy = if (yr > kE) yr.pow(1.0 / 3.0) else (kK * yr + 16.0) / 116.0
            val fz = if (zr > kE) zr.pow(1.0 / 3.0) else (kK * zr + 16.0) / 116.0

            val l = 116.0 * fy - 16.0
            val a = 500.0 * (fx - fy)
            val b = 200.0 * (fy - fz)

            return Triple(l.f(), a.f(), b.f())
        }

        internal fun lab2xyz(l: Float, a: Float, b: Float): Triple<Float, Float, Float> {
            val ld = l.toDouble()
            val ad = a.toDouble()
            val bd = b.toDouble()

            val fy = (ld + 16.0) / 116.0
            val fx = 0.002 * ad + fy
            val fz = fy - 0.005 * bd

            val fx3 = fx * fx * fx
            val fz3 = fz * fz * fz

            val xr = if (fx3 > kE) fx3 else (116.0 * fx - 16.0) / kK
            val yr = if (ld > kKE) ((ld + 16.0) / 116.0).pow(3.0) else ld / kK
            val zr = if (fz3 > kE) fz3 else (116.0 * fz - 16.0) / kK

            val x = xr * Xn
            val y = yr * Yn
            val z = zr * Zn

            return Triple(x.f(), y.f(), z.f())
        }
    }
}
