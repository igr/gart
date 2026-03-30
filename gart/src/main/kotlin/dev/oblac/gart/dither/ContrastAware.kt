package dev.oblac.gart.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Contrast-aware halftoning (Mould & Grant, 2009).
 * Distributes error preferentially to pixels with similar intensity,
 * preserving contrast and structure in the output.
 *
 * https://doi.org/10.1111/j.1467-8659.2009.01596.x
 *
 * @param maskSize size of the circular error distribution mask (odd number)
 * @param kParameter controls how strongly distance affects error distribution
 */
fun ditherContrastAware(
    bitmap: Pixels,
    pixelSize: Int = 1,
    colorCount: Int = 256,
    maskSize: Int = 7,
    kParameter: Double = 2.6,
) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)
    val maskRadius = maskSize / 2

    // Track visited pixels and residual error per channel
    val visited = BooleanArray(width * height)
    var residualR = 0.0
    var residualG = 0.0
    var residualB = 0.0

    for (y in 0 until height step pixelSize) {
        for (x in 0 until width step pixelSize) {
            val blockPixel = bitmap.calcAverageBlockColor(x, y, pixelSize)
            val oldColor = RGBA.of(blockPixel)

            val adjR = oldColor.r + residualR
            val adjG = oldColor.g + residualG
            val adjB = oldColor.b + residualB
            residualR = 0.0; residualG = 0.0; residualB = 0.0

            val newR = adjR.toInt().roundToNearestQuantization(stepSize)
            val newG = adjG.toInt().roundToNearestQuantization(stepSize)
            val newB = adjB.toInt().roundToNearestQuantization(stepSize)

            val errorR = adjR - newR
            val errorG = adjG - newG
            val errorB = adjB - newB

            bitmap.setBlock(x, y, pixelSize, RGBA.of(newR, newG, newB, oldColor.a).value)
            visited[y * width + x] = true

            // Compute luminance of original pixel for weight calculation
            val origLum = (0.299 * oldColor.r + 0.587 * oldColor.g + 0.114 * oldColor.b) / 255.0
            val errorLum = (0.299 * errorR + 0.587 * errorG + 0.114 * errorB) / 255.0

            // Calculate weights for circular mask
            var totalWeight = 0.0
            val weights = mutableMapOf<Long, Double>()

            for (my in -maskRadius..maskRadius) {
                for (mx in -maskRadius..maskRadius) {
                    if (mx == 0 && my == 0) continue
                    val ds = mx * mx + my * my
                    if (ds > maskRadius * maskRadius) continue

                    val ny = y + my * pixelSize
                    val nx = x + mx * pixelSize
                    if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue
                    if (visited[ny * width + nx]) continue

                    val neighborPixel = bitmap.calcAverageBlockColor(nx, ny, pixelSize)
                    val neighborColor = RGBA.of(neighborPixel)
                    val maskLum = (0.299 * neighborColor.r + 0.587 * neighborColor.g + 0.114 * neighborColor.b) / 255.0
                    val dist = sqrt(ds.toDouble())

                    val w = if (errorLum > 0.0) {
                        maskLum / dist.pow(kParameter)
                    } else {
                        (1.0 - maskLum) / dist.pow(kParameter)
                    }

                    val key = ny.toLong() * width + nx
                    weights[key] = w
                    totalWeight += w
                }
            }

            if (totalWeight > 0.0) {
                for ((key, w) in weights) {
                    val nx = (key % width).toInt()
                    val ny = (key / width).toInt()
                    val normalized = w / totalWeight

                    val addR = errorR * normalized
                    val addG = errorG * normalized
                    val addB = errorB * normalized

                    // Check for overflow and accumulate residual
                    val curPixel = bitmap.calcAverageBlockColor(nx, ny, pixelSize)
                    val curColor = RGBA.of(curPixel)
                    val newValR = curColor.r + addR
                    val newValG = curColor.g + addG
                    val newValB = curColor.b + addB

                    if (newValR > 255) residualR += newValR - 255
                    else if (newValR < 0) residualR += newValR
                    if (newValG > 255) residualG += newValG - 255
                    else if (newValG < 0) residualG += newValG
                    if (newValB > 255) residualB += newValB - 255
                    else if (newValB < 0) residualB += newValB

                    bitmap.addBlockColor(nx, ny, pixelSize, addR.toInt(), addG.toInt(), addB.toInt())
                }
            }
        }
    }
}
