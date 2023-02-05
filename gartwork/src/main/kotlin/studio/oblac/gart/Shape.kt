package studio.oblac.gart

import studio.oblac.gart.skia.Canvas

/**
 * Generic drawable unit.
 */
interface Shape {
    fun draw(canvas: Canvas)
}
