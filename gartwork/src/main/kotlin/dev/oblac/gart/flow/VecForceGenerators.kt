package dev.oblac.gart.flow

import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.RotationDirection
import dev.oblac.gart.math.fastSqrt
import dev.oblac.gart.math.normalizeRad
import kotlin.math.atan2

/**
 * Circular force that pulls the points towards the center.
 * It is the same as [VecForce] except there is no additional angle pull.
 * Closer points are faster. Points are always circulating around the center.
 */
class CircularVecForce(
    private val cx: Float,
    private val cy: Float,
    private val maxMagnitude: Float = 1024f,
    private val direction: RotationDirection = RotationDirection.CW
) : ForceGenerator {
    override fun invoke(x: Float, y: Float): VecForce {
        val dx = x - cx
        val dy = y - cy

        val theta = when (direction) {
            RotationDirection.CW -> PIf - atan2(-dy, dx)
            RotationDirection.CCW -> -atan2(-dy, dx)
        }

        val distance = fastSqrt(dx * dx + dy * dy)
        val magnitude = maxMagnitude / (maxMagnitude * 0.1f + distance)

        return VecForce(normalizeRad(theta), magnitude)
    }
}

/**
 * Spiral force that pulls the points towards the center.
 * The center is never reached - points start to circle around it.
 */
class SpiralVecForce(
    val cx: Float,
    val cy: Float,
    /**
     * The speed of the spiral. The higher the value, the faster the spiral.pull.
     */
    private val spiralSpeed: Float = 0.3f,
    /**
     * The magnitude is calculated as this value divided by the distance from the center.
     */
    private val maxMagnitude: Float = 1024f,
    private val minDistance: Float = 200f,
    private val direction: RotationDirection = RotationDirection.CW
) : ForceGenerator {
    override fun invoke(x: Float, y: Float): VecForce {
        val dx = x - cx
        val dy = y - cy

        val theta = when (direction) {
            RotationDirection.CW -> PIf - atan2(-dy, dx)
            RotationDirection.CCW -> -atan2(-dy, dx)
        } + spiralSpeed

        val distance = fastSqrt(dx * dx + dy * dy)
        val magnitude = maxMagnitude / (minDistance + distance)

        return VecForce(normalizeRad(theta), magnitude)
    }
}
