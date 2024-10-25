package dev.oblac.gart.force

import dev.oblac.gart.angles.Radians
import dev.oblac.gart.angles.cos
import dev.oblac.gart.angles.middleAngle
import dev.oblac.gart.angles.sin
import dev.oblac.gart.math.Vector2
import org.jetbrains.skia.Point

/**
 * Flow is a vector that represents the direction and magnitude of a flow.
 * Flow magnitude is the speed of the flow. It is **equal** in each point of the flow.
 *
 * @param direction in radians, indicates the direction of the flow. The angle is measured from the negative x-axis.
 * 0 is up, PI/2 is right, PI is down, 3PI/2 is left.
 */
data class Flow(val direction: Radians, val magnitude: Float = 1f) : Force {

    // todo remove? ne treba jer ce sabirati vektori
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
