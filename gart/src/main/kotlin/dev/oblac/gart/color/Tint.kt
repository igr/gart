package dev.oblac.gart.color

import dev.oblac.gart.color.space.MutableColor4f
import kotlin.math.pow

/**
 * A tint: linear r, g, b at luminance 1. It keeps only the hue and the saturation of a colour.
 *
 * Multiply a light by a tint to colour the light, and the brightness of the light does not
 * change. A tint is a multiplier, not a colour to draw: strong colours go over 1, and pure
 * blue is about 13.9. [tintOf] makes one from a colour.
 */
data class Tint(val r: Float, val g: Float, val b: Float) {

    /**
     * The channel at [i]: 0 is r, 1 is g, 2 is b. For loops over the channels.
     */
    operator fun get(i: Int): Float = when (i) {
        0 -> r
        1 -> g
        2 -> b
        else -> throw IndexOutOfBoundsException("a tint has 3 channels, got $i")
    }

    companion object {
        /**
         * No tint. Greys, black and white give it, and a light keeps its colour under it.
         */
        val NEUTRAL = Tint(1f, 1f, 1f)
    }
}

/**
 * Tint of a packed ARGB [color].
 *
 * The channels go linear with gamma 2.2, then they are divided by their Rec. 709 luminance.
 * Greys and black give exactly [Tint.NEUTRAL]. The alpha of [color] does not count.
 */
fun tintOf(color: Int): Tint {
    fun lin(shift: Int) = (((color shr shift) and 255) / 255f).pow(2.2f)
    val v = MutableColor4f(lin(16), lin(8), lin(0))
    if (v.r == v.g && v.g == v.b) return Tint.NEUTRAL // exactly 1, so a grey light changes no bits
    val y = 0.2126f * v.r + 0.7152f * v.g + 0.0722f * v.b
    return Tint(v.r / y, v.g / y, v.b / y)
}
