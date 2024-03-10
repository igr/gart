package dev.oblac.gart

import dev.oblac.gart.skia.Canvas

/**
 * Drawable canvas consumer function interface.
 * todo remove, there is one in Skia
 */
fun interface Drawable {
    operator fun invoke(canvas: Canvas)
}
