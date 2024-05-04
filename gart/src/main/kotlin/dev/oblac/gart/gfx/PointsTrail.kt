package dev.oblac.gart.gfx

import org.jetbrains.skia.Path
import org.jetbrains.skia.Point

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
     * Returns sequence of the points in the trail, starting from
     * the oldest point (first) to the newest point (last).
     */
    fun sequence(): Sequence<Point> = points.asSequence()

    fun sequenceIndexed(): Sequence<IndexedValue<Point>> = points.asSequence().withIndex()

    fun last(): Point {
        return points.last()
    }

    fun update(apply: (Point) -> Point?): PointsTrail {
        if (points.isEmpty()) {
            return this
        }
        val last = points.last()
        val newPoint = apply(last)
        if (newPoint != null) {
            this.add(newPoint)
        } else {
            points.removeFirst()
        }

        return this
    }

    /**
     * Filters points in the trail.
     * Warning - this method modifies the trail and may be slow.
     */
    fun filter(predicate: (Point) -> Boolean): PointsTrail {
        points.removeIf { !predicate(it) }
        return this
    }

    fun isActive(): Boolean = points.isNotEmpty()
    fun isEmpty(): Boolean = points.isEmpty()

    fun toPath(): Path = pathOf(points)

}

/**
 * Counts trails that are active, i.e. have at least one point.
 */
fun List<PointsTrail>.countActive() = this.count { it.isActive() }
