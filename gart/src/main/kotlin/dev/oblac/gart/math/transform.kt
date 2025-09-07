package dev.oblac.gart.math

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.cos
import dev.oblac.gart.angle.sin
import dev.oblac.gart.gfx.pointOf
import org.jetbrains.skia.Point

fun interface Transform {
    operator fun invoke(point: Point): Point
    operator fun plus(transform: Transform) = Transform { point -> transform(this.invoke(point)) }

    companion object {
        fun rotate(point: Point, angle: Angle): Transform {
            return RotationTransform(point.x, point.y, angle)
        }

        fun rotate(x: Number, y: Number, angle: Angle): Transform {
            return RotationTransform(x.toFloat(), y.toFloat(), angle)
        }
    }
}

enum class RotationDirection {
    CW,
    CCW
}

class RotationTransform(private val x: Float, private val y: Float, private val angle: Angle) : Transform {
    override operator fun invoke(point: Point): Point {
        val cx = x + (point.x - x) * cos(angle) - (point.y - y) * sin(angle)
        val cy = y + (point.x - x) * sin(angle) + (point.y - y) * cos(angle)
        return pointOf(cx, cy)
    }
}
