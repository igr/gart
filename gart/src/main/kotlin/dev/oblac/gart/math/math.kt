package dev.oblac.gart.math

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

fun Double.f() = toFloat()
fun Int.f() = toFloat()
fun Float.d() = toDouble()
fun Float.i() = toInt()
fun Long.i() = toInt()

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

fun wrap(v: Int, size: Int): Int {
    val m = v % size
    return if (m < 0) m + size else m
}

fun wrap(v: Float, size: Float): Float {
    val r = v % size
    if (r < 0f) {
        // `r + size` can round up to exactly `size` in float; fold that back to 0
        // so the result stays in [0, size) and is safe to use as an array index.
        val s = r + size
        return if (s < size) s else 0f
    }
    return r
}


// Helper function for smoothstep
fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

fun step(threshold: Float, x: Float) =
    if (x >= threshold) 1.0f else 0.0f

/**
 * Quadratic polynomial smooth-min (iq): `min(a, b)` with the crease rounded over a band
 * of width [k]. The dip at `a == b` is `k/4`; see [sminCubic] for the shallower one.
 */
fun smin(a: Float, b: Float, k: Float): Float {
    if (k <= 1e-4f) return min(a, b)
    val h = (0.5f + 0.5f * (b - a) / k).coerceIn(0f, 1f)
    return lerp(b, a, h) - k * h * (1f - h)
}

/**
 * Cubic smooth-min (iq): dips only `k/6` at `a == b`, so the blend comes in as a long
 * tapered spindle rather than a bulge. Outside the band it returns the winner exactly,
 * not a rounded copy of it - early-outs can lean on that.
 */
fun sminCubic(a: Float, b: Float, k: Float): Float {
    if (k <= 1e-4f) return min(a, b)
    val h = max(k - abs(a - b), 0f) / k
    return min(a, b) - h * h * h * k * (1f / 6f)
}

// Helper function for fractional part (equivalent to frac in shader)
fun frac(value: Float): Float = value - floor(value)
fun frac(value: Double): Double = value - floor(value)

// Helper function for linear interpolation (equivalent to lerp in shader)
fun lerp(a: Float, b: Float, t: Float) = a + t * (b - a)
fun lerp(a: Double, b: Double, t: Double) = a + t * (b - a)
fun lerp(a: Int, b: Int, t: Float): Float = a + (b - a) * t

fun mix(a: Float, b: Float, t: Float) = a * (1f - t) + b * t

