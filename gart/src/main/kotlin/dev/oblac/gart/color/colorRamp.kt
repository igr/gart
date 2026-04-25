package dev.oblac.gart.color

/**
 * One color stop on a color ramp.
 *
 * @param color ARGB integer. Alpha is preserved and interpolated by [ColorRamp].
 * @param threshold the `[0, 1]` value at which this stop is fully applied.
 */
data class ColorStop(val color: Int, val threshold: Float)

/**
 * Gradient mapping a scalar `[0, 1]` value to an ARGB pixel.
 *
 * For an input `value`:
 * - below the first stop's threshold, returns the first stop's color
 * - above the last stop's threshold, returns the last stop's color
 * - otherwise, linearly blends between the bracketing stops in ARGB space
 *
 * Stops must be sorted by threshold. Alpha is interpolated alongside RGB.
 */
class ColorRamp(stops: List<ColorStop>) {

    private val stops: List<ColorStop> = stops.sortedBy { it.threshold }.also {
        require(it.isNotEmpty()) { "ColorRamp needs at least one stop" }
    }

    fun colorAt(value: Float): Int {
        val first = stops.first()
        if (value <= first.threshold) return first.color
        for (i in 1 until stops.size) {
            val prev = stops[i - 1]
            val curr = stops[i]
            if (value <= curr.threshold) {
                val span = curr.threshold - prev.threshold
                val t = if (span > 0f) (value - prev.threshold) / span else 0f
                return lerpColor(prev.color, curr.color, t)
            }
        }
        return stops.last().color
    }

    companion object {
        /**
         * Default gradient from the original addon's constructor.
         * Thresholds 0.0 / 0.2 / 0.4 / 0.6 / 0.8 over:
         *   black, teal, magenta, pink, cyan.
         */
        val Default: ColorRamp = ColorRamp(
            listOf(
                ColorStop(argb(255, 0, 0, 0), 0.0f),
                ColorStop(argb(255, 0, 103, 174), 0.2f),
                ColorStop(argb(255, 200, 21, 187), 0.4f),
                ColorStop(argb(255, 255, 42, 126), 0.6f),
                ColorStop(argb(255, 5, 199, 234), 0.8f),
            )
        )

        /**
         * Build a [ColorRamp] from [palette] with stops spread evenly across `[0, 1]`.
         * The first color maps to `0f`, the last to `1f`. A single-color palette
         * yields a constant ramp.
         */
        fun of(palette: Palette): ColorRamp {
            val denom = (palette.size - 1).toFloat()
            return ColorRamp(palette.indices.map { i -> ColorStop(palette[i], i / denom) })
        }
    }
}
