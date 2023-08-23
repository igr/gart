package studio.oblac.gart.math

import java.lang.Double.doubleToLongBits
import java.lang.Double.longBitsToDouble

/**
 * Super-fast approximation of square root of a number, that is accurate enough for most of the cases.
 */
private const val A = 1L shl 52
private const val B = 1L shl 61
fun fastSqrt(d: Double): Double {
    val sqrt = longBitsToDouble(((doubleToLongBits(d) - A) shr 1) + B)
    return (sqrt + d / sqrt) / 2.0
}

fun fastSqrt(f: Float) = fastSqrt(f.toDouble()).toFloat()

/**
 * Less accurate, but somewhat faster approximation of square root of a number.
 */
fun fastFastSqrt(number: Double) = if (number < 100000) {
    longBitsToDouble(((java.lang.Double.doubleToRawLongBits(number) shr 32) + 1072632448) shl 31)
} else {
    longBitsToDouble(((java.lang.Double.doubleToRawLongBits(number) shr 32) + 1072679338) shl 31)
}
