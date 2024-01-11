package dev.oblac.gart

import dev.oblac.gart.skia.EncodedImageFormat
import dev.oblac.gart.video.VideoRecorder
import java.io.File

object Media {

    fun saveImage(gart: Gart, name: String = "${gart.name}.png") {
        gart.g.snapshot()
            .encodeToData(EncodedImageFormat.valueOf(name.substringAfterLast('.').uppercase()))
            .let { it!!.bytes }
            .also {
                File(name).writeBytes(it)
                println("Image saved: $name")
            }
    }

    fun saveVideo(gart: Gart, name: String = "${gart.name}.mp4", fps: Int = gart.f.fps) {
        val vcr = VideoRecorder(gart.d.w, gart.d.h, fps)

        with(gart.a) {
            val size = allFrames.size

            allFrames.forEachIndexed { index, it ->
                vcr.writeFrame(it.peekPixels()!!.buffer.bytes)
                val donePercentage = index / size.toDouble() * 100
                print("${donePercentage.toInt()}% \r")
            }
        }
        val videoBytes = vcr.finish()
        vcr.close()

        File(name).writeBytes(videoBytes)
        println("Video saved: $name")
    }
}
