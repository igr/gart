package ac.obl.gart

import ac.obl.gart.gfx.toARGB
import ac.obl.gart.gfx.toRGBA
import ac.obl.gart.skia.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer

/**
 * In-memory bitmap.
 */
open class Pixels(val w: Int, val h: Int) {
	private val bitmap = Bitmap()
	private val pixels: IntBuffer

	init {
		bitmap.allocPixels(ImageInfo(w, h, ColorType.RGBA_8888, ColorAlphaType.PREMUL))
		pixels = bitmap.peekPixels()!!.asIntBuffer()
	}

	operator fun get(x: Int, y: Int): Int {
		return pixels.get(y * w + x).toARGB()
	}

	operator fun set(x: Int, y: Int, value: Int) {
		pixels.put(y * w + x, value.toRGBA())
	}
	operator fun set(x: Int, y: Int, value: Long) {
		pixels.put(y * w + x, value.toInt().toRGBA())
	}

	operator fun set(offset: Int, value: Int) {
		pixels.put(offset, value.toRGBA())
	}
	operator fun set(offset: Int, value: Long) {
		pixels.put(offset, value.toInt().toRGBA())
	}

	/**
	 * Iterates all pixels.
	 */
	fun forEach(consumer: (x: Int, y: Int, color: Int) -> Unit) {
		for (j in 0 until h) {
			for (i in 0 until w) {
				consumer(i, j, get(i, j))
			}
		}
	}

	/**
	 * Creates a new canvas from the bitmap.
	 */
	fun canvas() : Canvas {
		return Canvas(bitmap)
	}
	/**
	 * Creates a Image of the bitmap.
	 */
	fun image(): Image {
		return Image.makeFromBitmap(bitmap.setImmutable())
	}
}