package dev.oblac.gart.math

/**
 * Re-maps a number from one range to another.
 * @param value the incoming value to be converted
 * @param inLower lower bound of the value's current range
 * @param inUpper upper bound of the value's current range
 * @param outLower lower bound of the value's target range
 * @param outUpper upper bound of the value's target range
 */
fun map(value: Number, inLower: Number, inUpper: Number, outLower: Number, outUpper: Number): Float {
    val s1 = inLower.toFloat()
	val delta1 = value.toFloat() - s1
    val total1 = inUpper.toFloat() - s1
	val percentage = delta1 / total1

    val s2 = outLower.toFloat()
    val total2 = outUpper.toFloat() - s2
	val delta2 = percentage * total2

	return s2 + delta2
}
