package dev.oblac.gart.stipple.util

import kotlin.random.Random

/**
 * A sample point in a Poisson disk distribution.
 * Coordinates are in [0,1] unit square.
 */
data class PoissonSample(
	val x: Float,
	val y: Float,
	val radius: Float,
	val ranking: Float
) : Comparable<PoissonSample> {
	fun distSquared(other: PoissonSample): Float {
		val dx = x - other.x
		val dy = y - other.y
		return dx * dx + dy * dy
	}

	override fun compareTo(other: PoissonSample) = ranking.compareTo(other.ranking)
}

/**
 * Generates a Poisson disk distribution using toroidal dart throwing.
 * Toroidal means the minimum-distance criterion wraps around edges,
 * producing seamlessly tileable distributions.
 */
fun generatePoissonDisk(
	desiredSamples: Int,
	initialRadius: Float = 0.15f,
	attemptsPerRadius: Int = 1000,
	radiusDecreaseFactor: Float = 0.99f,
	seed: Int = 42
): List<PoissonSample> {
	val random = Random(seed)
	val samples = mutableListOf<PoissonSample>()
	var radius = initialRadius

	while (samples.size < desiredSamples && radius > 0f) {
		var attempts = 0
		while (attempts < attemptsPerRadius) {
			attempts++
			val candidate = PoissonSample(
				x = random.nextFloat(),
				y = random.nextFloat(),
				radius = radius,
				ranking = samples.size.toFloat() / desiredSamples
			)
			if (isPoissonCandidateValid(candidate, radius, samples)) {
				samples.add(candidate)
				break
			}
		}
		if (attempts == attemptsPerRadius) {
			radius *= radiusDecreaseFactor
		}
	}
	return samples
}

private fun isPoissonCandidateValid(
	candidate: PoissonSample,
	minDist: Float,
	existing: List<PoissonSample>
): Boolean {
	val minDist2 = minDist * minDist
	val cx = candidate.x
	val cy = candidate.y
	for (sample in existing) {
		val sx = sample.x
		val sy = sample.y
		// Regular distance
		val dx0 = cx - sx
		val dy0 = cy - sy
		if (dx0 * dx0 + dy0 * dy0 < minDist2) return false
		// Toroidal wraps: +-1 on each axis
		val dxp = cx + 1f - sx
		if (dxp * dxp + dy0 * dy0 < minDist2) return false
		val dxn = cx - 1f - sx
		if (dxn * dxn + dy0 * dy0 < minDist2) return false
		val dyp = cy + 1f - sy
		if (dx0 * dx0 + dyp * dyp < minDist2) return false
		val dyn = cy - 1f - sy
		if (dx0 * dx0 + dyn * dyn < minDist2) return false
	}
	return true
}
