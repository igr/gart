package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Error diffusion with output position constraints for homogeneous
 * highlight and shadow dot distribution (Marcu, 1998).
 * Uses triangular or circular roadmap to check for nearby dots before placing.
 *
 * https://doi.org/10.1117/12.298297
 */
fun ditherMarcu(
    bitmap: Pixels,
    kernel: Array<DitherKernelEntry> = DitherKernels.FLOYD_STEINBERG,
    pixelSize: Int = 1,
    colorCount: Int = 256,
    roadmap: MarcuRoadmap = MarcuRoadmap.TRIANGULAR,
    threshold2: Double = 0.1,
    noise: Double = 0.0,
) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)

    val lut1 = when (roadmap) {
        MarcuRoadmap.TRIANGULAR -> genTriangular(15)
        MarcuRoadmap.CIRCULAR -> genCircular(10)
    }

    // Output tracking: stores the quantized output for roadmap checks
    val output = IntArray(width * height)

    var leftToRight = true

    for (y in 0 until height step pixelSize) {
        val xRange = (0 until width step pixelSize).let {
            if (leftToRight) it else it.reversed()
        }

        for (x in xRange) {
            val blockPixel = bitmap.calcAverageBlockColor(x, y, pixelSize)
            val oldColor = RGBA.of(blockPixel)

            val luminance = (0.299 * oldColor.r + 0.587 * oldColor.g + 0.114 * oldColor.b).toInt().coerceIn(0, 255)
            val originalLuminance = luminance / 255.0

            // Standard quantization
            var newR = oldColor.r.roundToNearestQuantization(stepSize)
            var newG = oldColor.g.roundToNearestQuantization(stepSize)
            var newB = oldColor.b.roundToNearestQuantization(stepSize)

            val newLuminance = (0.299 * newR + 0.587 * newG + 0.114 * newB) / 255.0
            val isWhiteDot = newLuminance > 0.5
            val isBlackDot = newLuminance <= 0.5

            val threshold2Low = threshold2 + Random.nextDouble(0.0, noise)
            val threshold2High = threshold2 + Random.nextDouble(0.0, noise)

            // Check if we want to put a white dot in a shadow area
            if (originalLuminance < threshold2Low && isWhiteDot) {
                val roadmapSteps = MARCU_LUT2[luminance.coerceIn(0, MARCU_LUT2.size - 1)]
                if (hasNearbyDot(output, width, height, x, y, pixelSize, lut1, roadmapSteps, leftToRight, isWhite = true)) {
                    // White pixel found nearby -> force black
                    newR = 0; newG = 0; newB = 0
                }
            }
            // Check if we want to put a black dot in a highlight area
            else if (originalLuminance > 1.0 - threshold2High && isBlackDot) {
                val roadmapSteps = MARCU_LUT2[luminance.coerceIn(0, MARCU_LUT2.size - 1)]
                if (hasNearbyDot(output, width, height, x, y, pixelSize, lut1, roadmapSteps, leftToRight, isWhite = false)) {
                    // Black pixel found nearby -> force white
                    newR = 255; newG = 255; newB = 255
                }
            }

            val newColor = RGBA.of(newR, newG, newB, oldColor.a)
            bitmap.setBlock(x, y, pixelSize, newColor.value)
            output[y * width + x] = newColor.value

            // Distribute error
            val errorR = (oldColor.r - newR).coerceIn(-128, 127)
            val errorG = (oldColor.g - newG).coerceIn(-128, 127)
            val errorB = (oldColor.b - newB).coerceIn(-128, 127)

            val dxSign = if (leftToRight) 1 else -1
            for (entry in kernel) {
                val nx = x + entry.dx * dxSign * pixelSize
                val ny = y + entry.dy * pixelSize
                if (nx in 0 until width && ny in 0 until height) {
                    bitmap.addBlockColor(
                        nx, ny, pixelSize,
                        (errorR * entry.weight).toInt(),
                        (errorG * entry.weight).toInt(),
                        (errorB * entry.weight).toInt()
                    )
                }
            }
        }
        leftToRight = !leftToRight
    }
}

enum class MarcuRoadmap { TRIANGULAR, CIRCULAR }

private fun hasNearbyDot(
    output: IntArray, width: Int, height: Int,
    x: Int, y: Int, pixelSize: Int,
    lut1: List<Pair<Int, Int>>, roadmapSteps: Int,
    leftToRight: Boolean, isWhite: Boolean,
): Boolean {
    val steps = roadmapSteps.coerceAtMost(lut1.size)
    for (i in 0 until steps) {
        val (dx, dy) = lut1[i]

        // Do not look at current pixel
        if (dy == 0 && dx == 0) continue
        // Do not look at unprocessed pixels on the same row
        if (dy == 0) {
            if (leftToRight && dx > 0) continue
            if (!leftToRight && dx < 0) continue
        }

        val nx = x + dx * pixelSize
        val ny = y - dy * pixelSize  // note: dy goes upward (already processed rows)
        if (nx in 0 until width && ny in 0 until height) {
            val outColor = RGBA.of(output[ny * width + nx])
            val outLum = (0.299 * outColor.r + 0.587 * outColor.g + 0.114 * outColor.b) / 255.0
            if (isWhite && outLum > 0.5) return true
            if (!isWhite && outLum <= 0.5) return true
        }
    }
    return false
}

private fun genTriangular(h: Int): List<Pair<Int, Int>> {
    val positions = mutableListOf<Pair<Int, Int>>()
    for (i in 0..h) {
        for (px in -i..i) {
            val py = i - abs(px)
            positions.add(Pair(px, py))
        }
    }
    return positions
}

private fun genCircular(radius: Int): List<Pair<Int, Int>> {
    val positions = mutableListOf<Pair<Int, Int>>()
    for (py in -radius..radius) {
        for (px in -radius..radius) {
            val distance = sqrt((px * px + py * py).toDouble())
            if (distance <= radius && py >= 0) {
                positions.add(Pair(px, py))
            }
        }
    }
    // Sort by distance, then angle for tie-breaking
    positions.sortWith(compareBy<Pair<Int, Int>> { it.first * it.first + it.second * it.second }
        .thenBy { -atan2(it.second.toDouble(), it.first.toDouble()) })
    return positions
}

// Roadmap steps lookup table indexed by intensity (0-255)
private val MARCU_LUT2: IntArray = run {
    val base = intArrayOf(
        0, 148, 111, 92, 79, 70, 63, 57, 52, 48, 44, 41, 38, 35, 32, 30,
        28, 26, 24, 22, 20, 19, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8,
        7, 6, 5, 4, 3, 2, 1
    )
    val zeros = IntArray(178)
    val reversed = base.reversedArray()
    base + zeros + reversed
}
