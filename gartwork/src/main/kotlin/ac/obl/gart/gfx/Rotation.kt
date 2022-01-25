package ac.obl.gart.gfx

import ac.obl.gart.math.toRadian
import io.github.humbleui.types.Point
import kotlin.math.cos
import kotlin.math.sin

/**
 * Defines a rotation around provided center for given angle.
 */
class RotatePoint(private val x: Float, private val y: Float, degrees: Float) {
	private val alpha = degrees.toRadian()

	operator fun invoke(point: Point): Point {
		val cx = x + (point.x - x) * cos(alpha) - (point.y - y) * sin(alpha)
		val cy = y + (point.x - x) * sin(alpha) + (point.y - y) * cos(alpha)
		return Point(cx, cy)
	}
}