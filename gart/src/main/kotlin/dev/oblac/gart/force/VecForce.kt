package dev.oblac.gart.force

import dev.oblac.gart.angles.Angle
import dev.oblac.gart.angles.Radians
import dev.oblac.gart.angles.cos
import dev.oblac.gart.angles.sin
import dev.oblac.gart.math.Vector2
import dev.oblac.gart.math.fastSqrt
import org.jetbrains.skia.Point
import kotlin.math.atan2

/**
 * Vector force has a vector defined in each point.
 */
data class VecForce(val direction: Angle, val magnitude: Float = 1f) : Force {

    operator fun plus(other: VecForce): VecForce {
        val x3 = magnitude * cos(direction) + other.magnitude * cos(other.direction)
        val y3 = magnitude * sin(direction) + other.magnitude * sin(other.direction)
        val r3 = fastSqrt(x3 * x3 + y3 * y3)
        val t3 = atan2(y3, x3)
        return VecForce(Radians.of(t3).normalize(), r3.toFloat())
    }

    /**
     * Calculates the offset of a point by the flow.
     */
    override fun apply(p: Point): Vector2 {
        val dx = sin(direction) * magnitude
        val dy = -cos(direction) * magnitude
        return Vector2(dx, dy)
    }
}
