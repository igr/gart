package dev.oblac.gart.pixels

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.*

/**
 * Converts the given bitmap to grayscale by averaging the RGB components.
 */
fun makeGray(bitmap: Pixels) {
    bitmap.forEach { x, y, pixel ->
        val r = red(pixel)
        val g = green(pixel)
        val b = blue(pixel)
        val a = alpha(pixel)
        val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
        bitmap[x, y] = argb(a, gray, gray, gray)
    }
}
