package dev.oblac.gart.color.space

import dev.oblac.gart.math.d
import dev.oblac.gart.math.f
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/**
 * Shared RGB<->XYZ conversion pipeline used by ColorLAB and ColorOKLAB.
 * D65 standard illuminant, Bradford chromatic adaptation, sRGB gamma.
 */

// D65 standard referent
internal const val Xn = 0.95047f
internal const val Yn = 1f
internal const val Zn = 1.08883f

// sRGB reference white
private const val RefWhiteX = 0.95047
private const val RefWhiteY = 1.0
private const val RefWhiteZ = 1.08883

// Pre-computed As, Bs, Cs (used in rgb2xyz)
private const val As = 0.9414285350000001
private const val Bs = 1.040417467
private const val Cs = 1.089532651

// Bradford chromatic adaptation matrix
private val MtxAdaptMa = doubleArrayOf(
    0.8951, -0.7502, 0.0389,
    0.2664, 1.7135, -0.0685,
    -0.1614, 0.0367, 1.0296
)

// Inverse Bradford matrix
private val MtxAdaptMaI = doubleArrayOf(
    0.9869929054667123, 0.43230526972339456, -0.008528664575177328,
    -0.14705425642099013, 0.5183602715367776, 0.04004282165408487,
    0.15996265166373125, 0.0492912282128556, 0.9684866957875502
)

// sRGB to XYZ matrix
private val MtxRGB2XYZ = doubleArrayOf(
    0.4124564390896922, 0.21267285140562253, 0.0193338955823293,
    0.357576077643909, 0.715152155287818, 0.11919202588130297,
    0.18043748326639894, 0.07217499330655958, 0.9503040785363679
)

// XYZ to sRGB matrix
private val MtxXYZ2RGB = doubleArrayOf(
    3.2404541621141045, -0.9692660305051868, 0.055643430959114726,
    -1.5371385127977166, 1.8760108454466942, -0.2040259135167538,
    -0.498531409556016, 0.041556017530349834, 1.0572251882231791
)

// Matrix access: chroma.js uses m{col}{row} -> array[col * 3 + row]
private fun m(matrix: DoubleArray, col: Int, row: Int) = matrix[col * 3 + row]

private const val D_Xn = 0.95047
private const val D_Yn = 1.0
private const val D_Zn = 1.08883

internal fun gammaAdjustSRGB(companded: Double): Double {
    val sign = sign(companded)
    val c = abs(companded)
    val linear = if (c <= 0.04045) {
        c / 12.92
    } else {
        ((c + 0.055) / 1.055).pow(2.4)
    }
    return linear * sign
}

internal fun compand(linear: Double): Double {
    val sign = sign(linear)
    val l = abs(linear)
    return (if (l <= 0.0031308) {
        l * 12.92
    } else {
        1.055 * l.pow(1.0 / 2.4) - 0.055
    }) * sign
}

internal fun rgb2xyz(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val rl = gammaAdjustSRGB(r.d())
    val gl = gammaAdjustSRGB(g.d())
    val bl = gammaAdjustSRGB(b.d())

    var x = rl * m(MtxRGB2XYZ, 0, 0) + gl * m(MtxRGB2XYZ, 1, 0) + bl * m(MtxRGB2XYZ, 2, 0)
    var y = rl * m(MtxRGB2XYZ, 0, 1) + gl * m(MtxRGB2XYZ, 1, 1) + bl * m(MtxRGB2XYZ, 2, 1)
    var z = rl * m(MtxRGB2XYZ, 0, 2) + gl * m(MtxRGB2XYZ, 1, 2) + bl * m(MtxRGB2XYZ, 2, 2)

    val ad = D_Xn * m(MtxAdaptMa, 0, 0) + D_Yn * m(MtxAdaptMa, 1, 0) + D_Zn * m(MtxAdaptMa, 2, 0)
    val bd = D_Xn * m(MtxAdaptMa, 0, 1) + D_Yn * m(MtxAdaptMa, 1, 1) + D_Zn * m(MtxAdaptMa, 2, 1)
    val cd = D_Xn * m(MtxAdaptMa, 0, 2) + D_Yn * m(MtxAdaptMa, 1, 2) + D_Zn * m(MtxAdaptMa, 2, 2)

    var xx = x * m(MtxAdaptMa, 0, 0) + y * m(MtxAdaptMa, 1, 0) + z * m(MtxAdaptMa, 2, 0)
    var yy = x * m(MtxAdaptMa, 0, 1) + y * m(MtxAdaptMa, 1, 1) + z * m(MtxAdaptMa, 2, 1)
    var zz = x * m(MtxAdaptMa, 0, 2) + y * m(MtxAdaptMa, 1, 2) + z * m(MtxAdaptMa, 2, 2)

    xx *= ad / As
    yy *= bd / Bs
    zz *= cd / Cs

    x = xx * m(MtxAdaptMaI, 0, 0) + yy * m(MtxAdaptMaI, 1, 0) + zz * m(MtxAdaptMaI, 2, 0)
    y = xx * m(MtxAdaptMaI, 0, 1) + yy * m(MtxAdaptMaI, 1, 1) + zz * m(MtxAdaptMaI, 2, 1)
    z = xx * m(MtxAdaptMaI, 0, 2) + yy * m(MtxAdaptMaI, 1, 2) + zz * m(MtxAdaptMaI, 2, 2)

    return Triple(x.f(), y.f(), z.f())
}

