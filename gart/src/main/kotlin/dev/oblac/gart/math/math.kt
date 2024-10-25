package dev.oblac.gart.math

import kotlin.math.cos
import kotlin.math.sin

fun Int.isEven() = this % 2 == 0
fun Int.isOdd() = this % 2 == 1

fun Float.toRadian(): Float = (this / 180 * Math.PI).toFloat()
fun Float.toDegree(): Float = (this * 180 / Math.PI).toFloat()

/**
 * Safe degrees' subtraction.
 */
fun Float.subDeg(delta: Number) = normalizeDeg(this - delta.toFloat())

/**
 * Safe degrees' addition.
 */
fun Float.addDeg(delta: Number) = normalizeDeg(this - delta.toFloat())

fun sinDeg(degrees: Number) = sin(degrees.toFloat().toRadian())
fun cosDeg(degrees: Number) = cos(degrees.toFloat().toRadian())

fun Float.format(digits: Int) = "%.${digits}f".format(this)

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
