package dev.oblac.gart

import dev.oblac.gart.color.*
import dev.oblac.gart.color.space.RGBA
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pixels - single source of truth is an [IntArray] of ARGB-packed pixels,
 * indexed `y * d.w + x`. Direct array access is several times faster than
 * the previous `IntBuffer`-view-over-`ByteArray` design and avoids the
 * round-trip allocations that came with it.
 *
 * Implementations:
 *  - [MemPixels] : pure in-memory buffer
 *  - [Gartmap]   : bridges to a [Gartvas] (Skia surface) via pull/push
 */
interface Pixels {
    val pixels: IntArray
    val d: Dimension

    operator fun get(x: Int, y: Int): Int = pixels[y * d.w + x]

    operator fun set(x: Int, y: Int, value: Int) {
        pixels[y * d.w + x] = value
    }

    operator fun set(x: Int, y: Int, value: Long) {
        pixels[y * d.w + x] = value.toInt()
    }

    operator fun set(offset: Int, value: Int) {
        pixels[offset] = value
    }

    operator fun set(offset: Int, value: Long) {
        pixels[offset] = value.toInt()
    }

    /** Sets a `pixelSize × pixelSize` block to [color]; clipped at image edges. */
    fun setBlock(x: Int, y: Int, pixelSize: Int, color: Int) {
        val w = d.w
        val rows = min(pixelSize, d.h - y)
        val cols = min(pixelSize, w - x)
        for (dy in 0 until rows) {
            val rowStart = (y + dy) * w + x
            for (dx in 0 until cols) {
                pixels[rowStart + dx] = color
            }
        }
    }

    fun setBlock(x: Int, y: Int, pixelSize: Int, color: RGBA) =
        setBlock(x, y, pixelSize, color.value)

    fun calcAverageBlockColor(x: Int, y: Int, pixelSize: Int): Int {
        if (pixelSize == 1) return this[x, y]
        val w = d.w
        var totalR = 0;
        var totalG = 0;
        var totalB = 0;
        var totalA = 0
        var count = 0
        val rows = min(pixelSize, d.h - y)
        val cols = min(pixelSize, w - x)
        for (dy in 0 until rows) {
            val rowStart = (y + dy) * w + x
            for (dx in 0 until cols) {
                val pixel = pixels[rowStart + dx]
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
        x: Int, y: Int, pixelSize: Int,
        deltaR: Int, deltaG: Int, deltaB: Int,
    ) {
        val w = d.w
        val rows = min(pixelSize, d.h - y)
        val cols = min(pixelSize, w - x)
        for (dy in 0 until rows) {
            val rowStart = (y + dy) * w + x
            for (dx in 0 until cols) {
                val cur = pixels[rowStart + dx]
                val newR = (red(cur) + deltaR).coerceIn(0, 255)
                val newG = (green(cur) + deltaG).coerceIn(0, 255)
                val newB = (blue(cur) + deltaB).coerceIn(0, 255)
                pixels[rowStart + dx] = argb(alpha(cur), newR, newG, newB)
            }
        }
    }

    /** Sets every pixel to [color] in one bulk operation. */
    fun fill(color: Int) {
        java.util.Arrays.fill(pixels, color)
    }

    /** Replaces this buffer's contents with [other]'s. Sizes must match. */
    fun copyPixelsFrom(other: Pixels) {
        other.pixels.copyInto(this.pixels)
    }

    fun row(y: Int): IntArray {
        val w = d.w
        val out = IntArray(w)
        System.arraycopy(pixels, y * w, out, 0, w)
        return out
    }

    fun row(y: Int, row: IntArray) {
        require(row.size == d.w) { "Row size must be equal to width" }
        System.arraycopy(row, 0, pixels, y * d.w, d.w)
    }

    fun column(x: Int): IntArray {
        val out = IntArray(d.h)
        var idx = x
        for (y in 0 until d.h) {
            out[y] = pixels[idx]
            idx += d.w
        }
        return out
    }

    fun column(x: Int, column: IntArray) {
        require(column.size == d.h) { "Column size must be equal to height" }
        var idx = x
        for (y in 0 until d.h) {
            pixels[idx] = column[y]
            idx += d.w
        }
    }

    /** Read one pixel, applying the requested out-of-bounds mode. */
    fun sampleNearest(
        px: Int, py: Int,
        mode: SampleMode,
        background: Int,
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
        background: Int,
    ): Int {
        if (!fx.isFinite() || !fy.isFinite()) return background

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

/**
 * Iterates every pixel row-major, calling [block] with `(x, y, color)`.
 *
 * Inline extension function so the loop body is inlined directly into the call
 * site, avoiding lambda allocation and per-call interface dispatch — measured
 * ~13× faster than the old non-inline interface default.
 */
inline fun Pixels.forEach(block: (x: Int, y: Int, color: Int) -> Unit) {
    val w = d.w
    val h = d.h
    var idx = 0
    for (y in 0 until h) {
        for (x in 0 until w) {
            block(x, y, pixels[idx])
            idx += 1
        }
    }
}

/** Pure in-memory pixel buffer — no Skia binding. */
class MemPixels(override val d: Dimension) : Pixels {
    override val pixels: IntArray = IntArray(d.area)
}

data class Pixel(val x: Int, val y: Int)

enum class SampleMode {
    /** Clamp to image edges when src coords fall outside bounds. */
    CLAMP,

    /** Tile (repeat) the source image infinitely — great for patterns. */
    TILE,

    /** Return background color for out-of-bounds coords. */
    BACKGROUND,
}
