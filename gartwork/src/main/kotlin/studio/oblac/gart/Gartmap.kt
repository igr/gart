package studio.oblac.gart

/**
 * Canvas pixels, i.e. a bitmap
 */
class Gartmap(private val g: Gartvas) : Pixels(g.d) {
    init {
        update()
    }

    override fun update() {
        g.surface.readPixels(bitmap, 0, 0)
        super.update()
    }
    /**
     * Draws bitmap to the canvas.
     * BITMAP -> CANVAS
     */
    fun draw() {
        g.canvas.drawImage(image(), 0f, 0f)
    }
}
