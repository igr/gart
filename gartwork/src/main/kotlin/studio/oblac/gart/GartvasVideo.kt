package studio.oblac.gart

import studio.oblac.gart.skia.Image
import studio.oblac.gart.video.VideoRecorder
import java.io.File

/**
 * Holds a buffer for [Gartvas] snapshots, used by [VideoRecorder].
 */
class GartvasVideo(
    private val g: Gartvas,
    private val fileName: String,
    private val fps: Int = 25) {

    private val framesCounter = FramesCounter(fps)
    val frames: Frames = framesCounter

    private var running = true
    private val queue = mutableListOf<Image>()

    /**
     * Adds a frame to the buffer.
     */
    fun addFrame() {
        if (!running) {
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
