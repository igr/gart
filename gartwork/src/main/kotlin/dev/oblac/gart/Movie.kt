package dev.oblac.gart

import dev.oblac.gart.skia.Canvas
import dev.oblac.gart.skia.Image

/**
 * Movie is just a simple buffer of snapshot images.
 */
class Movie(val d: Dimension, private val name: String) {
    private val allFrames = mutableListOf<Image>()
    private val gartvas = Gartvas(d)
    private val bitmap = gartvas.createBitmap()

    fun addFrame(gartvas: Gartvas) {
        allFrames.add(gartvas.snapshot())
    }

    fun addFrame(canvas: Canvas) {
        canvas.readPixels(bitmap, 0, 0)
        gartvas.writeBitmap(bitmap)
        addFrame(gartvas)
    }

    fun forEachFrame(consumer: (Int, Image) -> Unit) = allFrames.forEachIndexed(consumer)

    fun totalFrames() = allFrames.size

    /**
     * Decorates window with movie recording.
     */
    fun record(window: Window): Window {
        return object : Window(d) {
            override fun show(drawFrame: DrawFrame) {
                super.show { c, d, f ->
                    drawFrame(c, d, f)
                    if (f.new) {
                        addFrame(c)
                    }
                }
            }

            override fun onClose() {
                super.onClose()
                saveMovieToFile(this@Movie, window.fps, name)
            }
        }
    }

}
