package dev.oblac.gart.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.math.binaryEntropy
import dev.oblac.gart.pixels.gaussianBlur

import dev.oblac.gart.pixels.roundToNearestQuantization

/**
 * Structure-aware error-diffusion approach using entropy-constrained
 * threshold modulation (Li & Allebach, 2013).
 * Uses entropy of the pixel value to modulate the quantization threshold,
 * combined with a highpass filter to detect structure.
 *
 * https://doi.org/10.1007/s00371-013-0895-0
 *
 * @param kernel error diffusion kernel to use
 * @param c modulation strength: 7.6 and 16.4 are mentioned in the paper
 */
fun ditherEntropyConstrained(
    bitmap: Pixels,
    pixelSize: Int = 1,
    colorCount: Int = 256,
    kernel: Array<DitherKernelEntry> = DitherKernels.FLOYD_STEINBERG,
    c: Double = 7.6,
) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)

    // Compute luminance and highpass
    val luminanceMap = DoubleArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val p = RGBA.of(bitmap[x, y])
            luminanceMap[y * width + x] = (0.299 * p.r + 0.587 * p.g + 0.114 * p.b) / 255.0
        }
    }
    val lowpass = gaussianBlur(luminanceMap, width, height, 1.0)
    val highpass = DoubleArray(width * height) { lowpass[it] - luminanceMap[it] }

    var leftToRight = true

    for (y in 0 until height step pixelSize) {
        val xRange = (0 until width step pixelSize).let {
            if (leftToRight) it else it.reversed()
        }

        for (x in xRange) {
            val blockPixel = bitmap.calcAverageBlockColor(x, y, pixelSize)
            val oldColor = RGBA.of(blockPixel)

            val lum = (0.299 * oldColor.r + 0.587 * oldColor.g + 0.114 * oldColor.b) / 255.0
            val entropyMod = c * binaryEntropy(lum) * highpass[y * width + x]
            val modOffset = (entropyMod * stepSize).toInt()

            val newR = (oldColor.r + modOffset).roundToNearestQuantization(stepSize)
            val newG = (oldColor.g + modOffset).roundToNearestQuantization(stepSize)
            val newB = (oldColor.b + modOffset).roundToNearestQuantization(stepSize)

            val errorR = oldColor.r - newR
            val errorG = oldColor.g - newG
            val errorB = oldColor.b - newB

            val newColor = RGBA.of(newR, newG, newB, oldColor.a)
            bitmap.setBlock(x, y, pixelSize, newColor.value)

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

/**
 * Entropy-constrained variant using Ostromoukhov variable coefficients.
 */
fun ditherEntropyConstrainedOstromoukhov(
    bitmap: Pixels,
    pixelSize: Int = 1,
    colorCount: Int = 256,
    c: Double = 7.6,
) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)

    // Compute luminance and highpass
    val luminanceMap = DoubleArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val p = RGBA.of(bitmap[x, y])
            luminanceMap[y * width + x] = (0.299 * p.r + 0.587 * p.g + 0.114 * p.b) / 255.0
        }
    }
    val lowpass = gaussianBlur(luminanceMap, width, height, 1.0)
    val highpass = DoubleArray(width * height) { lowpass[it] - luminanceMap[it] }

    var leftToRight = true

    for (y in 0 until height step pixelSize) {
        val xRange = (0 until width step pixelSize).let {
            if (leftToRight) it else it.reversed()
        }

        for (x in xRange) {
            val blockPixel = bitmap.calcAverageBlockColor(x, y, pixelSize)
            val oldColor = RGBA.of(blockPixel)

            val lum = (0.299 * oldColor.r + 0.587 * oldColor.g + 0.114 * oldColor.b) / 255.0
            val entropyMod = c * binaryEntropy(lum) * highpass[y * width + x]
            val modOffset = (entropyMod * stepSize).toInt()

            val newR = (oldColor.r + modOffset).roundToNearestQuantization(stepSize)
            val newG = (oldColor.g + modOffset).roundToNearestQuantization(stepSize)
            val newB = (oldColor.b + modOffset).roundToNearestQuantization(stepSize)

            val errorR = oldColor.r - newR
            val errorG = oldColor.g - newG
            val errorB = oldColor.b - newB

            val newColor = RGBA.of(newR, newG, newB, oldColor.a)
            bitmap.setBlock(x, y, pixelSize, newColor.value)

            // Ostromoukhov variable coefficients
            val lumIdx = (lum * 255.0).toInt().coerceIn(0, 255)
            val coeff = ostromoukhovCoefficients(lumIdx)
            val cRight = coeff[0]
            val cBottomLeft = coeff[1]
            val cBottom = coeff[2]
            val cSum = coeff[3].toDouble()

            val dxSign = if (leftToRight) 1 else -1

            val rightX = x + dxSign * pixelSize
            if (rightX in 0 until width) {
                bitmap.addBlockColor(rightX, y, pixelSize, (errorR * cRight / cSum).toInt(), (errorG * cRight / cSum).toInt(), (errorB * cRight / cSum).toInt())
            }

            val bottomY = y + pixelSize
            if (bottomY < height) {
                val bottomLeftX = x - dxSign * pixelSize
                if (bottomLeftX in 0 until width) {
                    bitmap.addBlockColor(bottomLeftX, bottomY, pixelSize, (errorR * cBottomLeft / cSum).toInt(), (errorG * cBottomLeft / cSum).toInt(), (errorB * cBottomLeft / cSum).toInt())
                }
                bitmap.addBlockColor(x, bottomY, pixelSize, (errorR * cBottom / cSum).toInt(), (errorG * cBottom / cSum).toInt(), (errorB * cBottom / cSum).toInt())
            }
        }
        leftToRight = !leftToRight
    }
}

