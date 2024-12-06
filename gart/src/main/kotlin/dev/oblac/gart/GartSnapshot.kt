package dev.oblac.gart

import org.jetbrains.skia.Canvas

/**
 * A snapshot of the current state of the Gart.
 * Used in the view loops, to capture the current state of the Gart.
 */
class GartSnapshot(private val gart: Gart) {
    private var g: Gartvas? = null

    /**
     * Freezes the current state of the Gart as snapshot.
     */
    fun freeze(targetCanvas: Canvas) {
        val g = gart.gartvas()  // internal gartvas
        this.g = g

        val bitmap = g.createBitmap()
        targetCanvas.readPixels(bitmap, 0, 0)
        g.writeBitmap(bitmap)
    }

    /**
     * Draws the captured snapshot to the given canvas.
     */
    fun draw(targetCanvas: Canvas) {
        g?.snapshotTo(targetCanvas)
    }

    /**
     * The internal Gartvas containing the snapshot.
     * If called before the snapshot is drawn, it will be empty.
     */
    fun get() = g

    /**
     * Returns true if the snapshot is captured.
     */
    fun isCaptured() = g != null

    /**
     * Saves the snapshot as an image.
     */
    fun saveImage() {
        gart.saveImage(g!!)
    }
}
