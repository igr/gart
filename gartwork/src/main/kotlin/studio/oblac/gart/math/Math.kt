package studio.oblac.gart.math

import kotlin.math.cos
import kotlin.math.sin

fun Float.toRadian(): Float = (this / 180 * Math.PI).toFloat()
fun Double.toRadian(): Double = this / 180 * Math.PI

fun Float.toDegree(): Float = (this * 180 / Math.PI).toFloat()
fun Double.toDegree(): Double = this * 180 / Math.PI

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

fun normalizeDeg(deg: Float): Float {
    var result = deg
    while (result < 0) {
        result += 360
    }
    while (result > 360) {
        result -= 360
    }
    return result
}

/**
 * Calculates the middle angle between two angles. The result is always in the range of -PI..PI.
 * This means that the result is always the shortest angle between the two angles.
 */
fun middleAngle(a: Float, b: Float): Float {
    return when (val diff = b - a) {
        in -PIf..PIf -> normalizeRad(a + diff / 2)
        in PIf..DOUBLE_PIf -> normalizeRad(a + diff / 2 - PIf)
        in -DOUBLE_PIf..-PIf -> normalizeRad(a + diff / 2 + PIf)
        in DOUBLE_PIf..2 * DOUBLE_PIf -> normalizeRad(a + (diff - DOUBLE_PIf) / 2)
        in -2 * DOUBLE_PIf..-DOUBLE_PIf -> normalizeRad(a + (diff + DOUBLE_PIf) / 2)
        else -> throw IllegalStateException("Unexpected angle difference: $diff")
    }
}
