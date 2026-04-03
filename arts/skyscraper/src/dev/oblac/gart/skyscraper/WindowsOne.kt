package dev.oblac.gart.skyscraper

import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.gfx.RectIsometric
import dev.oblac.gart.gfx.RectIsometricLeft
import dev.oblac.gart.math.toRadians
import org.jetbrains.skia.Point
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
        val edge = gart.d.h
		val w = right.x - left.x - 2 * gap
		val x = left.x + gap
		val b = w / cos(beta.toRadians())

		var y = left.y + gap + gap * sin(beta.toRadians())
		while (true) {
            val r = RectIsometricLeft(x, y, windowSize, b, Degrees(beta))
			fn(r)
			y += gap + windowSize
			if (y > edge && r.right.y > edge) {
				break
			}
		}
	}

}
