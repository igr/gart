package ac.obl.gart.math

/**
 * Re-maps a number from one range to another.
 * @param value the incoming value to be converted
 * @param start1 lower bound of the value's current range
 * @param stop1 upper bound of the value's current range
 * @param start2 lower bound of the value's target range
 * @param stop2 upper bound of the value's target range
 */
fun map(value: Number, start1: Number, stop1: Number, start2: Number, stop2: Number): Float {
	val s1 = start1.toFloat()
	val delta1 = value.toFloat() - s1
	val total1 = stop1.toFloat() - s1
	val percentage = delta1 / total1

	val s2 = start2.toFloat()
	val total2 = stop2.toFloat() - s2
	val delta2 = percentage * total2

	return s2 + delta2
}