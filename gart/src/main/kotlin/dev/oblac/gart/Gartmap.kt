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
) : Pixels, AutoCloseable {

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

    // Most recent Image handed out by image(). Skia Images are native (off-heap) objects;
    // we release the previous one when the next is requested so a render loop calling
    // image() every frame keeps at most one alive instead of leaking one per frame.
    private var lastImage: Image? = null

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
     *
     * The returned [Image] is a native (off-heap) resource **owned by this Gartmap**:
     * it stays valid until the next [image] call (or [close]), at which point it is
     * released. That lets a render loop call `image()` every frame without leaking a
     * native image per frame. Do not retain the result across frames, and do not request
     * two live images from the same Gartmap at once — copy it, or use a second Gartmap,
     * if you need it to outlive the next call.
     */
    fun image(): Image {
        lastImage?.close()      // release the previous frame's native image before minting this one
        lastImage = null
        syncBytesFromPixels()
        bitmap.installPixels(bytes)
        return Image.makeFromBitmap(bitmap.setImmutable()).also { lastImage = it }
    }

    /** Releases the cached native [image] and the backing [bitmap]. Idempotent. */
    override fun close() {
        lastImage?.close()
        lastImage = null
        bitmap.close()
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
