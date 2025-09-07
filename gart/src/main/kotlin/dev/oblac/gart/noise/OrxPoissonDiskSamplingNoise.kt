package dev.oblac.gart.noise

import dev.oblac.gart.Dimension
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.gfx.*
import dev.oblac.gart.hashgrid.HashGrid
import dev.oblac.gart.math.Polar
import dev.oblac.gart.math.distSquared
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.abs
import kotlin.random.Random

fun poissonDiskSamplingNoise(d: Dimension, r: Number = 30.0): List<Point> {
    return poissonDiskSampling(
        Rect.ofXYWH(0.0, 0.0, d.wd, d.hd), r.toFloat()
    )
}


internal const val epsilon = 0.0000001f

/**
 * Creates a random point distribution on a given area
 * Each point gets n [tries] at generating the next point
 * By default the points are generated along the circumference of r + epsilon to the point
 * They can also be generated on a ring like in the original algorithm from Robert Bridson
 *
 * @param bounds the rectangular bounds of the area to generate points in
 * @param radius the minimum distance between each point
 * @param tries number of candidates per point
 * @param randomOnRing generate random points on a ring with an annulus from r to 2r
 * @param random a random number generator, default value is [Random.Default]
 * @param initialPoints a list of points in sampler space, these points will not be tested against [r]
 * @param obstacleHashGrids a list of obstacles to avoid, defined by points and radii
 * @param boundsMapper a custom function to check if a point is within bounds

 * @return a list of points
 */
fun poissonDiskSampling(
    bounds: Rect,
    radius: Float,
    tries: Int = 30,
    randomOnRing: Boolean = true,
    random: Random = Random.Default,
    initialPoints: List<Point> = listOf(bounds.center()),
    obstacleHashGrids: List<HashGrid> = emptyList(),
    boundsMapper: ((v: Point) -> Boolean)? = null,
): List<Point> {
    val disk = mutableListOf<Point>()
    val queue = mutableSetOf<Pair<Point, Float>>()
    val hashGrid = HashGrid(radius)

    fun addPoint(v: Point, radius: Float) {
        hashGrid.insert(v)
        disk.add(v)
        queue.add(Pair(v, radius))
    }

    for (initialPoint in initialPoints) {
        addPoint(initialPoint, radius)
    }

    for (ohg in obstacleHashGrids) {
        for (point in ohg.points()) {
            queue.add(Pair(point.first, ohg.radius))
        }
    }

    while (queue.isNotEmpty()) {
        val queueItem = queue.random(random)
        val (active, activeRadius) = queueItem
        var candidateAccepted = false
        candidateSearch@ for (l in 0 until tries) {
            val c = if (randomOnRing) {
                active + Point.uniformRing(activeRadius, 2 * activeRadius - epsilon)
            } else {
                active + Polar(Degrees.of(rndf(0.0, 360.0)), activeRadius).cartesian
            }
            if (!bounds.contains(c)) continue@candidateSearch

            if (!hashGrid.isFree(c) || obstacleHashGrids.any { !it.isFree(c) })
                continue@candidateSearch

            // check if the candidate point is within bounds
            // EJ: This is somewhat counter-intuitively moved to the last stage in the process;
            //     It turns out that the above neighbour search is much more affordable than the bounds check in the
            //     case of complex bounds (such as described by Shapes or ShapeContours). A simple benchmark shows a
            //     speed-up of roughly 300%
            if (boundsMapper != null && !boundsMapper(c)) continue@candidateSearch

            addPoint(c, radius)
            candidateAccepted = true
            break
        }

        // If no candidate was accepted, remove the sample from the active list
        if (!candidateAccepted) {
            queue.remove(queueItem)
        }
    }
    return disk
}

fun Point.Companion.uniformRing(
    innerRadius: Float = 0.0f,
    outerRadius: Float = 1.0f,
): Point {
    require(innerRadius <= outerRadius)

    val eps = 1E-6

    if (abs(innerRadius - outerRadius) < eps) {
        val angle = rndf(-180.0, 180.0)
        return Polar(Degrees.of(angle), innerRadius).cartesian

    } else if (innerRadius < outerRadius) {
        while (true) {
            uniform(-outerRadius, outerRadius).let {
                val squaredLength = distSquared(Point.ZERO, it)
                if (squaredLength >= innerRadius * innerRadius && squaredLength < outerRadius * outerRadius) {
                    return it
                }
            }
        }
    } else {
        error("innerRadius ($innerRadius) should be less or equal to outerRadius ($outerRadius)")
    }
}


private fun uniform(
    min: Float = -1.0f, max: Float = 1.0f
) = randomPoint(Point(min, min), Point(max, max))
