package dev.oblac.gart.color.space

import dev.oblac.gart.math.f
import org.jetbrains.skia.Color4f
import kotlin.math.cbrt
import kotlin.math.pow

/**
 * OKLab color representation.
 * L is lightness [0..1], a and b are chromaticity (roughly [-0.4..0.4]).
 *
 * Uses the same RGB<->XYZ pipeline as ColorLAB (D65, Bradford adaptation, sRGB),
 * then applies OKLab-specific LMS matrices from CSS Color Level 4 spec.
 */
data class ColorOKLAB(val l: Float, val a: Float, val b: Float, val alpha: Float = 1f) {

    fun mix(other: ColorOKLAB, f: Float = 0.5f): ColorOKLAB {
        return ColorOKLAB(
            l = l + f * (other.l - l),
            a = a + f * (other.a - a),
            b = b + f * (other.b - b),
            alpha = alpha + f * (other.alpha - alpha)
        )
    }

    fun toColor4f(): Color4f {
        val (x, y, z) = oklabToXyz(l, a, b)
        val (r, g, b_) = xyz2rgb(x, y, z)
        return Color4f(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b_.coerceIn(0f, 1f), alpha)
    }

    companion object {
        fun of(color4f: Color4f): ColorOKLAB {
            val (x, y, z) = rgb2xyz(color4f.r, color4f.g, color4f.b)
            val (l, a, b) = xyzToOklab(x, y, z)
            return ColorOKLAB(l = l, a = a, b = b, alpha = color4f.a)
        }

        // --- OKLab-specific matrices (from CSS Color Level 4) ---

        // XYZ to LMS
        private val XYZtoLMS = arrayOf(
            doubleArrayOf(0.819022437996703, 0.3619062600528904, -0.1288737815209879),
            doubleArrayOf(0.0329836539323885, 0.9292868615863434, 0.0361446663506424),
            doubleArrayOf(0.0481771893596242, 0.2642395317527308, 0.6335478284694309)
        )

        // LMS to OKLab
        private val LMStoOKLab = arrayOf(
            doubleArrayOf(0.210454268309314, 0.7936177747023054, -0.0040720430116193),
            doubleArrayOf(1.9779985324311684, -2.4285922420485799, 0.450593709617411),
            doubleArrayOf(0.0259040424655478, 0.7827717124575296, -0.8086757549230774)
        )

        // OKLab to LMS
        private val OKLabtoLMS = arrayOf(
            doubleArrayOf(1.0, 0.3963377773761749, 0.2158037573099136),
            doubleArrayOf(1.0, -0.1055613458156586, -0.0638541728258133),
            doubleArrayOf(1.0, -0.0894841775298119, -1.2914855480194092)
        )

        // LMS to XYZ
        private val LMStoXYZ = arrayOf(
            doubleArrayOf(1.2268798758459243, -0.5578149944602171, 0.2813910456659647),
            doubleArrayOf(-0.0405757452148008, 1.112286803280317, -0.0717110580655164),
            doubleArrayOf(-0.0763729366746601, -0.4214933324022432, 1.5869240198367816)
        )

        private fun mulMatVec(m: Array<DoubleArray>, v: DoubleArray): DoubleArray {
            return DoubleArray(3) { i -> m[i][0] * v[0] + m[i][1] * v[1] + m[i][2] * v[2] }
        }

        private fun xyzToOklab(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
            val lms = mulMatVec(XYZtoLMS, doubleArrayOf(x.toDouble(), y.toDouble(), z.toDouble()))
            val lmsCbrt = DoubleArray(3) { cbrt(lms[it]) }
            val lab = mulMatVec(LMStoOKLab, lmsCbrt)
            return Triple(lab[0].f(), lab[1].f(), lab[2].f())
        }

        private fun oklabToXyz(l: Float, a: Float, b: Float): Triple<Float, Float, Float> {
            val lmsNl = mulMatVec(OKLabtoLMS, doubleArrayOf(l.toDouble(), a.toDouble(), b.toDouble()))
            val lms = DoubleArray(3) { lmsNl[it].pow(3.0) }
            val xyz = mulMatVec(LMStoXYZ, lms)
            return Triple(xyz[0].f(), xyz[1].f(), xyz[2].f())
        }
    }
}
