package dev.oblac.gart

import dev.oblac.gart.skia.Canvas
import dev.oblac.gart.skia.Image

/**
 * Movie is just a simple buffer of snapshot images.
 */
class Movie(val d: Dimension, private val name: String, startRecording: Boolean = true) {
    private val allFrames = mutableListOf<Image>()
    private val gartvas = Gartvas(d)
    private val bitmap = gartvas.createBitmap()

    private var recording = startRecording

    fun addFrame(gartvas: Gartvas) {
        if (!recording) return
        allFrames.add(gartvas.snapshot())
    }

    fun addFrame(canvas: Canvas) {
        if (!recording) return
        canvas.readPixels(bitmap, 0, 0)
        gartvas.writeBitmap(bitmap)
        addFrame(gartvas)
    }

    fun startRecording() {
        if (!recording) {
            println("Recording started")
        }
        recording = true
    }

    fun stopRecording() {
        if (recording) {
            println("Recording stopped")
        }
        recording = false
    }

    fun forEachFrame(consumer: (Int, Image) -> Unit) = allFrames.forEachIndexed(consumer)

    fun totalFrames() = allFrames.size

    /**
     * Decorates window with movie recording.
     */
    fun record(window: Window, recording: Boolean = this.recording): Window {
        this.recording = recording
        return object : Window(d, window.fps, window.printFps) {
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
