package studio.oblac.gart.skyscraper

import studio.oblac.gart.gfx.RectIsometric
import studio.oblac.gart.gfx.RectIsometricLeft
import studio.oblac.gart.math.toRadian
import studio.oblac.gart.skia.Point
import kotlin.math.cos
import kotlin.math.sin

class WindowsOne(
	private val left: Point,
	private val right: Point,
	private val windowSize: Float,
	private val gap: Float,
	private val beta: Float,
) : WindowsConsumer {

	override operator fun invoke(fn: (consumer: RectIsometric) -> Unit) {
		val edge = g.d.h
		val w = right.x - left.x - 2 * gap
		val x = left.x + gap
		val b = w / cos(beta.toRadian())

		var y = left.y + gap + gap * sin(beta.toRadian())
		while (true) {
			val r = RectIsometricLeft(x, y, windowSize, b, beta)
			fn(r)
			y += gap + windowSize
			if (y > edge && r.right.y > edge) {
				break
			}
		}
	}

}
