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

/**
 * Fill a disc of `radius` cells centered at `(cx, cy)` with `value` in `dst`.
 * Cells outside `[0, w) x [0, h)` are skipped.
 */
internal fun stampDisc(
    dst: FloatArray,
    cx: Int,
    cy: Int,
    radius: Int,
    value: Float,
    w: Int,
    h: Int,
) {
    if (radius < 0) return
    val r2 = radius * radius
    val xMin = maxOf(0, cx - radius)
    val xMax = minOf(w - 1, cx + radius)
    val yMin = maxOf(0, cy - radius)
    val yMax = minOf(h - 1, cy + radius)
    for (y in yMin..yMax) {
        val dy = y - cy
        val row = y * w
        for (x in xMin..xMax) {
            val dx = x - cx
            if (dx * dx + dy * dy <= r2) dst[row + x] = value
        }
    }
}
