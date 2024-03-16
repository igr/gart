package dev.oblac.gart

/**
 * GART.
 */
data class Gart(
    /**
     * The name of the Gart.
     */
    val name: String,
) {

    fun gartvas(dimension: Dimension) = Gartvas(dimension)

    fun gartmap(gartvas: Gartvas) = Gartmap(gartvas)

    fun dimension(width: Int, height: Int) = Dimension(width, height)

    fun window(d: Dimension, fps: Int = 60, printFps: Boolean = true) = Window(d, fps, printFps)

    fun movie(d: Dimension, name: String = "${this.name}.mp4") = Movie(d, name)

    fun saveImage(gartvas: Gartvas, name: String = "${this.name}.png") = saveImageToFile(gartvas, name)

    fun saveMovie(movie: Movie, fps: Int, name: String = "${this.name}.mp4") = saveMovieToFile(movie, fps, name)

    fun showImage(gartvas: Gartvas) {
        window(gartvas.d).show { c, _, _ ->
            c.drawImage(gartvas.snapshot(), 0f, 0f)
        }
    }

}
