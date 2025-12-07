package dev.oblac.gart.jfa

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartvas
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.shader.sksl
import org.jetbrains.skia.*

/**
 * Jump Flood Algorithm based object outlining.
 *
 * The JFA is an efficient algorithm for computing distance fields on the GPU.
 * It works by propagating the nearest seed coordinates in O(log N) passes.
 *
 * This implementation:
 * 1. Initializes a buffer with edge pixels as seeds
 * 2. Runs JFA passes to propagate nearest seed coordinates
 * 3. Uses the computed distance field to render outlines
 */
class JfaOutline(
    private val d: Dimension,
    private val outlineWidth: Float = 10f,
) {

    /**
     * Computes the JFA distance field for the given path.
     * Returns the seed buffer image containing encoded nearest seed coordinates.
     */
    private fun computeDistanceField(path: Path): Pair<Image, Image> {
        // Step 1: Draw the path to a temporary buffer
        val sourceBuffer = Gartvas.of(d.w, d.h)
        sourceBuffer.canvas.clear(Color.TRANSPARENT)
        sourceBuffer.canvas.drawPath(path, fillOf(Color.WHITE))
        val sourceImage = sourceBuffer.snapshot()

        // Step 2: Initialize seed buffer with edge pixels
        var currentBuffer = Gartvas.of(d.w, d.h)
        initShader.uniform("resolution", d.wf, d.hf)
        val initFilter = ImageFilter.makeRuntimeShader(
            initShader,
            shaderName = "image",
            input = null
        )
        currentBuffer.canvas.drawImage(sourceImage, 0f, 0f, Paint().apply {
            imageFilter = initFilter
        })

        // Step 3: Run JFA passes
        var stepSize = maxOf(d.w, d.h) / 2f
        while (stepSize >= 1f) {
            val nextBuffer = Gartvas.of(d.w, d.h)
            val currentImage = currentBuffer.snapshot()

            jfaShader.uniform("resolution", d.wf, d.hf)
            jfaShader.uniform("stepSize", stepSize)

            val jfaFilter = ImageFilter.makeRuntimeShader(
                jfaShader,
                shaderName = "seedBuffer",
                input = null
            )

            nextBuffer.canvas.drawImage(currentImage, 0f, 0f, Paint().apply {
                imageFilter = jfaFilter
            })

            currentBuffer = nextBuffer
            stepSize /= 2f
        }

        return Pair(currentBuffer.snapshot(), sourceImage)
    }

    // Initialization shader: marks edge pixels with their coordinates
    private val initShader = """
        uniform shader image;
        uniform float2 resolution;

        // Edge detection threshold
        const float EDGE_THRESHOLD = 0.5;

        half4 main(float2 coord) {
            half4 center = image.eval(coord);

            // Check if this is an edge pixel by comparing with neighbors
            bool isObject = center.a > EDGE_THRESHOLD;
            bool isEdge = false;

            if (isObject) {
                // Check 8 neighbors for edge detection
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        float2 neighborCoord = coord + float2(dx, dy);
                        half4 neighbor = image.eval(neighborCoord);
                        if (neighbor.a <= EDGE_THRESHOLD) {
                            isEdge = true;
                            break;
                        }
                    }
                    if (isEdge) break;
                }
            }

            if (isEdge) {
                // Store normalized coordinates in RG channels
                // We encode coordinates as (coord + 1) / (resolution + 1) to avoid 0
                float2 encoded = (coord + 1.0) / (resolution + 1.0);
                return half4(encoded.x, encoded.y, 1.0, 1.0);
            } else {
                // No seed: store sentinel (0, 0, 0, alpha)
                // Alpha indicates if this is inside the object
                return half4(0.0, 0.0, 0.0, isObject ? 1.0 : 0.0);
            }
        }
    """.sksl()

    // JFA pass shader: propagates nearest seed coordinates
    private val jfaShader = """
        uniform shader seedBuffer;
        uniform float2 resolution;
        uniform float stepSize;

        float2 decodeCoord(half4 pixel) {
            if (pixel.b < 0.5) return float2(-1.0, -1.0); // sentinel
            return pixel.rg * (resolution + 1.0) - 1.0;
        }

        half4 encodeCoord(float2 coord, bool isObject) {
            if (coord.x < 0.0) return half4(0.0, 0.0, 0.0, isObject ? 1.0 : 0.0);
            float2 encoded = (coord + 1.0) / (resolution + 1.0);
            return half4(encoded.x, encoded.y, 1.0, 1.0);
        }

        half4 main(float2 coord) {
            half4 current = seedBuffer.eval(coord);
            bool isObject = current.a > 0.5;
            float2 bestSeed = decodeCoord(current);
            float bestDist = 1e10;

            if (bestSeed.x >= 0.0) {
                bestDist = distance(coord, bestSeed);
            }

            // Check 9 neighbors at stepSize distance
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    float2 neighborCoord = coord + float2(dx, dy) * stepSize;

                    // Clamp to image bounds
                    neighborCoord = clamp(neighborCoord, float2(0.0), resolution - 1.0);

                    half4 neighbor = seedBuffer.eval(neighborCoord);
                    float2 neighborSeed = decodeCoord(neighbor);

                    if (neighborSeed.x >= 0.0) {
                        float dist = distance(coord, neighborSeed);
                        if (dist < bestDist) {
                            bestDist = dist;
                            bestSeed = neighborSeed;
                        }
                    }
                }
            }

            return encodeCoord(bestSeed, isObject);
        }
    """.sksl()

    /**
     * Extracts the outline as a Path at the specified outline width distance.
     * Uses marching squares algorithm to trace the contour.
     */
    fun outlinePath(path: Path): Path {
        val (seedImage, _) = computeDistanceField(path)

        // Read pixels from seed image
        val bitmap = Bitmap()
        bitmap.allocPixels(ImageInfo.makeN32Premul(d.w, d.h))
        seedImage.readPixels(bitmap, 0, 0)

        // Build distance field array
        val distances = FloatArray(d.w * d.h)
        val pixels = bitmap.peekPixels()!!.buffer.bytes

        for (y in 0 until d.h) {
            for (x in 0 until d.w) {
                val idx = (y * d.w + x) * 4
                // N32Premul on little-endian is BGRA format
                val b = (pixels[idx].toInt() and 0xFF) / 255f
                val g = (pixels[idx + 1].toInt() and 0xFF) / 255f
                val r = (pixels[idx + 2].toInt() and 0xFF) / 255f

                val dist = if (b < 0.5f) {
                    Float.MAX_VALUE
                } else {
                    val seedX = r * (d.w + 1) - 1
                    val seedY = g * (d.h + 1) - 1
                    kotlin.math.sqrt((x - seedX) * (x - seedX) + (y - seedY) * (y - seedY))
                }
                distances[y * d.w + x] = dist
            }
        }

        // Marching squares to trace contour at outlineWidth
        return marchingSquares(distances, d.w, d.h, outlineWidth)
    }

    /**
     * Marching squares algorithm to extract contour at a given threshold.
     */
    private fun marchingSquares(field: FloatArray, width: Int, height: Int, threshold: Float): Path {
        val path = Path()

        fun getField(x: Int, y: Int): Float {
            if (x < 0 || x >= width || y < 0 || y >= height) return Float.MAX_VALUE
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
        if (segments.isEmpty()) return path

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

        return path
    }

    private fun dist(a: Pair<Float, Float>, b: Pair<Float, Float>): Float {
        val dx = a.first - b.first
        val dy = a.second - b.second
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
