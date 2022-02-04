package ac.obl.gart.gfx

data class HSLA(
	/**
	 * Hue, degrees, `NaN` for monochrome colors. 0 -360 degrees
	 */
	val h: Float,
	/**
	 * Saturation percentage [0, 100]
	 */
	val s: Float,
	/**
	 * Lightness percentage [0, 100]
	 */
	val l: Float,

	/**
	 * Alpha percentage [0, 100].
	 */
	val a: Float = 1f)