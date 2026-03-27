package dev.oblac.gart

import dev.oblac.gart.color.*
import dev.oblac.gart.color.space.RGBA
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

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

    /** Read one pixel, applying the requested out-of-bounds mode. */
    fun sampleNearest(
        px: Int, py: Int,
        mode: SampleMode,
        background: Int
    ): Int = when (mode) {
        SampleMode.CLAMP -> this[px.coerceIn(0, d.w - 1), py.coerceIn(0, d.h - 1)]
        SampleMode.TILE -> this[Math.floorMod(px, d.w), Math.floorMod(py, d.h)]
        SampleMode.BACKGROUND ->
            if (px in 0 until d.w && py in 0 until d.h) this[px, py] else background
    }

    /**
     * Bilinear-interpolated sample.
     * Works across all three [SampleMode]s by delegating edge handling to [sampleNearest].
     */
    fun sampleBilinear(
        fx: Double, fy: Double,
        mode: SampleMode,
        background: Int
    ): Int {
        if (!fx.isFinite() || !fy.isFinite()) return background

        // For TILE mode, reduce to [0, dimension) range in floating-point
        // to avoid int overflow when source coords are far outside the image.
        val rfx: Double
        val rfy: Double
        if (mode == SampleMode.TILE) {
            rfx = fx - floor(fx / d.w) * d.w
            rfy = fy - floor(fy / d.h) * d.h
        } else {
            rfx = fx
            rfy = fy
        }

        val x0 = floor(rfx).toInt()
        val y0 = floor(rfy).toInt()
        val tx = rfx - x0
        val ty = rfy - y0

        val c00 = sampleNearest(x0, y0, mode, background)
        val c10 = sampleNearest(x0 + 1, y0, mode, background)
        val c01 = sampleNearest(x0, y0 + 1, mode, background)
        val c11 = sampleNearest(x0 + 1, y0 + 1, mode, background)

        fun channel(shift: Int): Int {
            val v00 = (c00 shr shift) and 0xFF
            val v10 = (c10 shr shift) and 0xFF
            val v01 = (c01 shr shift) and 0xFF
            val v11 = (c11 shr shift) and 0xFF
            val top = v00 + (v10 - v00) * tx
            val bot = v01 + (v11 - v01) * tx
            return (top + (bot - top) * ty).roundToInt().coerceIn(0, 255)
        }

        return (channel(24) shl 24) or (channel(16) shl 16) or
            (channel(8) shl 8) or channel(0)
    }

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


data class Pixel(val x: Int, val y: Int)
enum class SampleMode {
    /** Clamp to image edges when src coords fall outside bounds. */
    CLAMP,

    /** Tile (repeat) the source image infinitely — great for patterns. */
    TILE,

    /** Return background color for out-of-bounds coords. */
    BACKGROUND
}
