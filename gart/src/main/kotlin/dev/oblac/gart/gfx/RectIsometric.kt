package dev.oblac.gart.gfx

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.angle.cos
import dev.oblac.gart.angle.sin
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
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
sealed class RectIsometric(x: Float, y: Float, a: Float, b: Float, alpha: Angle, beta: Angle) {
    val left = Point(x, y)
    val bottom: Point
    val right: Point
    val top: Point

    init {
        val ah = a * sin(alpha)
        val aw = a * cos(alpha)
        val bh = b * sin(beta)
        val bw = b * cos(beta)

        bottom = pointOf(x + aw, y + ah)
        right = pointOf(bottom.x + bw, bottom.y - bh)
        top = pointOf(x + bw, y - bh)
    }

    fun path(): Path {
        return PathBuilder()
            .moveTo(left)
            .lineTo(bottom)
            .lineTo(right)
            .lineTo(top)
            .closePath()
            .detach()
    }

    fun width(): Float {
        return right.x - left.x
    }

    fun height(): Float {
        return bottom.y - top.y
    }
}

/**
 * Isometric top rectangular.
 */
class RectIsometricTop(x: Float, y: Float, a: Float, b: Float, alpha: Angle) : RectIsometric(x, y, a, b, alpha, alpha)
class RectIsometricRight(x: Float, y: Float, a: Float, b: Float, beta: Angle) : RectIsometric(x, y, a, b, Degrees.D90, beta)
class RectIsometricLeft(x: Float, y: Float, a: Float, b: Float, beta: Angle) : RectIsometric(x, y, a, b, Degrees.D90, -beta)

