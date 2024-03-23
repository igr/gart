package dev.oblac.gart.force

import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.RotationDirection
import dev.oblac.gart.math.RotationDirection.CW
import dev.oblac.gart.math.normalizeRad
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Simple circular flow generator.
 */
class CircularFlow(
    private val cx: Float,
    private val cy: Float,
    private val direction: RotationDirection = CW,
    private val magnitude: Float = 1f,
) : ForceGenerator {
    override fun invoke(x: Float, y: Float): Flow {
        val dx = x - cx
        val dy = y - cy

        val theta = when (direction) {
            CW -> PIf - atan2(-dy, dx)
            RotationDirection.CCW -> -atan2(-dy, dx)
        }

        return Flow(normalizeRad(theta), magnitude)
    }
}

class SpiralFlow(
    val cx: Float,
    val cy: Float,
    private val spiralSpeed: Float = 0.3f,
    private val direction: RotationDirection = CW,
    private val magnitude: Float = 1f,
) : ForceGenerator {
    override fun invoke(x: Float, y: Float): Flow {
        val dx = x - cx
        val dy = y - cy

        val theta = when (direction) {
            CW -> PIf - atan2(-dy, dx)
            RotationDirection.CCW -> -atan2(-dy, dx)
        } + spiralSpeed

        return Flow(normalizeRad(theta), magnitude)
    }
}

class WaveFlow(
    private val xFreq: Float = 0.01f,
    private val yFreq: Float = 0.03f,
    private val xAmp: Float = 0.8f,
    private val yAmp: Float = 0.5f,
    private val magnitude: Float = 1f,
) : ForceGenerator {
    override fun invoke(x: Float, y: Float): Flow {
        val a = sin(x * xFreq) * xAmp
        val b = cos(y * yFreq) * yAmp
        return Flow(a + b, magnitude)
    }

}
