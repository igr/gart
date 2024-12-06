package dev.oblac.gart

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image

/**
 * GART.
 */
data class Gart(
    /**
     * The name of the Gart.
     */
    val name: String,
    /**
     * The default dimension of the Gart.
     */
    val d: Dimension,
    /**
     * The default frames per second of the Gart.
     */
    val fps: Int,
) {

    fun gg() = GartGG(this, gartvas())

    fun gartvas(dimension: Dimension = this.d) = Gartvas(dimension)

    fun gartmap(gartvas: Gartvas) = Gartmap(gartvas)

    fun dimension(width: Int, height: Int) = Dimension(width, height)

    fun window(d: Dimension = this.d, fps: Int = this.fps, printFps: Boolean = true) = Window(d, fps, printFps)

    fun movie(d: Dimension = this.d, name: String = "${this.name}.mp4") = Movie(d, name)

    fun movieGif(d: Dimension = this.d, name: String = "${this.name}.gif") = Movie(d, name, format = MovieFormat.GIF)

    fun saveImage(gartvas: Gartvas, name: String = "${this.name}.png") = saveImageToFile(gartvas, name)

    fun saveImage(canvas: Canvas, d: Dimension = this.d, name: String = "${this.name}.png") = saveImageToFile(canvas, d, name)

    fun saveImage(image: Image, name: String = "${this.name}.png") = saveImageToFile(image, name)

    fun saveMovie(movie: Movie, fps: Int = this.fps, name: String = this.name) = saveMovieToFile(movie, fps, name)

    fun snapshot() = GartSnapshot(this)

    override fun toString(): String {
        return "gȧrt! • $name"
    }

    companion object {
        fun of(name: String, width: Number, height: Number, fps: Int = 60) = Gart(name, Dimension(width.toInt(), height.toInt()), fps)
        fun of(name: String, d: Dimension, fps: Int = 60) = Gart(name, d, fps)
    }
}