internal fun xyz2rgb(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
    val xd = x.d()
    val yd = y.d()
    val zd = z.d()

    val as_ = D_Xn * m(MtxAdaptMa, 0, 0) + D_Yn * m(MtxAdaptMa, 1, 0) + D_Zn * m(MtxAdaptMa, 2, 0)
    val bs_ = D_Xn * m(MtxAdaptMa, 0, 1) + D_Yn * m(MtxAdaptMa, 1, 1) + D_Zn * m(MtxAdaptMa, 2, 1)
    val cs_ = D_Xn * m(MtxAdaptMa, 0, 2) + D_Yn * m(MtxAdaptMa, 1, 2) + D_Zn * m(MtxAdaptMa, 2, 2)

    val ad = RefWhiteX * m(MtxAdaptMa, 0, 0) + RefWhiteY * m(MtxAdaptMa, 1, 0) + RefWhiteZ * m(MtxAdaptMa, 2, 0)
    val bd = RefWhiteX * m(MtxAdaptMa, 0, 1) + RefWhiteY * m(MtxAdaptMa, 1, 1) + RefWhiteZ * m(MtxAdaptMa, 2, 1)
    val cd = RefWhiteX * m(MtxAdaptMa, 0, 2) + RefWhiteY * m(MtxAdaptMa, 1, 2) + RefWhiteZ * m(MtxAdaptMa, 2, 2)

    val x1 = (xd * m(MtxAdaptMa, 0, 0) + yd * m(MtxAdaptMa, 1, 0) + zd * m(MtxAdaptMa, 2, 0)) * (ad / as_)
    val y1 = (xd * m(MtxAdaptMa, 0, 1) + yd * m(MtxAdaptMa, 1, 1) + zd * m(MtxAdaptMa, 2, 1)) * (bd / bs_)
    val z1 = (xd * m(MtxAdaptMa, 0, 2) + yd * m(MtxAdaptMa, 1, 2) + zd * m(MtxAdaptMa, 2, 2)) * (cd / cs_)

    val x2 = x1 * m(MtxAdaptMaI, 0, 0) + y1 * m(MtxAdaptMaI, 1, 0) + z1 * m(MtxAdaptMaI, 2, 0)
    val y2 = x1 * m(MtxAdaptMaI, 0, 1) + y1 * m(MtxAdaptMaI, 1, 1) + z1 * m(MtxAdaptMaI, 2, 1)
    val z2 = x1 * m(MtxAdaptMaI, 0, 2) + y1 * m(MtxAdaptMaI, 1, 2) + z1 * m(MtxAdaptMaI, 2, 2)

    val r = compand(x2 * m(MtxXYZ2RGB, 0, 0) + y2 * m(MtxXYZ2RGB, 1, 0) + z2 * m(MtxXYZ2RGB, 2, 0))
    val g = compand(x2 * m(MtxXYZ2RGB, 0, 1) + y2 * m(MtxXYZ2RGB, 1, 1) + z2 * m(MtxXYZ2RGB, 2, 1))
    val b = compand(x2 * m(MtxXYZ2RGB, 0, 2) + y2 * m(MtxXYZ2RGB, 1, 2) + z2 * m(MtxXYZ2RGB, 2, 2))

    return Triple(r.f(), g.f(), b.f())
}
