package dev.oblac.gart

import dev.oblac.gart.skia.Image

/**
 * A painter function that takes a [Gartvas] Image and paints it.
 */
typealias AnimationPainter = (Image) -> Unit

class Animation(
    val g: Gartvas,
    fps: Int = 25,
) {

    private val framesCounter = FramesCounter(fps)
    val frames: Frames = framesCounter

    // the initial time is 1 second in the past, to kick painting right away
    private var lastPaintTimestamp = System.currentTimeMillis() - 1000

    /**
     * Defines paint callback that animation will _try_ to call at given FPS.
     * It is called after the drawing is done.
     */
    fun start(paintCallback: AnimationPainter): AnimationRunner {
        return AnimationRunner(paintCallback)
    }

    fun stop() {
    }

    inner class AnimationRunner(private val paintCallback: AnimationPainter) {
        private var running = true

        private fun drawSingleFrame(drawFrame: (Frames) -> Boolean) {
            val currentTimeStamp = System.currentTimeMillis()
            val elapsedSinceLastPaint = currentTimeStamp - lastPaintTimestamp
            val remainingSleepTime = frames.frameDuration.inWholeMilliseconds - elapsedSinceLastPaint

            if (remainingSleepTime < 0) {
                running = drawFrame(frames)
                paintCallback(g.snapshot())
                lastPaintTimestamp = currentTimeStamp
                framesCounter.tick()
            }
        }


        /**
         * Paints a frame while the window is up and return value is true, until the window is explicitly closed.
         */
        fun drawWhile(drawFrame: (Frames) -> Boolean) {
            while (running) {
                this.drawSingleFrame(drawFrame)
            }
        }

        fun drawWhile(condition: () -> Boolean, drawFrame: (Frames) -> Unit) {
            while (running) {
                this.drawSingleFrame {
                    drawFrame(it)
                    return@drawSingleFrame condition()
                }
            }
        }

        /**
         * Draws a frame while animation is running, until it is explicitly closed.
         */
        fun draw(drawFrame: (Frames) -> Unit) {     // todo have only one single function should be enough
            while (running) {
                this.drawSingleFrame {
                    drawFrame(it)
                    return@drawSingleFrame true
                }
            }
        }

        fun stop() {
            running = false
        }
    }
}
