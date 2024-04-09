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

    fun last(): Point {
        return points.last()
    }

    fun update(apply: (Point) -> Point): PointsTrail {
        val last = points.last()
        val newPoint = apply(last)
        this.add(newPoint)
        return this
    }

    fun filter(predicate: (Point) -> Boolean): PointsTrail {
        points.removeIf { !predicate(it) }
        return this
    }

    fun isNotEmpty(): Boolean = points.isNotEmpty()

}
