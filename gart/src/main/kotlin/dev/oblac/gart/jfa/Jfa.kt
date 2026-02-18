package dev.oblac.gart.jfa

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartvas
import dev.oblac.gart.gfx.fillOf
import org.jetbrains.skia.Color
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import kotlin.math.sqrt

/**
 * CPU-based Jump Flood Algorithm implementation.
 *
 * The JFA is an efficient algorithm for computing distance fields.
 * It works by propagating the nearest seed coordinates in O(log N) passes.
 */
class Jfa(private val d: Dimension) {

    /**
     * Computes the distance field for a given path.
     * Returns a 2D array of distances from each pixel to the nearest edge of the path.
     * Also returns a boolean array indicating which pixels are inside the path.
     */
    fun computeDistanceField(path: Path): JfaResult {
        val width = d.w
        val height = d.h

        // Step 1: Rasterize the path to determine inside/outside
        val inside = BooleanArray(width * height)
        val sourceBuffer = Gartvas.of(width, height)
        sourceBuffer.canvas.clear(Color.TRANSPARENT)
        sourceBuffer.canvas.drawPath(path, fillOf(Color.WHITE))

        val bitmap = sourceBuffer.createBitmap()
        sourceBuffer.surface.readPixels(bitmap, 0, 0)
        val pixels = bitmap.peekPixels()!!.buffer.bytes

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = (y * width + x) * 4
                val a = (pixels[idx + 3].toInt() and 0xFF)
                inside[y * width + x] = a > 127
            }
        }

        // Step 2: Initialize seed buffer with edge pixels
        // seedX[i], seedY[i] = coordinates of nearest seed for pixel i
        // -1 means no seed found yet
        val seedX = IntArray(width * height) { -1 }
        val seedY = IntArray(width * height) { -1 }

        // Find edge pixels (inside pixels with at least one outside neighbor)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                if (inside[idx]) {
                    // Check 8 neighbors for edge detection
                    var isEdge = false
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = x + dx
                            val ny = y + dy
                            if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                                isEdge = true
                                break
                            }
                            if (!inside[ny * width + nx]) {
                                isEdge = true
                                break
                            }
                        }
                        if (isEdge) break
                    }
                    if (isEdge) {
                        seedX[idx] = x
                        seedY[idx] = y
                    }
                }
            }
        }

        // Step 3: Run JFA passes
        var stepSize = maxOf(width, height) / 2
        while (stepSize >= 1) {
            val newSeedX = seedX.copyOf()
            val newSeedY = seedY.copyOf()

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val idx = y * width + x
                    var bestDist = if (seedX[idx] >= 0) {
                        dist(x, y, seedX[idx], seedY[idx])
                    } else {
                        Float.MAX_VALUE
                    }
                    var bestSeedX = seedX[idx]
                    var bestSeedY = seedY[idx]

                    // Check 9 neighbors at stepSize distance
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val nx = (x + dx * stepSize).coerceIn(0, width - 1)
                            val ny = (y + dy * stepSize).coerceIn(0, height - 1)
                            val nidx = ny * width + nx

                            if (seedX[nidx] >= 0) {
                                val d = dist(x, y, seedX[nidx], seedY[nidx])
                                if (d < bestDist) {
                                    bestDist = d
                                    bestSeedX = seedX[nidx]
                                    bestSeedY = seedY[nidx]
                                }
                            }
                        }
                    }

                    newSeedX[idx] = bestSeedX
                    newSeedY[idx] = bestSeedY
                }
            }

            for (i in seedX.indices) {
                seedX[i] = newSeedX[i]
                seedY[i] = newSeedY[i]
            }

            stepSize /= 2
        }

        // Step 4: Compute final distances
        val distances = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                distances[idx] = if (seedX[idx] >= 0) {
                    dist(x, y, seedX[idx], seedY[idx])
                } else {
                    Float.MAX_VALUE
                }
            }
        }

        return JfaResult(distances, inside, width, height)
    }

    /**
     * Extracts the outline as a Path at the specified distance.
     *
     * @param path The input path to outline
     * @param outlineWidth The distance from the edge to trace the outline
     * @param outerOnly If true, only traces the outer contour (outside the closed path)
     */
    fun outlinePath(path: Path, outlineWidth: Float, outerOnly: Boolean = false): Path {
        val result = computeDistanceField(path)
        return result.tracePath(outlineWidth, outerOnly)
    }

    private fun dist(x1: Int, y1: Int, x2: Int, y2: Int): Float {
        val dx = (x1 - x2).toFloat()
        val dy = (y1 - y2).toFloat()
        return sqrt(dx * dx + dy * dy)
    }
}

/**
 * Result of JFA computation containing distance field and inside/outside information.
 */
