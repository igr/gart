package dev.oblac.gart

import dev.oblac.gart.gfx.convertRGBAtoARGB
import dev.oblac.gart.gfx.covertARGBtoRGBA
import dev.oblac.gart.skia.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer

/**
 * Actual memory storage.
 */
private class PixelBytes(
    val bytes: ByteArray,
    private val width: Int
) {

    private val ints: IntBuffer = ByteBuffer
        .wrap(bytes)
        .order(ByteOrder.BIG_ENDIAN)
        .asIntBuffer()

    fun get(x: Int, y: Int): Int {
        return ints.get(y * width + x).convertRGBAtoARGB()
    }

    fun set(x: Int, y: Int, value: Int) {
        ints.put(y * width + x, value.covertARGBtoRGBA())
    }

    fun set(offset: Int, value: Int) {
        ints.put(offset, value.covertARGBtoRGBA())
    }
}

/**
 * In-memory bitmap.
 */
open class Pixels(val d: Dimension) {
    protected val bitmap = Bitmap()
    private var pixelBytes: PixelBytes

    init {
        bitmap.allocPixels(ImageInfo(d.w, d.h, ColorType.RGBA_8888, ColorAlphaType.PREMUL))
        this.pixelBytes = bytes()
    }

    /**
     * Creates new pixel bytes from the internal bitmap.
     */
    private fun bytes(): PixelBytes {
        return PixelBytes(bitmap.peekPixels()!!.buffer.bytes, d.w)
    }

    /**
     * Updates the bitmap from the canvas.
     * CANVAS -> BITMAP
     */
    open fun update() {
        this.pixelBytes = bytes()
    }

    /**
     * Returns an updated bitmap.
     */
    private fun bitmap(): Bitmap {
        this.bitmap.installPixels(this.pixelBytes.bytes)
        return this.bitmap
    }

    /**
     * Creates an Image from the updated bitmap.
     */
    fun image(): Image {
        return Image.makeFromBitmap(bitmap().setImmutable())
    }

    // --- set/get

    operator fun get(x: Int, y: Int): Int {
        return pixelBytes.get(x, y)
    }

    operator fun set(x: Int, y: Int, value: Int) {
        pixelBytes.set(x, y, value)
    }

    operator fun set(x: Int, y: Int, value: Long) {
        pixelBytes.set(x, y, value.toInt())
    }

    operator fun set(offset: Int, value: Int) {
        pixelBytes.set(offset, value)
    }

    operator fun set(offset: Int, value: Long) {
        pixelBytes.set(offset, value.toInt())
    }

    /**
     * Iterates all pixels.
     */
    fun forEach(consumer: (x: Int, y: Int, color: Int) -> Unit) {
        for (j in 0 until d.h) {
            for (i in 0 until d.w) {
                consumer(i, j, get(i, j))
            }
        }
    }
}

