package dev.oblac.gart.color

import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import org.jetbrains.skia.Color
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.Paint

fun alpha(color: Int): Int {
    return color shr 24 and 0xFF
}

fun red(color: Int): Int {
    return color shr 16 and 0xFF
}

fun green(color: Int): Int {
    return color shr 8 and 0xFF
}

fun blue(color: Int): Int {
    return color and 0xFF
}

fun rgb(r: Int, g: Int, b: Int): Int {
    return Color.makeARGB(0xFF, r, g, b)
}

fun argb(a: Int, r: Int, g: Int, b: Int): Int {
    return Color.makeARGB(a, r, g, b)
}

@JvmName("setAlpha")
fun alpha(color: Int, a: Int): Int {
    return a and 0xFF shl 24 or (color and 0x00FFFFFF)
}

fun Int.alpha(a: Int): Int = alpha(this, a)

fun red(color: Int, r: Int): Int {
    return r and 0xFF shl 16 or (color and -0xff0001)
}

fun green(color: Int, g: Int): Int {
    return g and 0xFF shl 8 or (color and -0xff01)
}

fun blue(color: Int, b: Int): Int {
    return b and 0xFF or (color and -0x100)
}

/**
 * Converts ARGB to RGBA.
 */
fun Int.covertARGBtoRGBA(): Int {
    return this shl 8 or (this ushr 24)
}

/**
 * Converts RGBA to ARGB.
 */
fun Int.convertRGBAtoARGB(): Int {
    return this ushr 8 or (this shl 24)
}


fun Number.toColor4f(): Color4f = Color4f(this.toInt())

fun Int.toFillPaint(): Paint = fillOf(this)
fun Int.toStrokePaint(width: Float): Paint = strokeOf(this, width)

fun Long.toIntColor(): Int = alpha(this.toInt(), 255)

fun blendColors(front: Int, back: Int): Int {
    val frontAlpha = alpha(front)
    val backAlpha = alpha(back)
    val alphaFactor = frontAlpha / 255f
    val invAlphaFactor = 1f - alphaFactor

    val blendedR = (red(front) * alphaFactor + red(back) * invAlphaFactor).toInt()
    val blendedG = (green(front) * alphaFactor + green(back) * invAlphaFactor).toInt()
    val blendedB = (blue(front) * alphaFactor + blue(back) * invAlphaFactor).toInt()
    val blendedA = (frontAlpha + backAlpha * invAlphaFactor).toInt()

    return argb(blendedA, blendedR, blendedG, blendedB)
}
