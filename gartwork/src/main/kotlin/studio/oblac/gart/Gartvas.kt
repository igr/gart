package studio.oblac.gart

import studio.oblac.gart.skia.Image
import studio.oblac.gart.skia.Surface

/**
 * It's the canvas.
 */
class Gartvas(val d: Dimension) {

    internal val surface = Surface.makeRasterN32Premul(d.w, d.h)

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
