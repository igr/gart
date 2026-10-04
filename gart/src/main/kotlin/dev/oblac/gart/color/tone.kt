package dev.oblac.gart.color

import dev.oblac.gart.math.lerp
import kotlin.math.exp
import kotlin.math.pow

/**
 * Light to tone: `(1 - e^(-exposure l))^gamma`. Any light `l >= 0` maps into `0..1`.
 *
 * The exponential rolls bright light off toward white, so nothing clips. It works like film:
 * each extra unit of light exposes the same share of the film that is still clear. A gamma over 1
 * sinks the mid tones.
 */
class ToneCurve(val exposure: Float, val gamma: Float = 1f) {
    operator fun invoke(l: Float): Float = (1f - exp(-exposure * l)).pow(gamma)
}

/**
 * Ink to paper in 256 steps, mixed in linear light (gamma 2.2), the way printed dots mix from
 * a step back. Each channel goes to linear light with the power 2.2, mixes there, and goes back
 * with the power 1/2.2. A plain sRGB lerp sits too dark in the mid tones.
 */
class InkRamp(ink: Int, paper: Int) {

    private val lut = IntArray(256) { i ->
        // one channel from ink to paper, mixed in light
        fun ch(a: Int, b: Int) = (lerp((a / 255f).pow(2.2f), (b / 255f).pow(2.2f), i / 255f).pow(1f / 2.2f) * 255f + 0.5f).toInt()
        rgb(ch(red(ink), red(paper)), ch(green(ink), green(paper)), ch(blue(ink), blue(paper)))
    }

    /**
     * The color at [t]: 0 is the ink, 1 is the paper. Values outside `0..1` clamp.
     */
    fun at(t: Float): Int = lut[(t.coerceIn(0f, 1f) * 255f + 0.5f).toInt()]

    /**
     * Each channel goes through the ramp on its own, then they are packed with alpha 255.
     */
    fun rgb(r: Float, g: Float, b: Float): Int = rgb(red(at(r)), green(at(g)), blue(at(b)))
}
