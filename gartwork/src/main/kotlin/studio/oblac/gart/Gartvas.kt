package studio.oblac.gart

import studio.oblac.gart.skia.EncodedImageFormat
import studio.oblac.gart.skia.Image
import studio.oblac.gart.skia.Surface
import java.io.File

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

    fun forEachPoint(consumer: (x: Int, y: Int) -> Unit) {
        for (j in 0 until d.h) {
            for (i in 0 until d.w) {
                consumer(i, j)
            }
        }
    }
}
