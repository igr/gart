package dev.oblac.gart.attractor

import dev.oblac.gart.skia.Point3

interface Attractor {
    fun compute(p: Point3, dt: Float): Point3

    /**
     * Computes N points of the attractor starting from the given point.
     */
    fun computeN(p: Point3, dt: Float, n: Int): List<Point3> {
        val result = mutableListOf(p)
        repeat(n) {
            result.add(compute(result.last(), dt))
        }
        return result
    }
}
