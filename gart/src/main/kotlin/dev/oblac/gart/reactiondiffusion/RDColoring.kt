package dev.oblac.gart.reactiondiffusion

import dev.oblac.gart.color.argb
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red

/**
 * One color stop on a reaction-diffusion gradient.
 *
 * @param color ARGB integer. The alpha bits are ignored by [RDColoring]; the
 *        produced pixels are always fully opaque.
 * @param threshold the `[0, 1]` value at which this stop is fully applied.
 */
data class RDColorStop(val color: Int, val threshold: Float)

/**
 * 5-stop (or more) gradient mapping a scalar `[0, 1]` value to an ARGB pixel.
 *
 * Ports the `coloringFragment` GLSL shader from the original addon. For an
 * input `value`:
 * - below the first stop's threshold, returns the first stop's RGB
 * - above the last stop's threshold, returns the last stop's RGB
 * - otherwise, linearly blends between the bracketing stops in RGB space
 *
 * Stops must be sorted by threshold. Output alpha is always `0xFF`.
 */
class RDColoring(stops: List<RDColorStop>) {

    private val stops: List<RDColorStop> = stops.sortedBy { it.threshold }.also {
        require(it.isNotEmpty()) { "RDColoring needs at least one stop" }
    }

    fun colorAt(value: Float): Int {
        val first = stops.first()
        if (value <= first.threshold) return opaque(first.color)
        for (i in 1 until stops.size) {
            val prev = stops[i - 1]
            val curr = stops[i]
            if (value <= curr.threshold) {
                val span = curr.threshold - prev.threshold
                val t = if (span > 0f) (value - prev.threshold) / span else 0f
                return mixRgb(prev.color, curr.color, t)
            }
        }
        return opaque(stops.last().color)
    }

    private fun opaque(color: Int): Int = argb(255, red(color), green(color), blue(color))

    private fun mixRgb(c0: Int, c1: Int, t: Float): Int {
        val r = red(c0) + ((red(c1) - red(c0)) * t).toInt()
        val g = green(c0) + ((green(c1) - green(c0)) * t).toInt()
        val b = blue(c0) + ((blue(c1) - blue(c0)) * t).toInt()
        return argb(255, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    companion object {
        /**
         * Default gradient from the original addon's constructor.
         * Thresholds 0.0 / 0.2 / 0.4 / 0.6 / 0.8 over:
         *   black, teal, magenta, pink, cyan.
         */
        val Default: RDColoring = RDColoring(
            listOf(
                RDColorStop(argb(255, 0, 0, 0), 0.0f),
                RDColorStop(argb(255, 0, 103, 174), 0.2f),
                RDColorStop(argb(255, 200, 21, 187), 0.4f),
                RDColorStop(argb(255, 255, 42, 126), 0.6f),
                RDColorStop(argb(255, 5, 199, 234), 0.8f),
            )
        )
    }
}
