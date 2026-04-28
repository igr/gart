package dev.oblac.gart.grow

import dev.oblac.gart.gfx.toPoints
import dev.oblac.gart.math.TWO_PIf
import dev.oblac.gart.math.rndGaussian
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.PathMeasure
import org.jetbrains.skia.Point
import kotlin.math.*

/**
 * A closed polyline of connected nodes grows over time. Each step:
 *
 *   1. brownian noise nudges every node — the random "growth" energy
 *   2. attraction pulls each node toward the midpoint of its two neighbors
 *      (smooths the curve)
 *   3. rejection pushes nodes away from any other node within the
 *      rejection radius — what shepherds the random walks and prevents
 *      self-intersection
 *   4. any edge longer than maxEdgeLength gets a midpoint inserted,
 *      so the perimeter can grow
 *
 * A uniform spatial hash (linked-list-in-array per cell) gives O(1)
 * average-case neighbour lookup so the algorithm scales to many thousands
 * of nodes.
 */
class Growth(
    val maxEdgeLength: Float = 5f,
    val rejectionRadius: Float = 10f,
    val attractionStrength: Float = 0.15f,
    val rejectionStrength: Float = 0.5f,
    val brownianStrength: Float = 0.4f,
    val closed: Boolean = true,
    val maxNodes: Int = 200_000,
    val centerX: Float = 0f,
    val centerY: Float = 0f,
    val maxRadius: Float = Float.POSITIVE_INFINITY,
    /** Peak push from obstacle samples. Make this larger than [rejectionStrength]
     *  so the wall holds under sustained pressure from a packed curve. */
    val obstacleStrength: Float = rejectionStrength * 4f,
    /** Sample spacing along an obstacle path, as a fraction of [rejectionRadius].
     *  Smaller = denser = harder to slip through. */
    val obstacleSampleFraction: Float = 0.25f,
) {
    var size: Int = 0
        private set

    var done: Boolean = false
        private set

    val xs = FloatArray(maxNodes)
    val ys = FloatArray(maxNodes)

    private val nx = FloatArray(maxNodes)
    private val ny = FloatArray(maxNodes)

    private val cellSize = rejectionRadius
    private val nextNode = IntArray(maxNodes)
    private val cellHead = HashMap<Long, Int>()

    private var obstacleX = FloatArray(0)
    private var obstacleY = FloatArray(0)
    private var obstacleNext = IntArray(0)
    private val obstacleCellHead = HashMap<Long, Int>()

    private val splitMidX = ArrayList<Float>()
    private val splitMidY = ArrayList<Float>()
    private val splitInsertAt = ArrayList<Int>()

    fun seedCircle(cx: Float, cy: Float, radius: Float, count: Int) {
        size = count
        for (i in 0 until count) {
            val a = i * TWO_PIf / count
            xs[i] = cx + radius * cos(a)
            ys[i] = cy + radius * sin(a)
        }
    }

    /**
     * Sets a static obstacle from sampled points. Curve nodes are repelled by
     * each obstacle point using the same rejection field as inter-node rejection.
     * Sample density should be at least one point every (rejectionRadius / 2)
     * units along the obstacle so the curve cannot slip between samples.
     */
    fun setObstacle(points: List<Point>) {
        val n = points.size
        obstacleX = FloatArray(n)
        obstacleY = FloatArray(n)
        obstacleNext = IntArray(n)
        for (i in 0 until n) {
            obstacleX[i] = points[i].x
            obstacleY[i] = points[i].y
        }
        obstacleCellHead.clear()
        for (i in 0 until n) {
            val key = packKey(floor(obstacleX[i] / cellSize).toInt(), floor(obstacleY[i] / cellSize).toInt())
            obstacleNext[i] = obstacleCellHead[key] ?: -1
            obstacleCellHead[key] = i
        }
    }

    /**
     * Samples [path] at a density of one point every
     * (rejectionRadius * obstacleSampleFraction) units and uses the result
     * as an obstacle.
     */
    fun setObstacle(path: Path) {
        val length = PathMeasure(path).length
        val spacing = rejectionRadius * obstacleSampleFraction
        val samples = max(8, (length / spacing).toInt())
        setObstacle(path.toPoints(samples))
    }

    fun step() {
        if (done || size < 2) return
        rebuildGrid()
        applyForces()
        commitPositions()
        splitLongEdges()
        if (reachedMaxRadius()) done = true
    }

    private fun reachedMaxRadius(): Boolean {
        if (maxRadius.isInfinite()) return false
        val r2 = maxRadius * maxRadius
        for (i in 0 until size) {
            val dx = xs[i] - centerX
            val dy = ys[i] - centerY
            if (dx * dx + dy * dy > r2) return true
        }
        return false
    }

    fun toPath(): Path {
        val p = PathBuilder()
        if (size == 0) return p.detach()
        p.moveTo(xs[0], ys[0])
        for (i in 1 until size) p.lineTo(xs[i], ys[i])
        if (closed) p.closePath()
        return p.detach()
    }

    private fun rebuildGrid() {
        cellHead.clear()
        for (i in 0 until size) {
            val key = packKey(floor(xs[i] / cellSize).toInt(), floor(ys[i] / cellSize).toInt())
            nextNode[i] = cellHead[key] ?: -1
            cellHead[key] = i
        }
    }

    private fun applyForces() {
        val r2 = rejectionRadius * rejectionRadius
        val invR = 1f / rejectionRadius

        for (i in 0 until size) {
            val px = xs[i]
            val py = ys[i]
            var dx = 0f
            var dy = 0f

            // attraction toward midpoint of two neighbours
            val prev = if (closed) (i - 1 + size) % size else (i - 1).coerceAtLeast(0)
            val next = if (closed) (i + 1) % size else (i + 1).coerceAtMost(size - 1)
            if (prev != i && next != i) {
                val mx = (xs[prev] + xs[next]) * 0.5f
                val my = (ys[prev] + ys[next]) * 0.5f
                dx += (mx - px) * attractionStrength
                dy += (my - py) * attractionStrength
            }

            // rejection from any node within rejectionRadius (skip immediate
            // neighbours so the attraction force isn't fighting them)
            val cellX = floor(px / cellSize).toInt()
            val cellY = floor(py / cellSize).toInt()
            for (cx in cellX - 1..cellX + 1) {
                for (cy in cellY - 1..cellY + 1) {
                    val key = packKey(cx, cy)
                    var node = cellHead[key] ?: -1
                    while (node != -1) {
                        if (node != i && node != prev && node != next) {
                            val ddx = px - xs[node]
                            val ddy = py - ys[node]
                            val d2 = ddx * ddx + ddy * ddy
                            if (d2 in 0f..r2 && d2 > 0f) {
                                val d = sqrt(d2)
                                val f = (rejectionRadius - d) * invR * rejectionStrength
                                dx += (ddx / d) * f
                                dy += (ddy / d) * f
                            }
                        }
                        node = nextNode[node]
                    }

                    // rejection from static obstacle points: steeper falloff
                    // and stronger peak so the wall holds under pressure
                    var obs = obstacleCellHead[key] ?: -1
                    while (obs != -1) {
                        val ddx = px - obstacleX[obs]
                        val ddy = py - obstacleY[obs]
                        val d2 = ddx * ddx + ddy * ddy
                        if (d2 in 0f..r2 && d2 > 0f) {
                            val d = sqrt(d2)
                            val t = (rejectionRadius - d) * invR
                            val f = t * t * obstacleStrength
                            dx += (ddx / d) * f
                            dy += (ddy / d) * f
                        }
                        obs = obstacleNext[obs]
                    }
                }
            }

            // brownian — the random walk that drives growth
            dx += rndGaussian(0f, brownianStrength)
            dy += rndGaussian(0f, brownianStrength)

            nx[i] = px + dx
            ny[i] = py + dy
        }
    }

    private fun commitPositions() {
        System.arraycopy(nx, 0, xs, 0, size)
        System.arraycopy(ny, 0, ys, 0, size)
    }

    private fun splitLongEdges() {
        val maxL2 = maxEdgeLength * maxEdgeLength
        val originalSize = size
        val edgesEnd = if (closed) originalSize else originalSize - 1

        splitMidX.clear()
        splitMidY.clear()
        splitInsertAt.clear()

        for (i in 0 until edgesEnd) {
            val j = if (closed) (i + 1) % originalSize else i + 1
            val ddx = xs[j] - xs[i]
            val ddy = ys[j] - ys[i]
            if (ddx * ddx + ddy * ddy > maxL2) {
                splitMidX.add((xs[i] + xs[j]) * 0.5f)
                splitMidY.add((ys[i] + ys[j]) * 0.5f)
                splitInsertAt.add(i + 1)
            }
        }

        // Insert in reverse so earlier indices stay valid as the array grows.
        for (k in splitInsertAt.indices.reversed()) {
            if (size >= maxNodes) break
            insertNode(splitInsertAt[k], splitMidX[k], splitMidY[k])
        }
    }

    private fun insertNode(idx: Int, x: Float, y: Float) {
        if (size >= maxNodes) return
        if (idx >= size) {
            xs[size] = x
            ys[size] = y
        } else {
            for (k in size downTo idx + 1) {
                xs[k] = xs[k - 1]
                ys[k] = ys[k - 1]
            }
            xs[idx] = x
            ys[idx] = y
        }
        size++
    }

    private fun packKey(ix: Int, iy: Int): Long =
        (ix.toLong() shl 32) or (iy.toLong() and 0xFFFFFFFFL)
}
