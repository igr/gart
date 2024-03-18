package dev.oblac.gart.flow

import dev.oblac.gart.math.middleAngle
import dev.oblac.gart.skia.Point
import kotlin.math.cos
import kotlin.math.sin

/**
 * Flow is a vector that represents the direction and magnitude of a flow.
 * Flow magnitude is the speed of the flow. It is **equal** in each point of the flow.
 *
 * @param direction in radians, indicates the direction of the flow. The angle is measured from the negative x-axis.
 * 0 is up, PI/2 is right, PI is down, 3PI/2 is left.
 */
data class Flow(override val direction: Float, override val magnitude: Float = 1f) : Force<Flow> {

    override operator fun plus(other: Flow): Flow {
        return Flow(middleAngle(direction, other.direction), (magnitude + other.magnitude) / 2)
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
