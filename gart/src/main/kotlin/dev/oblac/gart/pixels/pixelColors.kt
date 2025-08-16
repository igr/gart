package dev.oblac.gart.pixels

import kotlin.math.roundToInt

/**
* Quantize an integer to the nearest quantization step.
*/
fun Int.roundToNearestQuantization(stepSize: Int): Int {
    return (this.toDouble() / stepSize).roundToInt() * stepSize
}
