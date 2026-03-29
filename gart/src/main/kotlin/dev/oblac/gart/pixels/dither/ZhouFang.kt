package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization
import kotlin.random.Random

/**
 * Improving mid-tone quality of variable-coefficient error diffusion
 * using threshold modulation (Zhou & Fang, 2003).
 * Uses intensity-dependent coefficients with threshold modulation
 * to reduce artifacts in mid-tone regions.
 *
 * https://doi.org/10.1145/882262.882289
 */
fun ditherZhouFang(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)

    val coefficients = buildZhouFangCoefficients()
    val modulator = buildZhouFangModulator()

    var leftToRight = true

    for (y in 0 until height step pixelSize) {
        val xRange = (0 until width step pixelSize).let {
            if (leftToRight) it else it.reversed()
        }

        for (x in xRange) {
            val blockPixel = bitmap.calcAverageBlockColor(x, y, pixelSize)
            val oldColor = RGBA.of(blockPixel)

            val luminance = ((0.299 * oldColor.r + 0.587 * oldColor.g + 0.114 * oldColor.b).toInt()).coerceIn(0, 255)

            // Threshold modulation based on luminance
            val rnd = Random.nextDouble()
            val mod = modulator[luminance]
            val modOffset = (0.5 + (rnd % 0.5) * mod - 0.5) * stepSize

            val newR = (oldColor.r + modOffset).toInt().roundToNearestQuantization(stepSize)
            val newG = (oldColor.g + modOffset).toInt().roundToNearestQuantization(stepSize)
            val newB = (oldColor.b + modOffset).toInt().roundToNearestQuantization(stepSize)

            val errorR = oldColor.r - newR
            val errorG = oldColor.g - newG
            val errorB = oldColor.b - newB

            val newColor = RGBA.of(newR, newG, newB, oldColor.a)
            bitmap.setBlock(x, y, pixelSize, newColor.value)

            // Get intensity-dependent coefficients for 3 neighbors
            val coeff = coefficients[luminance]
            val dxSign = if (leftToRight) 1 else -1

            // Directions: right, bottom-left, bottom
            val directions = arrayOf(
                Pair(1, 0),   // right
                Pair(-1, 1),  // bottom-left
                Pair(0, 1),   // bottom
            )
            for (i in 0..2) {
                val (dx, dy) = directions[i]
                val nx = x + dx * dxSign * pixelSize
                val ny = y + dy * pixelSize
                if (nx in 0 until width && ny in 0 until height) {
                    val w = coeff[i]
                    bitmap.addBlockColor(
                        nx, ny, pixelSize,
                        (errorR * w).toInt(),
                        (errorG * w).toInt(),
                        (errorB * w).toInt()
                    )
                }
            }
        }
        leftToRight = !leftToRight
    }
}

// Interpolation and mirroring utility for ZhouFang/ZhangPang tables
internal fun interpolateAndMirror(keyValues: List<Pair<Int, DoubleArray>>): Array<DoubleArray> {
    val segments = mutableListOf<DoubleArray>()
    for (i in 1 until keyValues.size) {
        val (prevKey, prevVal) = keyValues[i - 1]
        val (curKey, curVal) = keyValues[i]
        val isLast = i == keyValues.size - 1
        val count = curKey - prevKey + (if (isLast) 1 else 0)
        val dims = prevVal.size

        for (j in 0 until count) {
            val t = if (count <= 1) 0.0 else j.toDouble() / (count - if (isLast) 1 else 0).coerceAtLeast(1)
            val interpolated = DoubleArray(dims) { d -> prevVal[d] + t * (curVal[d] - prevVal[d]) }
            segments.add(interpolated)
        }
    }

    // Mirror: concat with reversed copy
    val result = segments.toMutableList()
    result.addAll(segments.reversed())

    // Should be 256 entries; pad or trim if needed
    while (result.size < 256) result.add(result.last())
    return result.take(256).toTypedArray()
}

private fun buildZhouFangModulator(): DoubleArray {
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

private fun buildZhouFangCoefficients(): Array<DoubleArray> {
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
    // Normalize: divide each row by its sum
    return interpolated.map { row ->
        val sum = row.sum()
        if (sum > 0) DoubleArray(row.size) { row[it] / sum } else row
    }.toTypedArray()
}
