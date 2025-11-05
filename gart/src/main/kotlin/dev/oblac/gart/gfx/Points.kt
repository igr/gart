package dev.oblac.gart.gfx

import org.jetbrains.skia.Point

/**
 * Encapsulate list of points.
 * Experimental - why we need this?
 */
class Points {
    private val points = mutableListOf<Point>()

    operator fun plusAssign(point: Point) {
        points.add(point)
    }

    fun path() = points.toPath()

}
