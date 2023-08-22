package studio.oblac.gart.math

/**
 * Super-fast approximation of square root of a number, that is accurate enough for most of the cases.
 */
private const val A = 1L shl 52
private const val B = 1L shl 61
fun fastSqrt(d: Double): Double {

    val sqrt = java.lang.Double.longBitsToDouble(((java.lang.Double.doubleToLongBits(d) - A) shr 1) + B)
    return (sqrt + d / sqrt) / 2.0
}

/**
 * Less accurate, but somewhat faster approximation of square root of a number.
 */
fun fastFastSqrt(number: Double) = if (number < 100000) {
    java.lang.Double.longBitsToDouble(((java.lang.Double.doubleToRawLongBits(number) shr 32) + 1072632448) shl 31)
} else {
    java.lang.Double.longBitsToDouble(((java.lang.Double.doubleToRawLongBits(number) shr 32) + 1072679338) shl 31)
}
