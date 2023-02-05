package ac.obl.gart

import ac.obl.gart.skia.Image
import ac.obl.gart.skia.Surface

/**
 * It's the canvas.
 */
class Gartvas(val box: Box) {

    internal val surface = Surface.makeRasterN32Premul(box.w, box.h)

    /**
     * Canvas.
     */
    val canvas = surface.canvas

    /**
     * Makes a snapshot of a canvas.
     */
    fun snapshot(): Image {
        return surface.makeImageSnapshot()
    }
}
