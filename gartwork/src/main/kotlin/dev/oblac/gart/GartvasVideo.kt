package dev.oblac.gart

import dev.oblac.gart.skia.Image
import dev.oblac.gart.video.VideoRecorder
import java.io.File

/**
 * Holds a buffer for [Gartvas] snapshots, used by [VideoRecorder].
 */
class GartvasVideo(
    private val g: Gartvas,
    private val fileName: String,
    private val fps: Int = 25,
    private val dryRun: Boolean = false
) {

    private val framesCounter = FramesCounter(fps)
    val frames: Frames = framesCounter

    private var running = true
    private val queue = mutableListOf<Image>()

    fun addFrame(frame: Image) {
        queue.add(frame)
    }

    /**
     * Adds a frame to the buffer.
     */
    fun addFrame() {
        if (!running) {
            return
        }
        if (dryRun) {
            return
        }
        queue.add(g.snapshot())
        framesCounter.tick()
    }

    /**
     * Saves the buffer as a video and stops recording.
     */
    fun stopAndSaveVideo() {
        if (!running) {
            return
        }
        running = false

        if (dryRun) {
            println("Dry run, not saving video.")
            return
        }

        println("Saving video...")

        val vcr = VideoRecorder(g.d.w, g.d.h, fps)
        queue.forEachIndexed { index, it ->
            vcr.writeFrame(it.peekPixels()!!.buffer.bytes)

            val donePercentage = index / queue.size.toDouble() * 100
            print("${donePercentage.toInt()}% \r")
        }

        val videoBytes = vcr.finish()
        vcr.close()

        File(fileName).writeBytes(videoBytes)
        println("Video saved: $fileName")

        queue.clear()
    }
}
