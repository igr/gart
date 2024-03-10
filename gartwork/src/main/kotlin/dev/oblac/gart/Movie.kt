package dev.oblac.gart

import dev.oblac.gart.skia.Image

/**
 * A painter function that takes a [Gartvas] Image and paints it.
 */
typealias MovieFramePainter = (Image) -> Unit

class Movie(
    val g: Gartvas,
    fps: Int = 25
) {

    private val framesCounter = FramesCounter(fps)
    val frames: Frames = framesCounter

    private var running = true

    // the initial time is 1 second in the past, to kick painting right away
    private var lastPaintTimestamp = System.currentTimeMillis() - 1000

    private val painters = mutableListOf<MovieFramePainter>()

    /**
     * Defines paint callback that movie will _try_ to call at given FPS.
     * It is called after the drawing is done.
     */
    fun onPaint(painter: MovieFramePainter) {
        painters.add(painter)
    }


    private fun drawSingleFrame(drawFrame: () -> Unit) {
        val currentTimeStamp = System.currentTimeMillis()
        val elapsedSinceLastPaint = currentTimeStamp - lastPaintTimestamp
        val remainingSleepTime = frames.frameDuration.inWholeMilliseconds - elapsedSinceLastPaint

        if (remainingSleepTime < 0) {
            drawFrame()

            val frameSnapshot = g.snapshot()
            painters.forEach { it(frameSnapshot) }

            lastPaintTimestamp = currentTimeStamp
            framesCounter.tick()
        }
    }

    /**
     * Draws a frame while the movie is running, until it is explicitly closed.
     * Use [stop] to stop the movie loop.
     */
    fun draw(drawFrame: () -> Unit) {
        println("movie started")
        while (running) {
            this.drawSingleFrame(drawFrame)
        }
        println("movie stopped")
    }

    /**
     * Stops the movie.
     */
    fun stop() {
        running = false
    }

    /// RECORDING

    internal val allFrames = mutableListOf<Image>()

    private var recording = false

    /**
     * Enables storing of all the frames.
     */
    fun record() {
        if (recording) {
            return
        }
        recording = true
        println("recording in progress...")
        onPaint {
            if (recording) {
                allFrames.add(it)
            }
        }
    }

    fun stopRecording() {
        println("recording stopped")
        recording = false
    }
}
