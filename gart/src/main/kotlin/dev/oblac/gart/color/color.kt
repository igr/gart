package dev.oblac.gart.color

import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import org.jetbrains.skia.Color
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.Paint
import kotlin.math.min

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

fun argb(af: Float, rf: Float, gf: Float, bf: Float): Int {
    val r = (rf * 255).toInt().coerceIn(0, 255)
    val g = (gf * 255).toInt().coerceIn(0, 255)
    val b = (bf * 255).toInt().coerceIn(0, 255)
    val a = (af * 255).toInt().coerceIn(0, 255)
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

/**
 * Blends two colors considering their alpha channels.
 * The 'front' color is drawn over the 'back' color.
 * Integer only arithmetic for performance.
 * Porter-Duff SRC_OVER.
 */
fun blendColors(front: Int, back: Int): Int {
    val af = alpha(front)
    val ab = alpha(back)
    val aOut = af + (ab * (255 - af) + 127) / 255  // rounded

    if (aOut == 0) return argb(0, 0, 0, 0)

    val rOut = (
        red(front) * af * 255 +
            red(back) * ab * (255 - af) +
            aOut / 2
        ) / (aOut * 255)

    val gOut = (
        green(front) * af * 255 +
            green(back) * ab * (255 - af) +
            aOut / 2
        ) / (aOut * 255)

    val bOut = (
        blue(front) * af * 255 +
            blue(back) * ab * (255 - af) +
            aOut / 2
        ) / (aOut * 255)

    return argb(aOut, rOut, gOut, bOut)
}


fun blendDarken(existingColor: Int, newColor: Int): Int {
    val existingR = red(existingColor)
    val existingG = green(existingColor)
    val existingB = blue(existingColor)
    val existingA = alpha(existingColor)

    val newR = red(newColor)
    val newG = green(newColor)
    val newB = blue(newColor)
    val newA = alpha(newColor)

    // Darken blend mode: take minimum of each channel
    val blendedR = min(existingR, newR)
    val blendedG = min(existingG, newG)
    val blendedB = min(existingB, newB)

    // Alpha compositing
    val blendedA = existingA + newA * (255 - existingA) / 255

    return argb(blendedA, blendedR, blendedG, blendedB)
}

fun String.parseColor(): Int {
    if (!this.startsWith("#")) {
        throw IllegalArgumentException("Color string must start with '#'")
    }
    return when (this.length) {
        7 -> { // #RRGGBB
            val r = this.substring(1, 3).toInt(16)
            val g = this.substring(3, 5).toInt(16)
            val b = this.substring(5, 7).toInt(16)
            rgb(r, g, b)
        }

        9 -> { // #AARRGGBB
            val a = this.substring(1, 3).toInt(16)
            val r = this.substring(3, 5).toInt(16)
            val g = this.substring(5, 7).toInt(16)
            val b = this.substring(7, 9).toInt(16)
            argb(a, r, g, b)
        }

        else -> throw IllegalArgumentException("Color string must be in format #RRGGBB or #AARRGGBB")
    }
}

fun colorDistance(c1: Int, c2: Int): Int {
    val dr = red(c1) - red(c2)
    val dg = green(c1) - green(c2)
    val db = blue(c1) - blue(c2)
    return maxOf(kotlin.math.abs(dr), kotlin.math.abs(dg), kotlin.math.abs(db))
}

fun lerpColor(from: Int, to: Int, t: Float): Int {
    val t1 = t.coerceIn(0f, 1f)
    val t0 = 1f - t1
    return argb(
        (alpha(from) * t0 + alpha(to) * t1).toInt(),
        (red(from) * t0 + red(to) * t1).toInt(),
        (green(from) * t0 + green(to) * t1).toInt(),
        (blue(from) * t0 + blue(to) * t1).toInt()
    )
}
