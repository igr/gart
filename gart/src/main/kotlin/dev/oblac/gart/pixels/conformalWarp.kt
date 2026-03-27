package dev.oblac.gart.pixels

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.SampleMode
import dev.oblac.gart.math.Complex
import kotlin.math.*

/**
 * Maps a pixel coordinate to the complex plane.
 * Y is flipped (screen ↓ = math ↑). Default unitPixels = width/4,
 * so the visible range is ≈ [-2, 2] × [-2, 2].
 */
private fun pixelToComplex(
    px: Int, py: Int,
    width: Int, height: Int,
    centerRe: Double = 0.0,
    centerIm: Double = 0.0,
    unitPixels: Double = width / 4.0
) = Complex(
    (px - width / 2.0) / unitPixels + centerRe,
    -(py - height / 2.0) / unitPixels + centerIm
)


/**
 * Apply a conformal (log-polar) warp to [src] and return a new [Gartmap].
 *
 * Maps the source image into a ring in the output:
 *   - Source X axis → angle around the ring (seamless, no branch-cut seam)
 *   - Source Y axis → log-radius (conformal: preserves local angles)
 *
 * Algorithm (per output pixel):
 *   1. Map output pixel → complex coordinate u
 *   2. Compute log-polar: θ = arg(u), ln(r) = ln|u|
 *   3. Map θ → source X,  ln(r) → source Y
 *   4. Sample [src] with bilinear interpolation
 *
 * @param src           input Gartmap (source pixels to warp)
 * @param outDimension  output dimensions (defaults to src dimensions)
 * @param rInner        inner radius of the visible ring (maps to source top)
 * @param rOuter        outer radius of the visible ring (maps to source bottom)
 * @param unitPixels    pixels per complex-unit (controls zoom)
 * @param sampleMode    edge behaviour for out-of-bounds source coordinates
 * @param background    fill color (ARGB) used in BACKGROUND mode
 * @param bilinear      true = bilinear interpolation, false = nearest-neighbour
 */
fun conformalWarp(
    src: Gartmap,
    outDimension: Dimension = src.d,
    rInner: Double = 0.5,
    rOuter: Double = 2.0,
    unitPixels: Double = outDimension.w / 4.0,
    sampleMode: SampleMode = SampleMode.TILE,
    background: Int = 0xFF000000.toInt(),
    bilinear: Boolean = true
): Gartmap {
    val lnRInner = ln(rInner)
    val lnROuter = ln(rOuter)
    val dst = Gartmap(Gartvas(outDimension))

    for (py in 0 until outDimension.h) {
        for (px in 0 until outDimension.w) {

            // ── Step 1: output pixel → complex plane ──────────────────────
            val u = pixelToComplex(px, py, outDimension.w, outDimension.h, unitPixels = unitPixels)
            val r = sqrt(u.real * u.real + u.imag * u.imag)

            if (r == 0.0) {
                dst[px, py] = background
                continue
            }

            // ── Step 2: log-polar decomposition ───────────────────────────
            val theta = atan2(u.imag, u.real)                // (-π, π]

            // Shift to [0, 2π) — eliminates the branch-cut seam
            val thetaNorm = ((theta % (2 * PI)) + 2 * PI) % (2 * PI)

            // ── Step 3: map to source pixel coordinates ───────────────────
            // angle → source X  (full circle = full source width)
            val srcX = thetaNorm / (2 * PI) * src.d.w

            // log-radius → source Y  (conformal radial mapping)
            val lnR = ln(r)
            val srcY = (lnR - lnRInner) / (lnROuter - lnRInner) * src.d.h

            // ── Step 4: sample ────────────────────────────────────────────
            val color = if (!srcX.isFinite() || !srcY.isFinite()) {
                background
            } else if (bilinear) {
                src.sampleBilinear(srcX, srcY, sampleMode, background)
            } else {
                src.sampleNearest(srcX.roundToInt(), srcY.roundToInt(), sampleMode, background)
            }

            dst[px, py] = color
        }
    }

    return dst
}
