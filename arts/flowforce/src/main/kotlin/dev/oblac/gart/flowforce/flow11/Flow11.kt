package dev.oblac.gart.flowforce.flow11

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.NipponColors.col016_KURENAI
import dev.oblac.gart.color.NipponColors.col234_GOFUN
import dev.oblac.gart.flow.Flow2
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.gfx.isInside
import dev.oblac.gart.gfx.randomPoint
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.gfx.toPath
import dev.oblac.gart.hashgrid.HashGrid
import dev.oblac.gart.noise.OpenSimplexNoise
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("flow11", 1024, 1024)
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

    // prepare flow field
    val simplex = OpenSimplexNoise()
    val flowField = FlowField.of(d) { x, y ->
        val n = simplex.random2D(x * 0.001, y * 0.001) * 360f
        Flow2(Degrees.of(n), StreamlineTracer.STEP_SIZE)
    }

    val tracer = StreamlineTracer(d, flowField)
    val paths = tracer.trace()

    // draw
    c.clear(col016_KURENAI)
    val paint = strokeOf(col234_GOFUN, 2f)
    for (path in paths) {
        c.drawPath(path, paint)
    }

    gart.saveImage(g)
    val w = gart.window()
    w.showImage(g)
}

/**
 * Streamline tracer using HashGrid for spatial indexing.
 * Generates streamlines by following the flow field and seeding new streamlines at regular intervals.
 * This approach creates a dense and visually appealing flow pattern while ensuring streamlines are well-spaced.
 * I used a paper: "Creating evenly-spaced Streamlines of Arbitrary Density" by Bruno Jobard and Wilfred Lefer.
 * https://web.cs.ucdavis.edu/~ma/SIGGRAPH02/course23/notes/papers/Jobard.pdf
 *
 * @param d canvas dimension
 * @param flowField the flow field that defines the vector direction at each point
 * @param dSep separation distance between streamlines; larger values produce sparser output
 * @param maxSteps maximum number of integration steps per streamline direction (forward/backward)
 * @param seedInterval how often (in steps) to generate perpendicular seed candidates for new streamlines
 */
class StreamlineTracer(
    private val d: Dimension,
    private val flowField: FlowField,
    private val dSep: Float = D_SEP,
    private val maxSteps: Int = MAX_STEPS,
    private val seedInterval: Int = SEED_INTERVAL,
) {
    companion object {
        const val D_SEP = 18f
        const val STEP_SIZE = 1f
        const val MAX_STEPS = 300
        const val SEED_INTERVAL = 4
    }

    private val grid = HashGrid(dSep)
    private val seedQueue = ArrayDeque<Point>()
    private var streamlineId = 0

    fun trace(): List<Path> {
        val paths = mutableListOf<Path>()
        seedQueue.addLast(randomPoint(d))

        while (seedQueue.isNotEmpty()) {
            val seed = seedQueue.removeAt((Math.random() * seedQueue.size).toInt())

            if (!grid.isFree(seed)) continue
            if (!seed.isInside(d)) continue

            streamlineId++
            val owner = streamlineId

            val forwardPath = traceStreamline(seed, forward = true, owner = owner)
            val backwardPath = traceStreamline(seed, forward = false, owner = owner)

            // merge into single path: reverse backward + forward
            val allPoints = backwardPath.reversed() + forwardPath.drop(1)
            if (allPoints.size >= 2) {
                paths.add(allPoints.toPath())
            }
        }

        return paths
    }

    private fun traceStreamline(
        seed: Point,
        forward: Boolean,
        owner: Int
    ): List<Point> {
        val ignore = setOf(owner)
        var current = seed
        grid.insert(current, owner)

        val points = mutableListOf(current)

        for (step in 0 until maxSteps) {
            if (!current.isInside(d)) break

            val flow = flowField[current]
            val vec = flow(current)

            val next = if (forward) {
                Point(current.x + vec.x, current.y + vec.y)
            } else {
                Point(current.x - vec.x, current.y - vec.y)
            }

            if (!next.isInside(d)) break
            if (!grid.isFree(next, ignore)) break

            points.add(next)
            grid.insert(next, owner)

            if (step % seedInterval == 0) {
                val perpScale = dSep / STEP_SIZE
                val leftSeed = Point(next.x - vec.y * perpScale, next.y + vec.x * perpScale)
                val rightSeed = Point(next.x + vec.y * perpScale, next.y - vec.x * perpScale)

                if (leftSeed.isInside(d)) seedQueue.addLast(leftSeed)
                if (rightSeed.isInside(d)) seedQueue.addLast(rightSeed)
            }

            current = next
        }

        return points
    }
}
