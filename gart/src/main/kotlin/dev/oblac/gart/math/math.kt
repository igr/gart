package dev.oblac.gart.math

import kotlin.math.floor

fun Double.f() = toFloat()
fun Int.f() = toFloat()

fun Int.isEven() = this % 2 == 0
fun Int.isOdd() = this % 2 == 1
fun Float.format(digits: Int) = "%.${digits}f".format(this)

fun hypotFast(a: Float, b: Float): Float {
    return fastSqrt(a * a + b * b)
}
fun hypotFast(a: Double, b: Double): Double {
    return fastSqrt(a * a + b * b)
}

fun mod(a: Double, b: Double) = ((a % b) + b) % b
fun mod(a: Int, b: Int) = ((a % b) + b) % b
fun mod(a: Float, b: Float) = ((a % b) + b) % b
fun mod(a: Long, b: Long) = ((a % b) + b) % b


// Helper function for smoothstep
fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

// Helper function for fractional part (equivalent to frac in shader)
fun frac(value: Float): Float = value - floor(value)
fun frac(value: Double): Double = value - floor(value)

// Helper function for linear interpolation (equivalent to lerp in shader)
fun lerp(a: Float, b: Float, t: Float) = a + t * (b - a)
fun lerp(a: Double, b: Double, t: Double) = a + t * (b - a)
