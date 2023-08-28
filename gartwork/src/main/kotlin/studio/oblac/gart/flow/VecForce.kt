package studio.oblac.gart.flow

import studio.oblac.gart.math.fastSqrt
import studio.oblac.gart.math.normalizeRad
import studio.oblac.gart.skia.Point
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class VecForce(override val direction: Float, override val magnitude: Float = 1f) : Force<VecForce> {

    override operator fun plus(force: VecForce): VecForce {
        val x3 = magnitude * cos(direction) + force.magnitude * cos(force.direction)
        val y3 = magnitude * sin(direction) + force.magnitude * sin(force.direction)
        val r3 = fastSqrt(x3 * x3 + y3 * y3)
        val t3 = atan2(y3, x3)
        return VecForce(normalizeRad(t3), r3)
    }

    /**
     * Calculates the offset of a point by the flow.
     */
    override fun offset(p: Point): Point {
        val dx = sin(direction) * magnitude
        val dy = -cos(direction) * magnitude
        return p.offset(dx, dy)
    }
}
