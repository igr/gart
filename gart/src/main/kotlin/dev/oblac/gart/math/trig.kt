package dev.oblac.gart.math

fun Float.toRadians(): Float = (this / 180 * Math.PI).toFloat()
fun Float.toDegrees(): Float = (this * 180 / Math.PI).toFloat()

/**
 * Safe degrees' subtraction.
 */
fun Float.subDeg(delta: Number) = normalizeDeg(this - delta.toFloat())

/**
 * Safe degrees' addition.
 */
fun Float.addDeg(delta: Number) = normalizeDeg(this - delta.toFloat())

fun sinDeg(degrees: Number) = kotlin.math.sin(degrees.toFloat().toRadians())
fun cosDeg(degrees: Number) = kotlin.math.cos(degrees.toFloat().toRadians())

fun normalizeRad(rad: Float): Float {
    var result = rad
    while (result < 0) {
        result += DOUBLE_PIf
    }
    while (result > DOUBLE_PIf) {
        result -= DOUBLE_PIf
    }
    return result
}

private fun normalizeDeg(deg: Float): Float {
    var result = deg
    while (result < 0) {
        result += 360
    }
    while (result > 360) {
        result -= 360
    }
    return result
}
