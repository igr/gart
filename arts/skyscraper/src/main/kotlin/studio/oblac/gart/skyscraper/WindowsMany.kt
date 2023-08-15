package studio.oblac.gart.skyscraper

import studio.oblac.gart.gfx.RectIsometric
import studio.oblac.gart.gfx.RectIsometricLeft
import studio.oblac.gart.math.toRadian
import studio.oblac.gart.skia.Point
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class WindowsMany(
	private val left: Point,
	private val right: Point,
	private val windowSize: Float,
	gap: Float,
	minGapSide: Float,
	private val beta: Float
) : WindowsConsumer {

	private val count: Int
	private val gapX: Float
	private val gapY: Float
	private val gapEdge: Float

	init {
		gapX = gap// * cos(beta.toRadian())
		gapY = gap// * sin(beta.toRadian())

		// calculate the count of the windows
		// the leftmost and rightmost gap MAY be different from the give gap!
		val w = right.x - left.x - 2 * minGapSide
		count = floor(((w + gap) / (windowSize + gap))).toInt()

		gapEdge = minGapSide + (w - (count * (windowSize + gap) - gap)) / 2
	}

	override operator fun invoke(fn: (consumer: RectIsometric) -> Unit) {
		val edge = g.d.h

		val x = left.x
		var y = left.y

		while (true) {
			var rowX = x  + gapEdge
			var rowY = 0f

			if (rowX + windowSize >= right.x) {
				// no windows
				break
			}

			while (rowX + windowSize < right.x) {
				val w = rowX - x
				rowY = y + gapEdge + w * sin(beta.toRadian()) / cos(beta.toRadian())
				fn(RectIsometricLeft(rowX, rowY, windowSize, windowSize, beta))
				rowX += windowSize + gapX
			}
			y += gapY + windowSize

			if (y > edge && rowY > edge) {
				break
			}
		}
	}

}
