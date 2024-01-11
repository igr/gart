package dev.oblac.gart

import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.skia.EncodedImageFormat
import dev.oblac.gart.skia.Image
import dev.oblac.gart.skia.Rect
import dev.oblac.gart.skia.Surface
import java.io.File

/**
 * It's the canvas.
 */
class Gartvas(val d: Dimension) {

    internal val surface = Surface.makeRasterN32Premul(d.w, d.h)
    val rect = Rect(0f, 0f, d.wf, d.hf)

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

    /**
     * Creates snapshot and writes it as an image file.
     */
    fun writeSnapshotAsImage(name: String) {
        snapshot()
            .encodeToData(EncodedImageFormat.valueOf(name.substringAfterLast('.').uppercase()))
            .let { it!!.bytes }
            .also {
                File(name).writeBytes(it)
                println("Image saved: $name")
            }
    }

    /**
     * Draw one Gartvas onto another.
     */
    fun draw(g: Gartvas, x: Float, y: Float) {
        canvas.drawImage(g.snapshot(), x, y)
    }

    fun fill(color: Int) {
        canvas.drawRect(rect, fillOf(color))
    }

}
