package dev.oblac.gart.stipple

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.luminance
import dev.oblac.gart.color.space.of
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.Point
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Weighted Voronoi Stippling using Lloyd relaxation.
 *
 * Places stipple points where density follows image darkness.
 * Each iteration computes a rasterized Voronoi diagram and moves
 * each point to the weighted centroid of its cell, where weights
 * are the pixel darkness values.
 *
 * @param pixels source image (darkness drives density)
 * @param pointCount number of stipple points
 * @param iterations number of Lloyd relaxation iterations
 * @param gamma density curve exponent; higher values concentrate dots in darker areas (default 1.0)
 * @param brightnessThreshold pixels brighter than this (0-1) are ignored during centroid calculation (default 0.95)
 * @param overshoot velocity factor for centroid movement; values > 1 speed up convergence (default 1.8)
 * @param initialJitter jitter for initial point placement (0-1); decays over iterations
 * @param seed PRNG seed for reproducible output
 * @return list of stipple point positions
 */
fun stippleVoronoi(
	pixels: Pixels,
	pointCount: Int = 5000,
	iterations: Int = 50,
	gamma: Float = 1.0f,
	brightnessThreshold: Float = 0.95f,
	overshoot: Float = 1.8f,
	initialJitter: Float = 0.5f,
	seed: Int = 42
): List<Point> {
	val w = pixels.d.w
	val h = pixels.d.h
	val rng = Random(seed)

	// Pre-compute density map (darkness 0..1, where 1 = black)
	val density = FloatArray(w * h)
	var totalDensity = 0.0
	for (y in 0 until h) {
		for (x in 0 until w) {
			val lum = Color4f.of(pixels[x, y]).luminance
			// Skip pixels brighter than threshold
			val d = if (lum > brightnessThreshold) 0f
			else (1f - lum).pow(gamma)
			density[y * w + x] = d
			totalDensity += d
		}
	}

	// Phase 1: Initial point sampling via rejection sampling weighted by density
	val points = initialSampling(w, h, density, totalDensity, pointCount, rng)

	// Phase 2: Lloyd relaxation
	for (iter in 0 until iterations) {
		val jitter = initialJitter * (1f - iter.toFloat() / iterations)
		lloydRelaxation(points, w, h, density, overshoot, jitter, rng)
	}

	return points.map { Point(it[0], it[1]) }
}

/**
 * Places initial points using weighted rejection sampling.
 * Points are more likely to be placed in darker (higher density) areas.
 */
private fun initialSampling(
	w: Int, h: Int,
	density: FloatArray,
	totalDensity: Double,
	count: Int,
	rng: Random
): Array<FloatArray> {
	val points = Array(count) { FloatArray(2) }

	// Build cumulative density for fast weighted sampling
	val cumulative = DoubleArray(w * h)
	cumulative[0] = density[0].toDouble()
	for (i in 1 until density.size) {
		cumulative[i] = cumulative[i - 1] + density[i]
	}

	for (i in 0 until count) {
		val target = rng.nextDouble() * totalDensity
		var idx = cumulative.binarySearch(target)
		if (idx < 0) idx = -(idx + 1)
		idx = idx.coerceIn(0, w * h - 1)

		val y = idx / w
		val x = idx % w
		// Add sub-pixel jitter
		points[i][0] = (x + rng.nextFloat()).coerceIn(0f, w - 1f)
		points[i][1] = (y + rng.nextFloat()).coerceIn(0f, h - 1f)
	}

	return points
}

/**
 * One iteration of Lloyd relaxation.
 * Assigns each pixel to its nearest point, computes weighted centroids,
 * and moves points toward their centroids.
 */
private fun lloydRelaxation(
	points: Array<FloatArray>,
	w: Int, h: Int,
	density: FloatArray,
	overshoot: Float,
	jitter: Float,
	rng: Random
) {
	val n = points.size

	// Per-cell accumulators: weighted sum of x, y and total weight
	val sumX = DoubleArray(n)
	val sumY = DoubleArray(n)
	val sumW = DoubleArray(n)

	// Grid acceleration for nearest-point lookup
	val gridSize = max(1, sqrt(n.toFloat()).toInt())
	val cellW = w.toFloat() / gridSize
	val cellH = h.toFloat() / gridSize
	val grid = Array(gridSize * gridSize) { mutableListOf<Int>() }

	for (i in 0 until n) {
		val gx = (points[i][0] / cellW).toInt().coerceIn(0, gridSize - 1)
		val gy = (points[i][1] / cellH).toInt().coerceIn(0, gridSize - 1)
		grid[gy * gridSize + gx].add(i)
	}

	// For each pixel, find nearest point and accumulate
	for (py in 0 until h) {
		for (px in 0 until w) {
			val d = density[py * w + px]
			if (d < 0.001f) continue // skip near-white pixels

			val nearest = findNearest(points, grid, gridSize, cellW, cellH, px.toFloat(), py.toFloat())
			sumX[nearest] += px.toDouble() * d
			sumY[nearest] += py.toDouble() * d
			sumW[nearest] += d.toDouble()
		}
	}

	// Move points toward weighted centroids with overshoot
	for (i in 0 until n) {
		if (sumW[i] > 0) {
			val cx = (sumX[i] / sumW[i]).toFloat()
			val cy = (sumY[i] / sumW[i]).toFloat()

			// Overshoot: move past centroid by the overshoot factor
			val ox = points[i][0] + (cx - points[i][0]) * overshoot
			val oy = points[i][1] + (cy - points[i][1]) * overshoot

			if (jitter > 0f) {
				points[i][0] = (ox + (rng.nextFloat() - 0.5f) * jitter).coerceIn(0f, w - 1f)
				points[i][1] = (oy + (rng.nextFloat() - 0.5f) * jitter).coerceIn(0f, h - 1f)
			} else {
				points[i][0] = ox.coerceIn(0f, w - 1f)
				points[i][1] = oy.coerceIn(0f, h - 1f)
			}
		}
	}
}

/**
 * Finds the index of the nearest point to (px, py) using grid acceleration.
 */
private fun findNearest(
	points: Array<FloatArray>,
	grid: Array<MutableList<Int>>,
	gridSize: Int,
	cellW: Float, cellH: Float,
	px: Float, py: Float
): Int {
	val gx = (px / cellW).toInt().coerceIn(0, gridSize - 1)
	val gy = (py / cellH).toInt().coerceIn(0, gridSize - 1)

	var bestDist = Float.MAX_VALUE
	var bestIdx = 0

	// Search in expanding rings until we're sure we found the closest
	for (ring in 0 until gridSize) {
		val x0 = max(0, gx - ring)
		val x1 = min(gridSize - 1, gx + ring)
		val y0 = max(0, gy - ring)
		val y1 = min(gridSize - 1, gy + ring)

		for (cy in y0..y1) {
			for (cx in x0..x1) {
				// Only check cells on the ring border (skip interior on ring > 0)
				if (ring > 0 && cx > x0 && cx < x1 && cy > y0 && cy < y1) continue

				for (idx in grid[cy * gridSize + cx]) {
					val dx = points[idx][0] - px
					val dy = points[idx][1] - py
					val dist = dx * dx + dy * dy
					if (dist < bestDist) {
						bestDist = dist
						bestIdx = idx
					}
				}
			}
		}

		// If we found a point and the ring distance exceeds our best,
		// no closer point can exist in further rings
		if (bestDist < Float.MAX_VALUE) {
			val ringDist = ring * min(cellW, cellH)
			if (ringDist * ringDist > bestDist) break
		}
	}

	return bestIdx
}
