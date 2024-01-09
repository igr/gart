package dev.oblac.gart

import dev.oblac.gart.skia.Canvas

/**
 * Drawable canvas consumer function interface.
 */
fun interface Drawable {
    operator fun invoke(canvas: Canvas)
}
