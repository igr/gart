package ac.obl.gart.math

import kotlin.math.cos
import kotlin.math.sin

fun Float.toRadian(): Float = (this / 180 * Math.PI).toFloat()
fun Double.toRadian(): Double = this / 180 * Math.PI

fun Float.toDegree(): Float = (this * 180 / Math.PI).toFloat()
fun Double.toDegree(): Double = this * 180 / Math.PI

/**
 * Safe degrees' subtraction, result is never bellow 0.
 * Not suitable for the large numbers!
 */
fun Float.subDeg(delta: Number): Float {
	var result = this - delta.toFloat()
	while (result < 0) {
		result += 360
	}
	return result
}

/**
 * Sage degrees' addition, result is never bellow 360.
 * Not suitable for the large numbers!
 */
fun Float.addDeg(delta: Number): Float {
	var result = this - delta.toFloat()
	while (result > 360) {
		result -= 360
	}
	return result
}

fun sind(degrees: Number) = sin(degrees.toFloat().toRadian())
fun cosd(degrees: Number) = cos(degrees.toFloat().toRadian())
