package dev.oblac.gart.color.space

import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import org.jetbrains.skia.Color4f
import kotlin.math.sqrt

fun Number.color4f(): Color4f = Color4f(this.toInt())

/**
 * Calculates luminance value according to
 * https://www.w3.org/TR/2008/REC-WCAG20-20081211/#relativeluminancedef
 */
val Color4f.luminance: Float
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

