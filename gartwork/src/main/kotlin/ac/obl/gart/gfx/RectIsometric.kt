package ac.obl.gart.gfx

import ac.obl.gart.math.toRadian
import io.github.humbleui.skija.Path
import io.github.humbleui.types.Point
import kotlin.math.cos
import kotlin.math.sin

/**
 * Definition of the isometric rectangle. It is drawn as a skewed rectangle.
 *
 * @param x x coordinate of the leftmost point
 * @param y y coordinate of the rightmost point
 * @param a length from the leftmost point to the next, bottom point
 * @param b length from the leftmost point to the last, top point
 * @param alpha angle of the 'a'
 * @param beta angle of the `b`
 */
sealed class RectIsometric(x: Float, y:Float, a: Float, b: Float, alpha: Float, beta: Float) {
	val left = Point(x, y)
	val bottom: Point
	val right: Point
	val top: Point

	init {
		val alphaRad = alpha.toRadian()
		val betaRad = beta.toRadian()
		val ah = a * sin(alphaRad)
		val aw = a * cos(alphaRad)
		val bh = b * sin(betaRad)
		val bw = b * cos(betaRad)

		bottom = Point(x + aw, y + ah)
		right = Point(bottom.x + bw, bottom.y - bh)
		top = Point(x + bw, y - bh)
	}

	fun path(): Path {
		return Path()
			.moveTo(left)
			.lineTo(bottom)
			.lineTo(right)
			.lineTo(top)
			.closePath()
	}
}

/**
 * Isometric top rectangular.
 */
class RectIsometricTop(x: Float, y:Float, a: Float, b: Float, alpha: Float) : RectIsometric(x, y, a, b, alpha, alpha)
class RectIsometricRight(x: Float, y:Float, a: Float, b: Float, beta: Float) : RectIsometric(x, y, a, b, 90f, beta)
class RectIsometricLeft(x: Float, y:Float, a: Float, b: Float, beta: Float) : RectIsometric(x, y, a, b, 90f, -beta)

