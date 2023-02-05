package studio.oblac.gart.gfx

import studio.oblac.gart.skia.Color

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

fun alpha(color: Int, a: Int): Int {
	return a and 0xFF shl 24 or (color and 0x00FFFFFF)
}

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
fun Int.toRGBA(): Int {
	return this shl 8 or (this ushr 24)
}

/**
 * Converts RGBA to ARGB.
 */
fun Int.toARGB(): Int {
	return this ushr 8 or (this shl 24)
}