/**
 * Entropy-constrained variant using ZhouFang threshold modulation.
 */
fun ditherEntropyConstrainedZhouFang(
    bitmap: Pixels,
    pixelSize: Int = 1,
    colorCount: Int = 256,
    c: Double = 7.6,
) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)

    val coefficients = buildEntropyZhouFangCoefficients()
    val modulator = buildEntropyZhouFangModulator()

    // Compute luminance and highpass
    val luminanceMap = DoubleArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val p = RGBA.of(bitmap[x, y])
            luminanceMap[y * width + x] = (0.299 * p.r + 0.587 * p.g + 0.114 * p.b) / 255.0
        }
    }
    val lowpass = gaussianBlur(luminanceMap, width, height, 1.0)
    val highpass = DoubleArray(width * height) { lowpass[it] - luminanceMap[it] }

    var leftToRight = true

    for (y in 0 until height step pixelSize) {
        val xRange = (0 until width step pixelSize).let {
            if (leftToRight) it else it.reversed()
        }

        for (x in xRange) {
            val blockPixel = bitmap.calcAverageBlockColor(x, y, pixelSize)
            val oldColor = RGBA.of(blockPixel)

            val lum = (0.299 * oldColor.r + 0.587 * oldColor.g + 0.114 * oldColor.b) / 255.0
            val lumIdx = (lum * 255.0).toInt().coerceIn(0, 255)

            // ZhouFang threshold modulation + entropy modulation
            val rnd = kotlin.random.Random.nextDouble()
            val mod = modulator[lumIdx]
            val zfMod = 0.5 + (rnd % 0.5) * mod
            val entropyMod = c * binaryEntropy(lum) * highpass[y * width + x]
            val modOffset = ((zfMod - 0.5 + entropyMod) * stepSize).toInt()

            val newR = (oldColor.r + modOffset).roundToNearestQuantization(stepSize)
            val newG = (oldColor.g + modOffset).roundToNearestQuantization(stepSize)
            val newB = (oldColor.b + modOffset).roundToNearestQuantization(stepSize)

            val errorR = oldColor.r - newR
            val errorG = oldColor.g - newG
            val errorB = oldColor.b - newB

            val newColor = RGBA.of(newR, newG, newB, oldColor.a)
            bitmap.setBlock(x, y, pixelSize, newColor.value)

            val coeff = coefficients[lumIdx]
            val dxSign = if (leftToRight) 1 else -1

            val directions = arrayOf(Pair(1, 0), Pair(-1, 1), Pair(0, 1))
            for (i in 0..2) {
                val (dx, dy) = directions[i]
                val nx = x + dx * dxSign * pixelSize
                val ny = y + dy * pixelSize
                if (nx in 0 until width && ny in 0 until height) {
                    bitmap.addBlockColor(
                        nx, ny, pixelSize,
                        (errorR * coeff[i]).toInt(),
                        (errorG * coeff[i]).toInt(),
                        (errorB * coeff[i]).toInt()
                    )
                }
            }
        }
        leftToRight = !leftToRight
    }
}

private fun buildEntropyZhouFangModulator(): DoubleArray {
    val keyValues = listOf(
        Pair(0, doubleArrayOf(0.0)),
        Pair(44, doubleArrayOf(0.34)),
        Pair(64, doubleArrayOf(0.50)),
        Pair(85, doubleArrayOf(1.00)),
        Pair(95, doubleArrayOf(0.17)),
        Pair(102, doubleArrayOf(0.5)),
        Pair(107, doubleArrayOf(0.7)),
        Pair(112, doubleArrayOf(0.79)),
        Pair(127, doubleArrayOf(1.00)),
    )
    return interpolateAndMirror(keyValues).map { it[0] }.toDoubleArray()
}

private fun buildEntropyZhouFangCoefficients(): Array<DoubleArray> {
    val keyValues = listOf(
        Pair(0, doubleArrayOf(13.0, 0.0, 5.0)),
        Pair(1, doubleArrayOf(1300249.0, 0.0, 499250.0)),
        Pair(2, doubleArrayOf(213113.0, 287.0, 99357.0)),
        Pair(3, doubleArrayOf(351854.0, 0.0, 199965.0)),
        Pair(4, doubleArrayOf(801100.0, 0.0, 490999.0)),
        Pair(10, doubleArrayOf(704075.0, 297466.0, 303694.0)),
        Pair(22, doubleArrayOf(46613.0, 31917.0, 21469.0)),
        Pair(32, doubleArrayOf(47482.0, 30617.0, 21900.0)),
        Pair(44, doubleArrayOf(43024.0, 42131.0, 14826.0)),
        Pair(64, doubleArrayOf(36411.0, 43219.0, 20369.0)),
        Pair(72, doubleArrayOf(38477.0, 53843.0, 7678.0)),
        Pair(77, doubleArrayOf(40503.0, 51547.0, 7948.0)),
        Pair(85, doubleArrayOf(35865.0, 34108.0, 30026.0)),
        Pair(95, doubleArrayOf(34117.0, 36899.0, 28983.0)),
        Pair(102, doubleArrayOf(35464.0, 35049.0, 29485.0)),
        Pair(107, doubleArrayOf(16477.0, 18810.0, 14712.0)),
        Pair(112, doubleArrayOf(33360.0, 37954.0, 28685.0)),
        Pair(127, doubleArrayOf(35269.0, 36066.0, 28664.0)),
    )
    val interpolated = interpolateAndMirror(keyValues)
    return interpolated.map { row ->
        val sum = row.sum()
        if (sum > 0) DoubleArray(row.size) { row[it] / sum } else row
    }.toTypedArray()
}
