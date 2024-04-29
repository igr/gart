package dev.oblac.gart.force

import dev.oblac.gart.math.Vector
import dev.oblac.gart.math.fastSqrt
import dev.oblac.gart.math.normalizeRad
import org.jetbrains.skia.Point
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vector force has a vector defined in each point.
 */
data class VecForce(val direction: Float, val magnitude: Float = 1f) : Force {

    operator fun plus(other: VecForce): VecForce {
        val x3 = magnitude * cos(direction) + other.magnitude * cos(other.direction)
        val y3 = magnitude * sin(direction) + other.magnitude * sin(other.direction)
        val r3 = fastSqrt(x3 * x3 + y3 * y3)
        val t3 = atan2(y3, x3)
        return VecForce(normalizeRad(t3), r3)
    }

    /**
     * Calculates the offset of a point by the flow.
     */
    override fun apply(p: Point): Vector {
        val dx = sin(direction) * magnitude
        val dy = -cos(direction) * magnitude
        return Vector(dx, dy)
    }
}
