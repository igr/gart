package dev.oblac.gart

import org.jetbrains.skia.*

/**
 * In-memory, slow canvas.
 */
class Gartvas(val d: Dimension) {
    internal val surface = Surface.makeRasterN32Premul(d.w, d.h)

    /**
     * Canvas.
     */
    val canvas = surface.canvas

    /**
     * Makes a snapshot Image of a canvas.
     */
    fun snapshot(): Image {
        return surface.makeImageSnapshot()
    }

    /**
     * Draw on this canvas.
     */
    fun draw(draw: Draw) = draw(canvas, d)

    /**
     * Creates a bitmap compatible with the canvas.
     */
    fun createBitmap(): Bitmap {
        val bitmap = Bitmap()
        bitmap.setImageInfo(ImageInfo.makeN32Premul(d.w, d.h))
        bitmap.allocPixels()
        return bitmap
    }

    fun writeBitmap(bitmap: Bitmap) {
        surface.writePixels(bitmap, 0, 0)
    }

    /**
     * Copies the content of this canvas to another canvas.
     */
    fun snapshotTo(c: Canvas) {
        c.drawImage(snapshot(), 0f, 0f)
    }

    /**
     * Creates a sprite image from this canvas.
     */
    fun sprite(): Sprite = Sprite.of(this)

    companion object {
        fun of(width: Number, height: Number) =
            Gartvas(Dimension(width.toInt(), height.toInt()))
    }

}
