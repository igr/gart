package dev.oblac.gart

/**
 * Canvas pixels, i.e. a bitmap.
 * Pixels do not have transparency (!) as there is no background to be transparent to.
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
