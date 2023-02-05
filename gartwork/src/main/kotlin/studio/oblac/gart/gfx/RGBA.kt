package studio.oblac.gart.gfx

import kotlin.math.max
import kotlin.math.min

data class RGBA(
	val r: Int,
	val g: Int,
	val b: Int,
	val a: Int = 255
) {
	companion object {
		fun of(value: Long): RGBA {
			return this.of(value.toInt())
		}
		fun of(value: Int): RGBA {
			return RGBA(
				a = alpha(value),
				r = red(value),
				g = green(value),
				b = blue(value)
			)
		}
	}

	/**
	 * Converts RGB to HSL
	 */
	fun toHSL(): HSLA {
		val r = r / 255f
		val g = g / 255f
		val b = b / 255f

		val cmin = min(r, min(g, b))
		val cmax = max(r, max(g, b))
		val delta = cmax - cmin

		val h = when (cmax) {
				cmin -> 0f
				r -> ((60 * (g - b) / delta + 360) % 360)
				g -> (60 * (b - r) / delta + 120)
				b -> (60 * (r - g) / delta + 240)
			else -> Float.NaN
		}

		val l: Float = (cmax + cmin) / 2f
		val s: Float = if (cmax == cmin) {
			0f
		} else (if (l <= 0.5f) {
			delta / (cmax + cmin)
		} else {
			delta / (2 - cmax - cmin)
		}).toFloat()

		return HSLA(h, s * 100, l * 100, a / 255f * 100f)
	}
}
