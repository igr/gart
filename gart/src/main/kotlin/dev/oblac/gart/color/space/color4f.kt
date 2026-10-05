package dev.oblac.gart.color.space

import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import org.jetbrains.skia.Color4f
import kotlin.math.sqrt

fun Number.color4f(): Color4f = Color4f(this.toInt())

/**
 * Relative luminance, as WCAG defines it: the channels go linear with the sRGB curve, then
 * they get the Rec. 709 weights. Black is 0, white is 1, and a mid grey of 0.5 is about 0.21.
 * For the weights on the encoded channels, see [luma].
 * https://www.w3.org/TR/WCAG21/#dfn-relative-luminance
 */
val Color4f.luminance: Float
    get() = (0.2126 * gammaAdjustSRGB(r.toDouble()) + 0.7152 * gammaAdjustSRGB(g.toDouble()) + 0.0722 * gammaAdjustSRGB(b.toDouble())).toFloat()

/**
 * Rec. 709 luma: the luminance weights on the encoded channels, with no linear step.
 */
val Color4f.luma: Float
    get() = 0.2126f * r + 0.7152f * g + 0.0722f * b

/**
 * Calculates the contrast value between this color and the given color
 * contrast value is according to
 * http://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef
 */
fun Color4f.contrastRatio(other: Color4f): Double {
    val l1 = luminance
    val l2 = other.luminance
    return if (l1 > l2) (l1 + 0.05) / (l2 + 0.05) else (l2 + 0.05) / (l1 + 0.05)
}

fun Color4f.mix(other: Color4f, f: Float = 0.5f): Color4f {
    return Color4f(
        r + f * (other.r - r),
        g + f * (other.g - g),
        b + f * (other.b - b),
        a + f * (other.a - a)
    )
}

fun Color4f.mixLrgb(other: Color4f, f: Float = 0.5f): Color4f {
    return Color4f(
        sqrt(r * r * (1 - f) + other.r * other.r * f),
        sqrt(g * g * (1 - f) + other.g * other.g * f),
        sqrt(b * b * (1 - f) + other.b * other.b * f),
        a + f * (other.a - a)
    )
}

fun Color4f.Companion.of(r: Int, g: Int, b: Int, a: Int = 255) = Color4f(
    r.coerceIn(0, 255) / 255f,
    g.coerceIn(0, 255) / 255f,
    b.coerceIn(0, 255) / 255f,
    a.coerceIn(0, 255) / 255f
)

fun Color4f.Companion.of(color: Int): Color4f {
    return of(red(color), green(color), blue(color), alpha(color))
}


/**
 * A [Color4f] you can write into: one instance, reused, for per-pixel or per-ray loops that would
 * otherwise make a new colour at every step. [Color4f] is immutable.
 *
 * The channels are plain floats with no clamp and no encoding. A channel holds what you put in, for
 * example linear light far over 1. Helpers like [luminance] read a [Color4f] as sRGB, so convert
 * linear values before you use them there.
 *
 * Like [dev.oblac.gart.vector.MutableVec2], it has no operator that returns a new instance. The
 * index operators let a loop go over the channels: 0 is red, 1 green, 2 blue, 3 alpha.
 */
class MutableColor4f(var r: Float = 0f, var g: Float = 0f, var b: Float = 0f, var a: Float = 1f) {

    fun set(r: Float, g: Float, b: Float, a: Float = this.a): MutableColor4f {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
        return this
    }

    fun set(c: Color4f) = set(c.r, c.g, c.b, c.a)

    /** Sets every channel to 0, alpha too: the start of a sum. */
    fun zero() = set(0f, 0f, 0f, 0f)

    operator fun get(i: Int): Float = when (i) {
        0 -> r
        1 -> g
        2 -> b
        3 -> a
        else -> throw IndexOutOfBoundsException("channel $i, must be 0..3")
    }

    operator fun set(i: Int, v: Float) {
        when (i) {
            0 -> r = v
            1 -> g = v
            2 -> b = v
            3 -> a = v
            else -> throw IndexOutOfBoundsException("channel $i, must be 0..3")
        }
    }

    /** Freezes the current value into an immutable [Color4f]. */
    fun toColor4f() = Color4f(r, g, b, a)

    override fun toString() = "MutableColor4f($r, $g, $b, $a)"
}
