package ac.obl.gart

import io.github.humbleui.skija.*

/**
 * It's the canvas.
 */
class Gartvas(val w: Int, val h: Int) {

	/**
	 * Canvas area.
	 */
	val wh = w * h

	/**
	 * Width as float.
	 */
	val wf = w.toFloat()

	/**
	 * Height as float.
	 */
	val hf = h.toFloat()

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