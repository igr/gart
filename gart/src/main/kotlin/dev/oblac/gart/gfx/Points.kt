package dev.oblac.gart.gfx

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.skia.Point

/**
 * Encapsulate list of points, so we don't relay on
 * actual implementation of list, and can change it in future if needed.
 * Experimental - why we need this?
 */
@ApiStatus.Experimental
class Points {
    private val points = mutableListOf<Point>()

    operator fun plusAssign(point: Point) {
        points.add(point)
    }

    fun path() = points.toPath()

}
