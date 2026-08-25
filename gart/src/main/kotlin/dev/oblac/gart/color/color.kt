package dev.oblac.gart.color

import dev.oblac.gart.color.space.ColorOKLCH
import dev.oblac.gart.color.space.color4f
import dev.oblac.gart.color.space.of
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import org.jetbrains.skia.Color
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.Paint
import kotlin.math.floor
import kotlin.math.max
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

fun alphaf(color: Int): Float = alpha(color) / 255f
fun redf(color: Int): Float = red(color) / 255f
fun greenf(color: Int): Float = green(color) / 255f
fun bluef(color: Int): Float = blue(color) / 255f

/**
 * Relative luminance (perceived brightness) of a packed ARGB [color], in `0..255`.
 * Uses Rec. 601 luma weights. For normalized `Color4f` with Rec. 709 weights, see `Color4f.luminance`.
 */
fun lumOf(color: Int): Float = red(color) * 0.299f + green(color) * 0.587f + blue(color) * 0.114f

/**
 * Chroma of a packed ARGB [color]: the spread of its RGB channels, `max - min`, in `0..255`.
 * Greys are `0`, pure primaries `255`. This is the raw chroma that HSV/HSL saturation is
 * normalised from, so it ranks how loud a colour is without a colour-space round trip.
 */
fun chromaOf(color: Int): Int = maxOf(red(color), green(color), blue(color)) - minOf(red(color), green(color), blue(color))

/**
 * Adds [delta] to every RGB channel of a packed ARGB [color], clamping each
 * channel to `0..255` and preserving alpha.
 *
 * This changes brightness without changing hue for channels that do not clip.
 * Positive values lighten; negative values darken.
 */
fun shiftLuma(color: Int, delta: Float): Int = argb(
    alpha(color),
    (red(color) + delta).toInt().coerceIn(0, 255),
    (green(color) + delta).toInt().coerceIn(0, 255),
    (blue(color) + delta).toInt().coerceIn(0, 255),
)

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

fun String.parseColor4f() = Color4f.of(parseColor())

fun String.parseColor(): Int {
    val hex = when {
        this.startsWith("#") -> this.substring(1)
        this.startsWith("0x") || this.startsWith("0X") -> this.substring(2)
        else -> throw IllegalArgumentException("Color string must start with '#' or '0x'")
    }
    return when (hex.length) {
        6 -> { // RRGGBB
            val r = hex.substring(0, 2).toInt(16)
            val g = hex.substring(2, 4).toInt(16)
            val b = hex.substring(4, 6).toInt(16)
            rgb(r, g, b)
        }

        8 -> { // AARRGGBB
            val a = hex.substring(0, 2).toInt(16)
            val r = hex.substring(2, 4).toInt(16)
            val g = hex.substring(4, 6).toInt(16)
            val b = hex.substring(6, 8).toInt(16)
            argb(a, r, g, b)
        }

        else -> throw IllegalArgumentException("Color string must be in format #RRGGBB, #AARRGGBB, 0xRRGGBB, or 0xAARRGGBB")
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

/**
 * Samples [colors] as one continuous ramp: [t] runs `0f..1f` across the whole array and the result
 * is interpolated between the two entries it falls between, so walking [t] gives a smooth gradient
 * rather than a staircase. [t] is clamped, so the ends hold.
 *
 * This is the continuous counterpart to indexing: [Palette.bound] and friends snap to a slot,
 * this one blends. See [Palette.sample] for the same thing on a [Palette].
 */
fun lerpColors(colors: IntArray, t: Float): Int {
    require(colors.isNotEmpty()) { "colors must not be empty" }
    if (colors.size == 1) return colors[0]
    val x = t.coerceIn(0f, 1f) * (colors.size - 1)
    val i = floor(x).toInt().coerceAtMost(colors.size - 2)
    return lerpColor(colors[i], colors[i + 1], x - i)
}

/**
 * Darkens [color] by mixing it toward black. [f] is the mix amount:
 * `f = 0f` returns the colour unchanged, `f = 1f` returns black.
 */
fun darken(color: Int, f: Float): Int = lerpColor(color, Color.BLACK, f)

/**
 * Lightens [color] by mixing it toward white. [f] is the mix amount:
 * `f = 0f` returns the colour unchanged, `f = 1f` returns white.
 */
fun lighten(color: Int, f: Float): Int = lerpColor(color, Color.WHITE, f)

/**
 * Scales each RGB channel of [color] by [s] (clamped to `0..255`), forcing the alpha opaque.
 * A multiplicative brightness: `s < 1f` darkens, `s > 1f` brightens — unlike [darken]/[lighten], which lerp toward black/white.
 */
fun colorScale(color: Int, s: Float): Int {
    val r = (red(color) * s).toInt().coerceIn(0, 255)
    val g = (green(color) * s).toInt().coerceIn(0, 255)
    val b = (blue(color) * s).toInt().coerceIn(0, 255)
    return rgb(r, g, b)
}

/**
 * Brightens [color] multiplicatively like [colorScale], but never past the point where a
 * channel clips: [s] is capped at the headroom of the brightest channel.
 *
 * Every other way of brightening in here loses the colour when it is already near the top.
 * [lighten] mixes toward white, so it desaturates by construction. [shiftLuma] adds a
 * constant, which keeps the hue but flattens saturation. [colorScale] keeps both — right up
 * until it clips, and once all three channels pin at `255` the result is neutral white with
 * the hue gone. Capping at the headroom keeps hue and saturation exact at every value of
 * [s]; already-bright colours simply receive less lift.
 *
 * `s <= 1f` darkens and is passed straight through to [colorScale]. Alpha is forced opaque.
 */
/**
 * Rotates the hue of [color] by [deg] degrees in OKLCH, holding lightness and chroma fixed,
 * and returns it opaque.
 *
 * OKLCH is perceptual, so this turns a whole palette together without the lightness swings
 * an HSL rotation produces — which is what makes it usable as a single "turn the piece"
 * knob. `deg = 0f` returns [color] untouched.
 */
fun hueShift(color: Int, deg: Float): Int {
    if (deg == 0f) return color
    val o = ColorOKLCH.of(color.color4f())
    return ColorOKLCH(o.l, o.c, o.h + deg).toColor4f().toColor() or 0xFF000000.toInt()
}

fun colorLift(color: Int, s: Float): Int {
    if (s <= 1f) return colorScale(color, s)
    val m = max(max(red(color), green(color)), blue(color))
    if (m == 0) return color
    return colorScale(color, min(s, 255f / m))
}
