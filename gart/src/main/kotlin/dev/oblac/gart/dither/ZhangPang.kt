package dev.oblac.gart.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Rapidly creating structure-aware halftoning with improved error diffusion
 * (Zhang & Pang, 2009). Uses image transfer function (ITF) based on spatial
 * activity to preserve structure in the halftone output.
 *
 * https://doi.org/10.1109/CISP.2009.5303919
 *
 * @param c ITF coefficient: 0.013 is used by the paper, 0.0065 for a "softer" look
 */
fun ditherZhangPang(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256, c: Double = 0.013) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)

    val coefficients = buildZhangPangCoefficients()
    val modulator = buildZhangPangModulator()

    // Compute original luminance map (before any error diffusion)
    val originalLum = IntArray(width * height)
    for (y in 0 until height step pixelSize) {
        for (x in 0 until width step pixelSize) {
            val p = bitmap.calcAverageBlockColor(x, y, pixelSize)
            val col = RGBA.of(p)
            originalLum[y * width + x] = (0.299 * col.r + 0.587 * col.g + 0.114 * col.b).toInt().coerceIn(0, 255)
        }
    }

    // Spatial activity weights for 3x3 window
    val w = doubleArrayOf(0.1035, 0.1465, 0.1035, 0.1465, 0.0, 0.1465, 0.1035, 0.1465, 0.1035)

    var leftToRight = true

    for (y in 0 until height step pixelSize) {
        val xRange = (0 until width step pixelSize).let {
            if (leftToRight) it else it.reversed()
        }

        for (x in xRange) {
            val blockPixel = bitmap.calcAverageBlockColor(x, y, pixelSize)
            val oldColor = RGBA.of(blockPixel)
            val luminance = (0.299 * oldColor.r + 0.587 * oldColor.g + 0.114 * oldColor.b).toInt().coerceIn(0, 255)

            // Compute ITF (image transfer function) for structure awareness
            var itf = 0.0
            if (x > 0 && x + pixelSize < width && y > 0 && y + pixelSize < height) {
                // Compute average luminosity in 3x3 neighborhood
                var sum = 0.0
                var count = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val ny = y + dy * pixelSize
                        val nx = x + dx * pixelSize
                        if (nx in 0 until width && ny in 0 until height) {
                            sum += originalLum[ny * width + nx]
                            count++
                        }
                    }
                }
                val avgLum = sum / count

                // Spatial variation using weighted visual perception error
                var spatialVariation = 0.0
                var idx = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val ny = y + dy * pixelSize
                        val nx = x + dx * pixelSize
                        if (nx in 0 until width && ny in 0 until height) {
                            spatialVariation += w[idx] * abs(originalLum[ny * width + nx] - avgLum)
                        }
                        idx++
                    }
                }
                val spatialActivityMeasure = spatialVariation * (originalLum[y * width + x] - avgLum)
                itf = c * avgLum * spatialActivityMeasure
                itf = max(-127.0, min(127.0, itf))
            }

            // Threshold modulation
            val rnd = Random.nextDouble()
            val mod = modulator[luminance]
            val modThreshold = (0.5 + (rnd % 0.5) * mod) * stepSize

            val newR = (oldColor.r + itf).toInt().roundToNearestQuantization(stepSize)
            val newG = (oldColor.g + itf).toInt().roundToNearestQuantization(stepSize)
            val newB = (oldColor.b + itf).toInt().roundToNearestQuantization(stepSize)

            val errorR = oldColor.r - newR
            val errorG = oldColor.g - newG
            val errorB = oldColor.b - newB

            val newColor = RGBA.of(newR, newG, newB, oldColor.a)
            bitmap.setBlock(x, y, pixelSize, newColor.value)

            // Get intensity-dependent coefficients
            val coeff = coefficients[luminance]
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

private fun buildZhangPangModulator(): DoubleArray {
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

private fun buildZhangPangCoefficients(): Array<DoubleArray> {
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
