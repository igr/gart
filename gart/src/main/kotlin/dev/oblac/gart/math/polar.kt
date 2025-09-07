package dev.oblac.gart.math

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Radians
import dev.oblac.gart.angle.cos
import dev.oblac.gart.angle.sin
import dev.oblac.gart.gfx.Line
import org.jetbrains.skia.Point
import kotlin.math.atan2

data class Polar(val theta: Angle, val radius: Float = 1.0f) {

    companion object {
        /** Constructs equivalent polar coordinates from the Cartesian coordinate system. */
        fun of(point: Point): Polar {
            val line = Line(Point(0f, 0f), point)
            val r = line.length()
            return Polar(
                Radians.of(if (r == 0.0f) 0.0f else atan2(point.y, point.x)),
                r
            )
        }
    }

    val cartesian: Point
        get() {
            return Point(
                radius * cos(theta).toFloat(),
                radius * sin(theta).toFloat()
            )
        }

    operator fun plus(right: Polar) = Polar(theta + right.theta, radius + right.radius)
    operator fun minus(right: Polar) = Polar(theta - right.theta, radius - right.radius)

    //operator fun times(scale: Polar) = Polar(theta * scale.theta, radius * scale.radius)
    //operator fun times(scale: Double) = Polar(theta * scale, radius * scale)
    //operator fun div(scale: Double) = Polar(theta / scale, radius / scale)
}
