package dev.oblac.gart

import dev.oblac.gart.gif.GifSequenceWriter
import dev.oblac.gart.util.toBufferedImage
import dev.oblac.gart.video.VideoRecorder
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import javax.imageio.stream.FileImageOutputStream

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

enum class MovieFormat {
    MP4, GIF
}

internal fun saveMovieToFile(movie: Movie, fps: Int, name: String) {
    when (movie.format) {
        MovieFormat.MP4 -> saveMp4ToFile(movie, fps, name)
        MovieFormat.GIF -> saveGifToFile(movie, fps, name)
    }
}

private fun saveGifToFile(movie: Movie, fps: Int, name: String) {
    val size = movie.totalFrames()

    val out = FileImageOutputStream(File(name))

    val gif = GifSequenceWriter(out, java.awt.image.BufferedImage.TYPE_INT_ARGB, 1000 / fps, true)
    movie.forEachFrame { index, it ->
        gif.writeToSequence(it.toBufferedImage())
        val donePercentage = index / size.toDouble() * 100
        print("${donePercentage.toInt()}% \r")
    }
    gif.close()
    println("Gif saved: $name")
}

private fun saveMp4ToFile(movie: Movie, fps: Int, name: String) {
    val size = movie.totalFrames()

    val vcr = VideoRecorder(movie.d.w, movie.d.h, fps)
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

