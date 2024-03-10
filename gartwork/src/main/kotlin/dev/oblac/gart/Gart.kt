package dev.oblac.gart

/**
 * GART context.
 */
data class Gart(
    /**
     * The name of the Gart.
     */
    val name: String,
    /**
     * The dimension of the canvas.
     */
    val d: Dimension,
    /**
     * The canvas.
     */
    var g: Gartvas,
    /**
     * The movie.
     */
    val m: Movie,
    /**
     * The window, bound to the movie, but not yet started.
     */
    val w: Window,
) {

    val f: Frames = m.frames

    val b: Gartmap by lazy {
        Gartmap(g)
    }

    companion object {
        fun of(
            name: String,
            width: Int, height: Int,
            fps: Int = 25
        ): Gart {
            val d = Dimension(width, height)
            val g = Gartvas(d)
            val m = Movie(g, fps)
            val w = Window(m)
            return Gart(name, d, g, m, w)
        }
    }

}
