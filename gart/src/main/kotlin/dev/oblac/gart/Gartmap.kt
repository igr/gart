package dev.oblac.gart

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer

/**
 * In-memory pixel buffer that can optionally bridge to a Skia [Gartvas].
 *
 * Storage is an [IntArray] (`pixels`, ARGB, indexed `y * w + x`). Per-pixel
 * `[x, y]` access goes straight to the array — no `IntBuffer` view, no
 * round-trip to a Skia bitmap on every read/write.
 *
 * Two construction modes:
 *  - `Gartmap(d)`        — pure in-memory; no canvas binding. Useful as an
 *    accumulation buffer or for procedural pixel generation.
 *  - `Gartmap(gartvas)`  — bound to a canvas. Auto-pulls pixels from the
 *    canvas at construction; subsequent [updatePixelsFromCanvas] / [drawToCanvas]
 *    calls sync the two.
 *
 * The byte buffer used for Skia round-trips is allocated once at construction
 * and reused on every pull/push.
 */
class Gartmap private constructor(
    override val d: Dimension,
    private val gartvas: Gartvas?,
) : Pixels {

    /** In-memory only — no Skia surface binding. */
    constructor(d: Dimension) : this(d, null)

    /** Bound to [gartvas]; pulls the canvas pixels into [pixels] eagerly. */
    constructor(gartvas: Gartvas) : this(gartvas.d, gartvas) {
        updatePixelsFromCanvas()
    }

    override val pixels: IntArray = IntArray(d.area)

    val w = d.w
    val wf = d.wf
    val h = d.h
    val hf = d.hf

    private val bytes: ByteArray = ByteArray(d.area * 4)
    private val intView: IntBuffer = ByteBuffer.wrap(bytes)
        .order(ByteOrder.LITTLE_ENDIAN)
        .asIntBuffer()

    val bitmap: Bitmap = gartvas?.createBitmap() ?: createInMemBitmap(d)

    /**
     * Pulls pixels from the bound canvas into [pixels].
     * Throws if this Gartmap was created without a Gartvas.
     */
    fun updatePixelsFromCanvas() {
        val gv = gartvas ?: error("Gartmap: no Gartvas bound — created with Gartmap(d)")
        gv.surface.readPixels(bitmap, 0, 0)
        // Copy bitmap's bytes (Skia-managed) into our IntArray via a one-time view.
        val skBytes = bitmap.peekPixels()!!.buffer.bytes
        ByteBuffer.wrap(skBytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asIntBuffer()
            .get(pixels)
    }

    /**
     * Pushes [pixels] back to a Gartvas. Defaults to the bound canvas; pass an
     * explicit [target] when this Gartmap was created in-memory or you want to
     * compose into a different canvas.
     */
    fun drawToCanvas(target: Gartvas? = gartvas) {
        val gv = target ?: error("Gartmap: no Gartvas bound — pass an explicit target")
        syncBytesFromPixels()
        bitmap.installPixels(bytes)
        gv.surface.writePixels(bitmap, 0, 0)
    }

    /**
     * Returns a snapshot Skia [Image] of the current [pixels]. Calling this
     * does not require a Gartvas binding; it's a useful path for in-memory
     * Gartmaps that want to be drawn onto another canvas via [Canvas.drawImage].
     */
    fun image(): Image {
        syncBytesFromPixels()
        bitmap.installPixels(bytes)
        return Image.makeFromBitmap(bitmap.setImmutable())
    }

    private fun syncBytesFromPixels() {
        intView.position(0)
        intView.put(pixels)
    }
}

private fun createInMemBitmap(d: Dimension): Bitmap {
    val bmp = Bitmap()
    bmp.setImageInfo(ImageInfo.makeN32(d.w, d.h, ColorAlphaType.PREMUL))
    bmp.allocPixels()
    return bmp
}
