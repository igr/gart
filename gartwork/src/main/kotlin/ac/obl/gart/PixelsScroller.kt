package ac.obl.gart

class PixelScroller(private val p: Pixels) {

	fun up(delta: Int) {
		for (y in delta until p.h) {
			for (x in 0 until p.w) {
				p[x, y - delta] = p[x, y]
			}
		}
	}
}