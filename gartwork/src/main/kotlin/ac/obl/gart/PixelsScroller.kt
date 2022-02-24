package ac.obl.gart

class PixelScroller(private val p: Pixels) {

	fun up(delta: Int) {
		for (y in delta until p.box.h) {
			for (x in 0 until p.box.w) {
				p[x, y - delta] = p[x, y]
			}
		}
	}
}
