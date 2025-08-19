package dev.oblac.gart

import dev.oblac.gart.color.*
import dev.oblac.gart.color.space.RGBA
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import kotlin.math.min

/**
 * Actual pixels memory storage.
 */
class PixelBytes(
    val bytes: ByteArray,
    private val width: Int,
    private val height: Int
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

    fun row(y: Int) = ints.slice(y * width, width).toIntArray()

    fun row(y: Int, row: IntArray) {
        require(row.size == width) { "Row size must be equal to width" }
        val yw = y * width
        for (x in 0 until width) {
            ints.put(yw + x, row[x])
        }
    }

    fun column(x: Int): IntArray {
        val column = IntArray(height)
        var index = x
        for (y in 0 until height) {
            column[y] = ints.get(index)
            index += height
        }
        return column
    }

    fun column(x: Int, column: IntArray) {
        require(column.size == height) { "Column size must be equal to height" }
        var index = x
        for (y in column.indices) {
            ints.put(index, column[y])
            index += width
        }
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
     * Sets a block of pixels to a specific color.
     */
    fun setBlock(
        x: Int, y: Int,
        pixelSize: Int,
        color: Int
    ) {
        for (dy in 0 until min(pixelSize, d.h - y)) {
            for (dx in 0 until min(pixelSize, d.w - x)) {
                this[x + dx, y + dy] = color
            }
        }
    }

    /**
     * Sets a block of pixels to a specific color.
     */
    fun setBlock(
        x: Int, y: Int,
        pixelSize: Int,
        color: RGBA
    ) = setBlock(
        x, y, pixelSize, color.value
    )

    fun calcAverageBlockColor(
        x: Int, y: Int,
        pixelSize: Int,
    ): Int {
        var totalR = 0
        var totalG = 0
        var totalB = 0
        var totalA = 0
        var count = 0

        if (pixelSize == 1) {
            return this[x, y]
        }

        for (dy in 0 until min(pixelSize, d.h - y)) {
            for (dx in 0 until min(pixelSize, d.w - x)) {
                val pixel = this[x + dx, y + dy]
                totalR += red(pixel)
                totalG += green(pixel)
                totalB += blue(pixel)
                totalA += alpha(pixel)
                count++
            }
        }
        return argb(totalA / count, totalR / count, totalG / count, totalB / count)
    }

    fun addBlockColor(
        x: Int,
        y: Int,
        pixelSize: Int,
        deltaR: Int,
        deltaG: Int,
        deltaB: Int
    ) {
        for (dy in 0 until min(pixelSize, d.h - y)) {
            for (dx in 0 until min(pixelSize, d.w - x)) {
                if (x + dx < d.w && y + dy < d.h) {
                    val currentPixel = this[x + dx, y + dy]

                    val currentR = red(currentPixel)
                    val currentG = green(currentPixel)
                    val currentB = blue(currentPixel)
                    val currentA = alpha(currentPixel)

                    val newR = (currentR + deltaR).coerceIn(0, 255)
                    val newG = (currentG + deltaG).coerceIn(0, 255)
                    val newB = (currentB + deltaB).coerceIn(0, 255)

                    this[x + dx, y + dy] = argb(currentA, newR, newG, newB)
                }
            }
        }
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

    // todo make it faster: fill the inner int buffer
    fun fill(color: Int) {
        for (j in 0 until d.h) {
            for (i in 0 until d.w) {
                set(i, j, color)
            }
        }
    }

    fun copyPixelsFrom(bitmap: Pixels) {
        bitmap.pixelBytes.bytes.copyInto(this.pixelBytes.bytes)
    }

    fun row(y: Int) = pixelBytes.row(y)
    fun row(y: Int, row: IntArray) = pixelBytes.row(y, row)
    fun column(x: Int) = pixelBytes.column(x)
    fun column(x: Int, column: IntArray) = pixelBytes.column(x, column)
}

class MemPixels(
    override val d: Dimension
) : Pixels {

    override var pixelBytes: PixelBytes = PixelBytes(ByteArray(d.area * 4), d.w, d.h)

}


fun IntBuffer.toIntArray(): IntArray {
    return if (this.hasArray()) {
        this.array()
    } else {
        val array = IntArray(this.remaining())
        this.get(array) // Copies buffer contents to array
        array
    }
}
