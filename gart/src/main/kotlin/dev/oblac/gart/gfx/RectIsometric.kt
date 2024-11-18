package dev.oblac.gart.gfx

import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.angles.cos
import dev.oblac.gart.angles.sin
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point

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
sealed class RectIsometric(x: Float, y: Float, a: Float, b: Float, alpha: Degrees, beta: Degrees) {
    val left = Point(x, y)
    val bottom: Point
    val right: Point
    val top: Point

    init {
        val ah = a * sin(alpha)
        val aw = a * cos(alpha)
        val bh = b * sin(beta)
        val bw = b * cos(beta)

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
class RectIsometricTop(x: Float, y: Float, a: Float, b: Float, alpha: Degrees) : RectIsometric(x, y, a, b, alpha, alpha)
class RectIsometricRight(x: Float, y: Float, a: Float, b: Float, beta: Degrees) : RectIsometric(x, y, a, b, Degrees.D90, beta)
class RectIsometricLeft(x: Float, y: Float, a: Float, b: Float, beta: Degrees) : RectIsometric(x, y, a, b, Degrees.D90, -beta)

