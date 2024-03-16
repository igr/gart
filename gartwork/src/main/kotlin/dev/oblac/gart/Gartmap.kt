package dev.oblac.gart

import dev.oblac.gart.skia.Bitmap
import dev.oblac.gart.skia.Image

/**
 * In-memory bitmap.
 */
class Gartmap(private val g: Gartvas) : Pixels {
    override val d = g.d

    override var pixelBytes: PixelBytes
    private val bitmap = g.createBitmap()

    init {
        this.pixelBytes = PixelBytes(byteArrayOf(), 0)  // initial state
        updatePixelsFromCanvas()
    }

    /**
     * Updates the bitmap from the canvas.
     * CANVAS -> BITMAP -> PIXELBYTES
     */
    fun updatePixelsFromCanvas() {
        g.surface.readPixels(bitmap, 0, 0)
        this.pixelBytes = PixelBytes(bitmap.peekPixels()!!.buffer.bytes, d.w)
    }

    /**
     * Draws bitmap to the canvas.
     * PIXELBYTES -> BITMAP -> CANVAS
     */
    fun drawToCanvas() {
        g.surface.writePixels(updateBitmapFromPixels(), 0, 0)
    }

    /**
     * Returns an updated bitmap.
     * PIXELBYTES -> BITMAP
     */
    private fun updateBitmapFromPixels(): Bitmap {
        this.bitmap.installPixels(this.pixelBytes.bytes)
        return this.bitmap
    }

    /**
     * Creates an Image from the current bitmap.
     * PIXELBYTES -> BITMAP -> IMAGE
     */
    fun image(): Image {
        return Image.makeFromBitmap(updateBitmapFromPixels().setImmutable())
    }
}

