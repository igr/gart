package ac.obl.gart

import ac.obl.gart.skia.Image
import ac.obl.gart.skia.Surface

/**
 * It's the canvas.
 */
class Gartvas(val w: Int, val h: Int) {

	/**
	 * Canvas area.
	 */
	val area = w * h

	/**
	 * Width as float.
	 */
	val wf = w.toFloat()

	/**
	 * Height as float.
	 */
	val hf = h.toFloat()

	/**
	 * Right edge.
	 */
	val r = w - 1

	val rf = r.toFloat()

	/**
	 * Bottom edge.
	 */
	val b = h - 1

	val bf = b.toFloat()

	private val surface = Surface.makeRasterN32Premul(w, h)

	/**
	 * Canvas.
	 */
	val canvas = surface.canvas

	/**
	 * Makes a snapshot of a canvas.
	 */
	fun snapshot(): Image {
		return surface.makeImageSnapshot()
	}
}
