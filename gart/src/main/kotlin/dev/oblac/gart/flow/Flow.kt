package dev.oblac.gart.flow

import dev.oblac.gart.gfx.offset
import dev.oblac.gart.vector.Vector2
import org.jetbrains.skia.Point

/**
 * Represents a flow at a point.
 */
fun interface Flow {

    /**
     * Returns the offset (force vector) at the given point.
     */
    operator fun invoke(p: Point): Vector2

    /**
     * Applies the flow to the given point and returns the new point.
     */
    fun offset(p: Point) = invoke(p).let { p.offset(it) }

}
