package ac.obl.gart

/**
 * Canvas pixels, i.e. a bitmap
 */
class Gartmap(private val g: Gartvas) : Pixels(g.w, g.h) {

	init {
		canvas().drawImage(g.snapshot(), 0f, 0f)

		// alternative
//		val bb = g.snapshot().peekPixels()!!.asIntBuffer()
//		for (i in 0 until g.wh) {
//			pixels.put(i, bb[i])
//		}
	}

	/**
	 * Draws bitmap to the canvas
	 */
	fun draw() {
		g.canvas.drawImage(this.image(), 0f, 0f)
	}
}