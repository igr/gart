package dev.oblac.gart.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.PadMode
import dev.oblac.gart.pixels.gaussianBlur
import dev.oblac.gart.pixels.roundToNearestQuantization
import kotlin.random.Random

/**
 * Blue noise thresholding.
 * Generates a blue noise pattern using the void-and-cluster method
 * and uses it as a threshold map for ordered dithering.
 * Produces visually pleasant, noise-free halftones.
 *
 * Based on: https://github.com/laszlokorte/blue-noise
 *
 * @param noiseWidth width of the blue noise tile (tiled over the image)
 * @param noiseHeight height of the blue noise tile
 * @param sigma Gaussian sigma for the void-and-cluster algorithm
 */
fun ditherBlueNoise(
    bitmap: Pixels,
    pixelSize: Int = 1,
    colorCount: Int = 256,
    noiseWidth: Int = 64,
    noiseHeight: Int = 64,
    sigma: Double = 1.5,
) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)

    // Generate blue noise pattern
    val blueNoise = generateBlueNoise(noiseWidth, noiseHeight, sigma = sigma)

    // Normalize to [0, 1]
    var bnMin = Double.MAX_VALUE
    var bnMax = Double.MIN_VALUE
    for (v in blueNoise) {
        if (v < bnMin) bnMin = v
        if (v > bnMax) bnMax = v
    }
    val bnRange = bnMax - bnMin
    val bnNormalized = DoubleArray(blueNoise.size) { (blueNoise[it] - bnMin) / bnRange }

    for (y in 0 until height step pixelSize) {
        for (x in 0 until width step pixelSize) {
            val blockPixel = bitmap.calcAverageBlockColor(x, y, pixelSize)
            val oldColor = RGBA.of(blockPixel)

            val bnX = (x / pixelSize) % noiseWidth
            val bnY = (y / pixelSize) % noiseHeight
            val threshold = (bnNormalized[bnY * noiseWidth + bnX] * stepSize - stepSize / 2.0).toInt()

            val newR = (oldColor.r + threshold).roundToNearestQuantization(stepSize)
            val newG = (oldColor.g + threshold).roundToNearestQuantization(stepSize)
            val newB = (oldColor.b + threshold).roundToNearestQuantization(stepSize)

            bitmap.setBlock(x, y, pixelSize, RGBA.of(newR, newG, newB, oldColor.a).value)
        }
    }
}

/**
 * Generates a blue noise pattern using the void-and-cluster method.
 * Returns a DoubleArray of rank values (lower = placed earlier = denser region).
 */
private fun generateBlueNoise(
    width: Int,
    height: Int,
    initialRatio: Double = 0.1,
    sigma: Double = 1.5,
): DoubleArray {
    val size = width * height
    val ranks = DoubleArray(size)
    val placed = BooleanArray(size)

    // Initial white noise placement
    val rng = Random(42)
    var countPlaced = 0
    for (i in 0 until size) {
        if (rng.nextDouble() <= initialRatio) {
            placed[i] = true
            countPlaced++
        }
    }
    val countRemaining = size - countPlaced

    // Phase 1: Swap densest placed pixel with voidest unplaced pixel until stable
    var prevSwap: Pair<Int, Int>? = null
    while (true) {
        val blurred = gaussianBlurBoolean(placed, width, height, sigma)

        // Find densest placed pixel
        var densest = -1
        var densestVal = Double.NEGATIVE_INFINITY
        for (i in 0 until size) {
            if (placed[i] && blurred[i] > densestVal) {
                densestVal = blurred[i]
                densest = i
            }
        }

        // Find voidest unplaced pixel (minimum of blurred + placed as double)
        var voidest = -1
        var voidestVal = Double.POSITIVE_INFINITY
        for (i in 0 until size) {
            val v = blurred[i] + if (placed[i]) 1.0 else 0.0
            if (v < voidestVal) {
                voidestVal = v
                voidest = i
            }
        }

        val swap = Pair(voidest, densest)
        if (swap == prevSwap) break
        prevSwap = Pair(densest, voidest)

        placed[densest] = false
        placed[voidest] = true
    }

    // Phase 2: Rank placed pixels by density (remove densest first)
    val ranked = placed.copyOf()
    for (rank in countPlaced downTo 1) {
        val blurred = gaussianBlurBoolean(placed, width, height, sigma)

        var densest = -1
        var densestVal = Double.NEGATIVE_INFINITY
        for (i in 0 until size) {
            if (ranked[i] && blurred[i] > densestVal) {
                densestVal = blurred[i]
                densest = i
            }
        }

        ranked[densest] = false
        ranks[densest] = rank.toDouble()
    }

    // Phase 3: Fill remaining pixels from sparsest areas
    for (rank in 0 until countRemaining) {
        val blurred = gaussianBlurBoolean(placed, width, height, sigma)

        var voidest = -1
        var voidestVal = Double.POSITIVE_INFINITY
        for (i in 0 until size) {
            val v = blurred[i] + if (placed[i]) 1.0 else 0.0
            if (v < voidestVal) {
                voidestVal = v
                voidest = i
            }
        }

        placed[voidest] = true
        ranks[voidest] = (countPlaced + rank).toDouble()
    }

    return ranks
}

private fun gaussianBlurBoolean(data: BooleanArray, w: Int, h: Int, sigma: Double): DoubleArray {
    val doubleData = DoubleArray(data.size) { if (data[it]) 1.0 else 0.0 }
    return gaussianBlur(doubleData, w, h, sigma, PadMode.WRAP)
}
