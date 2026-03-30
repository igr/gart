package dev.oblac.gart.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.adaptiveMedianFilter
import dev.oblac.gart.pixels.gaussianBlur
import dev.oblac.gart.pixels.uniformFilter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * An improved error diffusion algorithm based on visual difference
 * (Chang et al., 2014). Iteratively compensates the input image based on
 * the visual difference between the original and the halftoned result.
 *
 * https://doi.org/10.1109/ICIP.2014.7025530
 *
 * @param numIterations 3 and 5 are suggested in the paper
 * @param uc compensation strength parameter
 * @param q detail threshold parameter
 * @param ramfEnabled enable RAMF post-filtering (disable at smaller image sizes)
 * @param ramfMin minimum RAMF window size
 * @param ramfMax maximum RAMF window size
 */
fun ditherVisualDifference(
    bitmap: Pixels,
    pixelSize: Int = 1,
    colorCount: Int = 256,
    numIterations: Int = 3,
    uc: Double = 15.0,
    q: Double = 5.0,
    ramfEnabled: Boolean = true,
    ramfMin: Int = 3,
    ramfMax: Int = 11,
) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)

    // Build luminance map (0-255 scale)
    val originalLum = DoubleArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val p = RGBA.of(bitmap[x, y])
            originalLum[y * width + x] = 0.299 * p.r + 0.587 * p.g + 0.114 * p.b
        }
    }
    val originalNorm = DoubleArray(originalLum.size) { originalLum[it] / 255.0 }

    // Detail map: highpass filter
    val originalSmoothed = uniformFilter(originalNorm, width, height, 3)
    val fd = DoubleArray(width * height) { (originalNorm[it] - originalSmoothed[it]) * 255.0 }

    // Compensated input (modified iteratively)
    val compensated = originalLum.copyOf()

    for (iter in 0 until numIterations) {
        // Halftone the compensated input using Floyd-Steinberg
        val compensatedNorm = DoubleArray(compensated.size) { compensated[it] / 255.0 }
        val halftone = applyFloydSteinberg(compensatedNorm, width, height)

        // Compute difference (Gaussian-smoothed)
        val diffRaw = DoubleArray(width * height) { abs(originalNorm[it] - halftone[it]) }
        val difference = gaussianBlur(diffRaw, width, height, 1.0)

        val meanDiff = difference.average()
        if (meanDiff <= 0.0) continue

        val u = uc / meanDiff
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val c = when {
                    fd[idx] >= q -> u
                    fd[idx] <= -q -> -u
                    else -> (u / (q * q * q)) * (fd[idx] * fd[idx] * fd[idx])
                }
                compensated[idx] = max(0.0, min(compensated[idx] + c * difference[idx], 255.0))
            }
        }
    }

    // Optional RAMF post-filtering
    val finalCompensated = if (ramfEnabled) {
        adaptiveMedianFilter(compensated, width, height, ramfMin, ramfMax)
    } else {
        compensated
    }

    // Apply final modulated error diffusion to the compensated image
    // Write compensated luminance back to bitmap, then dither
    for (y in 0 until height) {
        for (x in 0 until width) {
            val ratio = finalCompensated[y * width + x] / (originalLum[y * width + x].coerceAtLeast(1.0))
            val p = RGBA.of(bitmap[x, y])
            val newR = (p.r * ratio).toInt().coerceIn(0, 255)
            val newG = (p.g * ratio).toInt().coerceIn(0, 255)
            val newB = (p.b * ratio).toInt().coerceIn(0, 255)
            bitmap[x, y] = RGBA.of(newR, newG, newB, p.a).value
        }
    }

    // Final dithering pass with modulated error diffusion
    ditherErrorDiffusionModulated(
        bitmap,
        DitherKernels.FLOYD_STEINBERG,
        pixelSize,
        colorCount
    )
}

/**
 * Internal Floyd-Steinberg on normalized grayscale (0-1).
 * Returns binary halftone (0.0 or 1.0).
 */
private fun applyFloydSteinberg(input: DoubleArray, w: Int, h: Int): DoubleArray {
    val data = input.copyOf()
    val output = DoubleArray(w * h)

    for (y in 0 until h) {
        for (x in 0 until w) {
            val old = data[y * w + x]
            val new_ = if (old < 0.5) 0.0 else 1.0
            val err = old - new_
            output[y * w + x] = new_

            if (x + 1 < w) data[y * w + x + 1] += err * 7.0 / 16.0
            if (y + 1 < h) {
                if (x - 1 >= 0) data[(y + 1) * w + x - 1] += err * 3.0 / 16.0
                data[(y + 1) * w + x] += err * 5.0 / 16.0
                if (x + 1 < w) data[(y + 1) * w + x + 1] += err * 1.0 / 16.0
            }
        }
    }
    return output
}
