package studio.oblac.gart

import studio.oblac.gart.skia.Canvas

/**
 * Generic drawable unit.
 */
fun interface Drawable {
    operator fun invoke(canvas: Canvas)
}
