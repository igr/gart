package ac.obl.gart

import ac.obl.gart.skia.Canvas

/**
 * Generic drawable unit.
 */
interface Shape {
    fun draw(canvas: Canvas)
}
