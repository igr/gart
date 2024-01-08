package dev.oblac.gart

import dev.oblac.gart.skia.Canvas

/**
 * Generic drawable unit.
 */
fun interface Drawable {
    operator fun invoke(canvas: Canvas)
}
