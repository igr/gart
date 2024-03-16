package dev.oblac.gart

import dev.oblac.gart.skia.EncodedImageFormat
import dev.oblac.gart.video.VideoRecorder
import java.io.File

internal fun saveImageToFile(gartvas: Gartvas, name: String) {
    gartvas.snapshot()
        .encodeToData(EncodedImageFormat.valueOf(name.substringAfterLast('.').uppercase()))
        .let { it!!.bytes }
        .also {
            File(name).writeBytes(it)
            println("Image saved: $name")
        }
}

internal fun saveMovieToFile(movie: Movie, fps: Int, name: String) {
    val vcr = VideoRecorder(movie.d.w, movie.d.h, fps)

    val size = movie.totalFrames()

    movie.forEachFrame { index, it ->
        vcr.writeFrame(it.peekPixels()!!.buffer.bytes)
        val donePercentage = index / size.toDouble() * 100
        print("${donePercentage.toInt()}% \r")
    }

    val videoBytes = vcr.finish()
    vcr.close()

    File(name).writeBytes(videoBytes)
    println("Video saved: $name")
}