class JfaResult(
    val distances: FloatArray,
    val inside: BooleanArray,
    val width: Int,
    val height: Int
) {
    /**
     * Traces a path at the specified distance from the edge.
     *
     * @param threshold The distance at which to trace the contour
     * @param outerOnly If true, only traces the outer contour
     */
    fun tracePath(threshold: Float, outerOnly: Boolean = false): Path {
        val field = if (outerOnly) {
            // Set inside pixels to 0 so contour is only traced outside
            FloatArray(distances.size) { i ->
                if (inside[i]) 0f else distances[i]
            }
        } else {
            distances
        }

        return marchingSquares(field, width, height, threshold)
    }

    private fun marchingSquares(field: FloatArray, width: Int, height: Int, threshold: Float): Path {
        val path = PathBuilder()

        fun getField(x: Int, y: Int): Float {
            if (x !in 0 until width || y !in 0 until height) return Float.MAX_VALUE
            return field[y * width + x]
        }

        fun interpolate(x1: Int, y1: Int, x2: Int, y2: Int): Pair<Float, Float> {
            val v1 = getField(x1, y1)
            val v2 = getField(x2, y2)
            if (kotlin.math.abs(v2 - v1) < 0.0001f) {
                return Pair((x1 + x2) / 2f, (y1 + y2) / 2f)
            }
            val t = (threshold - v1) / (v2 - v1)
            return Pair(x1 + t * (x2 - x1), y1 + t * (y2 - y1))
        }

        // Find all contour segments
        val segments = mutableListOf<Pair<Pair<Float, Float>, Pair<Float, Float>>>()

        for (y in 0 until height - 1) {
            for (x in 0 until width - 1) {
                val v0 = getField(x, y)
                val v1 = getField(x + 1, y)
                val v2 = getField(x + 1, y + 1)
                val v3 = getField(x, y + 1)

                val case = ((if (v0 < threshold) 1 else 0) or
                    (if (v1 < threshold) 2 else 0) or
                    (if (v2 < threshold) 4 else 0) or
                    (if (v3 < threshold) 8 else 0))

                when (case) {
                    1, 14 -> segments.add(interpolate(x, y, x + 1, y) to interpolate(x, y, x, y + 1))
                    2, 13 -> segments.add(interpolate(x, y, x + 1, y) to interpolate(x + 1, y, x + 1, y + 1))
                    3, 12 -> segments.add(interpolate(x, y, x, y + 1) to interpolate(x + 1, y, x + 1, y + 1))
                    4, 11 -> segments.add(interpolate(x + 1, y, x + 1, y + 1) to interpolate(x, y + 1, x + 1, y + 1))
                    5 -> {
                        segments.add(interpolate(x, y, x + 1, y) to interpolate(x, y, x, y + 1))
                        segments.add(interpolate(x + 1, y, x + 1, y + 1) to interpolate(x, y + 1, x + 1, y + 1))
                    }
                    6, 9 -> segments.add(interpolate(x, y, x + 1, y) to interpolate(x, y + 1, x + 1, y + 1))
                    7, 8 -> segments.add(interpolate(x, y, x, y + 1) to interpolate(x, y + 1, x + 1, y + 1))
                    10 -> {
                        segments.add(interpolate(x, y, x + 1, y) to interpolate(x + 1, y, x + 1, y + 1))
                        segments.add(interpolate(x, y, x, y + 1) to interpolate(x, y + 1, x + 1, y + 1))
                    }
                }
            }
        }

        // Connect segments into paths
        if (segments.isEmpty()) return path.detach()

        val remaining = segments.toMutableList()
        while (remaining.isNotEmpty()) {
            val chain = mutableListOf<Pair<Float, Float>>()
            val first = remaining.removeAt(0)
            chain.add(first.first)
            chain.add(first.second)

            var changed = true
            while (changed) {
                changed = false
                val iter = remaining.iterator()
                while (iter.hasNext()) {
                    val seg = iter.next()
                    val last = chain.last()
                    val firstPt = chain.first()

                    if (dist(seg.first, last) < 1.5f) {
                        chain.add(seg.second)
                        iter.remove()
                        changed = true
                    } else if (dist(seg.second, last) < 1.5f) {
                        chain.add(seg.first)
                        iter.remove()
                        changed = true
                    } else if (dist(seg.first, firstPt) < 1.5f) {
                        chain.add(0, seg.second)
                        iter.remove()
                        changed = true
                    } else if (dist(seg.second, firstPt) < 1.5f) {
                        chain.add(0, seg.first)
                        iter.remove()
                        changed = true
                    }
                }
            }

            // Add chain to path
            if (chain.size >= 2) {
                path.moveTo(chain[0].first, chain[0].second)
                for (i in 1 until chain.size) {
                    path.lineTo(chain[i].first, chain[i].second)
                }
                // Close if endpoints are close
                if (dist(chain.first(), chain.last()) < 2f) {
                    path.closePath()
                }
            }
        }

        return path.detach()
    }

    private fun dist(a: Pair<Float, Float>, b: Pair<Float, Float>): Float {
        val dx = a.first - b.first
        val dy = a.second - b.second
        return sqrt(dx * dx + dy * dy)
    }
}
