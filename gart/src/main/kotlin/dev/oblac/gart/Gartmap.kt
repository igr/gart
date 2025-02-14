package dev.oblac.gart

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image

/**
 * In-memory bitmap backed by a Gartvas canvas.
 */
class Gartmap(private val g: Gartvas) : Pixels {
    override val d = g.d

    override var pixelBytes: PixelBytes
    val bitmap = g.createBitmap()

    init {
        this.pixelBytes = PixelBytes(byteArrayOf(), 0, 0)  // initial state
        updatePixelsFromCanvas()
    }

    /**
     * Updates the bitmap from the canvas.
     * CANVAS -> BITMAP -> PIXELBYTES
     */
    fun updatePixelsFromCanvas() {
        g.surface.readPixels(bitmap, 0, 0)
        this.pixelBytes = PixelBytes(bitmap.peekPixels()!!.buffer.bytes, d.w, d.h)
    }

    /**
     * Draws bitmap to the canvas.
     * PIXELBYTES -> BITMAP -> CANVAS
     */
    fun drawToCanvas(g: Gartvas = this.g) {
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

