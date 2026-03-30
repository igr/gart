package dev.oblac.gart.math

import dev.oblac.gart.pixels.PadMode
import dev.oblac.gart.pixels.uniformFilter
import kotlin.math.sqrt

/**
 * Computes per-pixel standard deviation over a window of given radius.
 * Uses the formula: stddev = sqrt(E[x^2] - E[x]^2).
 */
fun windowedStdDev(data: DoubleArray, w: Int, h: Int, radius: Int, mode: PadMode = PadMode.REFLECT): DoubleArray {
    val size = radius * 2
    val origin = -radius
    val mean = uniformFilter(data, w, h, size, mode, origin)
    val squared = DoubleArray(data.size) { data[it] * data[it] }
    val meanSquared = uniformFilter(squared, w, h, size, mode, origin)

    return DoubleArray(w * h) {
        sqrt((meanSquared[it] - mean[it] * mean[it]).coerceAtLeast(0.0))
    }
}

/**
 * Global standard deviation of the entire array.
 */
fun globalStdDev(data: DoubleArray): Double {
    val mean = data.average()
    val variance = data.sumOf { (it - mean) * (it - mean) } / data.size
    return sqrt(variance)
}
