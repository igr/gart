package dev.oblac.gart.math

fun Int.isEven() = this % 2 == 0
fun Int.isOdd() = this % 2 == 1
fun Float.format(digits: Int) = "%.${digits}f".format(this)

fun hypotFast(a: Float, b: Float): Float {
    return fastSqrt(a * a + b * b)
}
fun hypotFast(a: Double, b: Double): Double {
    return fastSqrt(a * a + b * b)
}
