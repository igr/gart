package dev.oblac.gart.noise

import dev.oblac.gart.math.Vector2
import dev.oblac.gart.math.rnd
import java.util.*
import kotlin.math.*

/**
 * Generates sets of random points via <i>Poisson Disk Sampling</i>.
 * Poisson-disc sampling produces points that are tightly-packed, but no closer
 * to each other than a specified minimum distance, resulting in a natural and
 * desirable pattern for many applications. This distribution is also described
 * as blue noise.
 *
 * The algorithm in this class is a Fork of <i>Martin Roberts’s</i> tweak to
 * <i>Bridson's Algorithm</i> for Poisson Disk sampling. This approach is faster
 * and better than the Bridson Algorithm, and balances performance with
 *  distribution quality compared to Robert's tweak.
 */
class PoissonDiskSamplingNoise(seed: Long = System.nanoTime()) {

    private var grid: DoubleArray
    private var cellSize: Double = 0.0
    private var gridWidth: Int = 0
    private var queue: MutableList<DoubleArray>
    private var random: SplittableRandom = SplittableRandom(seed)
    private var xOffset: Float = 0f
    private var yOffset: Float = 0f

    var points: MutableList<Vector2> = mutableListOf()

    init {
        grid = doubleArrayOf()
        queue = mutableListOf()
    }

    fun generate(xmin: Double, ymin: Double, xmax: Double, ymax: Double, minDist: Double, rejectionLimit: Int): List<Vector2> {
        xOffset = xmin.toFloat()
        yOffset = ymin.toFloat()
        return generate(xmax - xmin, ymax - ymin, minDist, rejectionLimit)
    }

    fun generate(xmin: Double, ymin: Double, xmax: Double, ymax: Double, minDist: Double): List<Vector2> {
        return generate(xmin, ymin, xmax, ymax, minDist, 11)
    }

    fun generate(xmin: Double, ymin: Double, xmax: Double, ymax: Double, n: Int): List<Vector2> {
        val radius2 = (sqrt(0.5) * ((xmax - xmin) * (ymax - ymin))) / n
        val radius = sqrt(radius2)
        val pointz = generate(xmin, ymin, xmax, ymax, radius, 11).shuffled(Random(1337))
        return pointz.subList(0, min(pointz.size, n))
    }

    private fun generate(width: Double, height: Double, radius: Double, k: Int): List<Vector2> {
        val m = 1 + k * 2
        cellSize = 1 / (radius * sqrt(0.5))
        val minDistSquared = radius * radius
        gridWidth = ceil(width * cellSize).toInt() + 4
        val gridHeight = ceil(height * cellSize).toInt() + 4
        grid = DoubleArray(2 * gridWidth * gridHeight)
        queue = mutableListOf()
        val rotx = cos((2 * PI * m) / k)
        val roty = sin((2 * PI * m) / k)

        points.clear()

        sample(width * rnd(0.45, 0.55), height * rnd(0.45, 0.55))

        while (queue.isNotEmpty()) {
            val i = random.nextInt(queue.size)
            val parent = queue[i]

            val epsilon = 1e-6
            val t = tanpi2(2 * random.nextDouble() - 1)
            val q = 1.0 / (1 + t * t)
            var dw: Double
            var dx = if (q != 0.0) (1 - t * t) * q else -1.0
            var dy = if (q != 0.0) 2 * t * q else 0.0

            for (j in 0 until k) {
                dw = dx * rotx - dy * roty
                dy = dx * roty + dy * rotx
                dx = dw

                val r = radius * (1 + epsilon + 0.65 * random.nextDouble() * random.nextDouble())
                val x = parent[0] + r * dx
                val y = parent[1] + r * dy

                if (x in 0.0..width && y in 0.0..height && far(x, y, minDistSquared)) {
                    sample(x, y)
                }
            }
            queue.removeAt(i)
        }

        return ArrayList(points)
    }

    private fun tanpi2(a: Double): Double {
        val b = (1 - a * a)
        return a * (-0.0187108 * b + 0.31583526 + 1.27365776 / b)
    }

    private fun sample(x: Double, y: Double) {
        val i = floor(x * cellSize + 2).toInt()
        val j = floor(y * cellSize + 2).toInt()
        val index = 2 * (gridWidth * j + i)
        grid[index] = x
        grid[index + 1] = y
        queue.add(doubleArrayOf(x, y))
        points.add(Vector2(x.toFloat() + xOffset, y.toFloat() + yOffset))
    }

    private fun far(x: Double, y: Double, minDistSquared: Double): Boolean {
        val j0 = floor(y * cellSize).toInt()
        val i0 = floor(x * cellSize).toInt()
        for (j in j0 until j0 + 5) {
            val index0 = 2 * (j * gridWidth + i0)
            for (i in index0 until index0 + 10 step 2) {
                val dx = grid[i] - x
                val dy = grid[i + 1] - y
                if (dx * dx + dy * dy < minDistSquared)
                    return false
            }
        }
        return true
    }

}
