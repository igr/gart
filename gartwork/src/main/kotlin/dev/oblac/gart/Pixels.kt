package dev.oblac.gart

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer

/**
 * Actual pixels memory storage.
 */
class PixelBytes(
    val bytes: ByteArray,
    private val width: Int
) {

    private val ints: IntBuffer = ByteBuffer
        .wrap(bytes)
        .order(ByteOrder.LITTLE_ENDIAN)
        .asIntBuffer()

    fun get(x: Int, y: Int): Int {
        return ints.get(y * width + x)
    }

    fun set(x: Int, y: Int, value: Int) {
        ints.put(y * width + x, value)
    }

    fun set(offset: Int, value: Int) {
        ints.put(offset, value)
    }
}

/**
 * Pixels interface.
 */
interface Pixels {
    val pixelBytes: PixelBytes
    val d: Dimension

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
    fun forEach(pixelConsumer: (x: Int, y: Int, color: Int) -> Unit) {
        for (j in 0 until d.h) {
            for (i in 0 until d.w) {
                pixelConsumer(i, j, get(i, j))
            }
        }
    }
}
