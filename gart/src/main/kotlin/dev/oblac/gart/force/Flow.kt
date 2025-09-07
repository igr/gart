package dev.oblac.gart.force

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.cos
import dev.oblac.gart.angle.middleAngle
import dev.oblac.gart.angle.sin
import dev.oblac.gart.vector.Vector2
import org.jetbrains.skia.Point

/**
 * Flow is a force represented with the direction and _scalar_ magnitude of a flow.
 * Flow magnitude is the speed of the flow.
 * It is not a math vector!
 * For real vector force, see [VecForce].
 *
 * @param direction in radians, indicates the direction of the flow. The angle is measured from the negative x-axis.
 * 0 is up, PI/2 is right, PI is down, 3PI/2 is left.
 */
data class Flow(val direction: Angle, val magnitude: Float = 1f) : Force {

    // todo this is incorrect, see VecForce.plus
    // magnitude is not averaged!
    operator fun plus(other: Flow): Flow {
        return Flow(middleAngle(direction, other.direction), (magnitude + other.magnitude) / 2)
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
