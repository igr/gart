package dev.oblac.gart.flow

import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.RotationDirection
import dev.oblac.gart.math.RotationDirection.CW
import dev.oblac.gart.math.normalizeRad
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class CircularFlowForce(
    private val cx: Float,
    private val cy: Float,
    private val direction: RotationDirection = CW,
    private val magnitude: Float = 1f,
) : ForceGenerator<FlowForce> {
    override fun invoke(x: Float, y: Float): FlowForce {
        val dx = x - cx
        val dy = y - cy

        val theta = when (direction) {
            CW -> PIf - atan2(-dy, dx)
            RotationDirection.CCW -> -atan2(-dy, dx)
        }

        return FlowForce(normalizeRad(theta), magnitude)
    }
}

class SpiralFlowForce(
    val cx: Float,
    val cy: Float,
    private val spiralSpeed: Float = 0.3f,
    private val direction: RotationDirection = CW,
    private val magnitude: Float = 1f,
) : ForceGenerator<FlowForce> {
    override fun invoke(x: Float, y: Float): FlowForce {
        val dx = x - cx
        val dy = y - cy

        val theta = when (direction) {
            CW -> PIf - atan2(-dy, dx)
            RotationDirection.CCW -> -atan2(-dy, dx)
        } + spiralSpeed

        return FlowForce(normalizeRad(theta), magnitude)
    }
}

class WaveFlowForce(
    private val xFreq: Float = 0.01f,
    private val yFreq: Float = 0.03f,
    private val xAmp: Float = 0.8f,
    private val yAmp: Float = 0.5f,
    private val magnitude: Float = 1f,
) : ForceGenerator<FlowForce> {
    override fun invoke(x: Float, y: Float): FlowForce {
        val a = sin(x * xFreq) * xAmp
        val b = cos(y * yFreq) * yAmp
        return FlowForce(a + b, magnitude)
    }

}