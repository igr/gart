package dev.oblac.gart.flow

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Radians
import dev.oblac.gart.angle.cos
import dev.oblac.gart.angle.sin
import dev.oblac.gart.math.fastSqrt
import dev.oblac.gart.vector.Vector2
import org.jetbrains.skia.Point
import kotlin.math.atan2

/**
 * Vector flow has a vector defined in each point.
 */
data class FlowVec(val direction: Angle, val magnitude: Float = 1f) : Flow {

    operator fun plus(other: FlowVec): FlowVec {
        val x3 = magnitude * cos(direction) + other.magnitude * cos(other.direction)
        val y3 = magnitude * sin(direction) + other.magnitude * sin(other.direction)
        val r3 = fastSqrt(x3 * x3 + y3 * y3)
        val t3 = atan2(y3, x3)
        return FlowVec(Radians.of(t3).normalize(), r3.toFloat())
    }

    /**
     * Calculates the offset of a point by the flow.
     */
    override fun invoke(p: Point): Vector2 {
        val dx = sin(direction) * magnitude
        val dy = -cos(direction) * magnitude
        return Vector2(dx, dy)
    }
}
