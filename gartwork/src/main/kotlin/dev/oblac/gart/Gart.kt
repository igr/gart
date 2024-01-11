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
     * The animation.
     */
    val anim: Animation,
    /**
     * The window, bind to the animation but not yet started.
     */
    val window: Window,
) {

    val frames: Frames = anim.frames

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
            val anim = Animation(g, fps)
            val window = Window(anim)
            return Gart(name, d, g, anim, window)
        }
    }

}
