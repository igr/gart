package dev.oblac.gart.gfx

import dev.oblac.gart.skia.Point

/**
 * Just a queue of points.
 */
class PointsTrail(val size: Int) {
    private val points = ArrayDeque<Point>(size)

    /**
     * Adds a point to the trail.
     */
    fun add(p: Point) {
        if (points.size == size) {
            points.removeFirst()
        }
        points.add(p)
    }

    /**
     * Iterates over the points in the trail.
     */
    fun forEach(action: (Point) -> Unit) {
        points.forEach(action)
    }

    /**
     * Iterates over the points in the trail in reverse order.
     */
    fun forEachReverse(action: (Point) -> Unit) {
        points.asReversed().forEach(action)
    }

}
