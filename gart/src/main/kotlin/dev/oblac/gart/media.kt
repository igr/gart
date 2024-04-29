package dev.oblac.gart

import dev.oblac.gart.video.VideoRecorder
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File

internal fun saveImageToFile(gartvas: Gartvas, name: String) {
    saveImageToFile(gartvas.snapshot(), name)
}

fun saveImageToFile(canvas: Canvas, d: Dimension, name: String) {
    val gartvas = Gartvas(d)
    val bitmap = gartvas.createBitmap()
    canvas.readPixels(bitmap, 0, 0)
    gartvas.writeBitmap(bitmap)
    saveImageToFile(gartvas.snapshot(), name)
}

fun saveImageToFile(image: Image, name: String) {
    image
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
