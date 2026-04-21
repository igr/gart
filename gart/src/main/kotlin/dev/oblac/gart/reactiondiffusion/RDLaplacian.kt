package dev.oblac.gart.reactiondiffusion

/**
 * Weighted isotropic 9-point Laplacian matching the kernel used in the
 * original GLSL shaders.
 *
 *   kernel weights (sum = 0):
 *     center              = -6.82842712
 *     cardinal (N,S,E,W)  =  1.0
 *     diagonal (NW,NE,SW,SE) = 0.707106781
 *
 * Out-of-bounds samples are clamped to the nearest edge (equivalent to
 * GLSL `texture2DRect` with clamp-to-border at integer texel coordinates).
 */
internal const val LAP_CENTER = -6.82842712
internal const val LAP_CARDINAL = 1.0
internal const val LAP_DIAGONAL = 0.707106781

internal fun laplacian(
    src: FloatArray,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
): Float {
    val xm = if (x > 0) x - 1 else 0
    val xp = if (x < w - 1) x + 1 else w - 1
    val ym = if (y > 0) y - 1 else 0
    val yp = if (y < h - 1) y + 1 else h - 1

    val rowM = ym * w
    val row0 = y * w
    val rowP = yp * w

    val cN = src[rowM + x]
    val cS = src[rowP + x]
    val cE = src[row0 + xp]
    val cW = src[row0 + xm]
    val c0 = src[row0 + x]
    val cNW = src[rowM + xm]
    val cNE = src[rowM + xp]
    val cSW = src[rowP + xm]
    val cSE = src[rowP + xp]

    return (LAP_CENTER * c0 +
        LAP_CARDINAL * (cN + cS + cE + cW) +
        LAP_DIAGONAL * (cNW + cNE + cSW + cSE)).toFloat()
}
