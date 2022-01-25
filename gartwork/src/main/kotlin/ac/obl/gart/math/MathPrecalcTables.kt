package ac.obl.gart.math

import kotlin.math.cos
import kotlin.math.sin

sealed class MathPrecalcTable(private val resolution: Float, calculation: (Float) -> Float) {
	private val values: FloatArray
	init {
		val total = (360 / resolution).toInt()
		values = FloatArray(total)

		var v = 0f
		var i = 0
		while (v < 360) {
			values[i] = calculation(v)
			v += resolution
			i++
		}
	}

	operator fun get(degrees: Number): Float {
		val d = degrees.toFloat() % 360
		val index = (d / resolution).toInt()

		return if (index >= 0) {
			values[index]
		} else {
			values[-index]
		}
	}
}

/**
 * Precalculated cos values.
 */
class MathCos(resolution: Float = 0.1f) : MathPrecalcTable(resolution, {
	cos(it.toRadian())
})
/**
 * Precalculated sin values.
 */
class MathSin(resolution: Float = 0.1f) : MathPrecalcTable(resolution, {
	sin(it.toRadian())
})
