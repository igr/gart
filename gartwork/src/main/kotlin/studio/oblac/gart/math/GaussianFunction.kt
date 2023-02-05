package studio.oblac.gart.math

import kotlin.math.exp
import kotlin.math.pow

/**
 * Gaussian function.
 * https://en.wikipedia.org/wiki/Gaussian_function
 */
class GaussianFunction(private val height: Number, private val center: Number, private val width: Number) {

	operator fun invoke(x: Number): Float =
		height.toFloat() *
		exp(
			-((x.toFloat() - center.toFloat()).pow(2) / (2 * width.toFloat() * width.toFloat()))
		)
}
