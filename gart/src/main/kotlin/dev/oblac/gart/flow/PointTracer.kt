package dev.oblac.gart.flow

import dev.oblac.gart.Dimension
import dev.oblac.gart.gfx.isInside
import org.jetbrains.skia.Point

/**
 * Traces the path of a point through a flow field for a given number of steps.
 * The point is updated by applying the flow at its current position, and the new position is added to the path.
 * The tracing continues until the specified number of steps is reached or the point goes outside the dimension.
 */
class PointTracer(
    private val d: Dimension,
    private val flowField: FlowField,
) {
    
    fun trace(point: Point, steps: Int): List<Point> =
        generateSequence(point) { p ->
            if (p.isInside(d)) flowField[p].offset(p) else null
        }.take(steps + 1).toList()
}
