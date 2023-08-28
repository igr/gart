package studio.oblac.gart.math

import studio.oblac.gart.skia.Point
import kotlin.math.cos
import kotlin.math.sin

enum class RotationDirection {
    CW,
    CCW
}

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
