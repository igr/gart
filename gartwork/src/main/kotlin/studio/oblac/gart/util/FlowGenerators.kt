package studio.oblac.gart.util

import studio.oblac.gart.math.PIf
import studio.oblac.gart.math.RotationDirection
import studio.oblac.gart.math.RotationDirection.CCW
import studio.oblac.gart.math.RotationDirection.CW
import studio.oblac.gart.math.normalizeRad
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class CircularFlow(private val cx: Float, private val cy: Float, private val direction: RotationDirection = CW) : FlowGenerator {
    override fun invoke(x: Float, y: Float): Flow {
        val dx = x - cx
        val dy = y - cy

        val theta = when (direction) {
            CW -> PIf - atan2(-dy, dx)
            CCW -> -atan2(-dy, dx)
        }

        return Flow(normalizeRad(theta), 1f)
    }
}

class WaveFlow(
    private val xFreq: Float = 0.01f,
    private val yFreq: Float = 0.03f,
    private val xAmp: Float = 0.8f,
    private val yAmp: Float = 0.5f,
) : FlowGenerator {
    override fun invoke(x: Float, y: Float): Flow {
        val a = sin(x * xFreq) * xAmp
        val b = cos(y * yFreq) * yAmp
        return Flow(a + b, 1f)
    }

}
