package dev.oblac.gart.noise

import kotlin.math.floor
import kotlin.random.Random

object Perlin {
    
    private val p = IntArray(512) {
		if (it < 256) permutation[it] else permutation[it - 256]
	}

    /**
     * Returns a random value between 0 and 1.
     */
    fun noise(): Double {
        return noise(
            Random.nextDouble(-1.0, 1.0),
            Random.nextDouble(-1.0, 1.0),
            Random.nextDouble(-1.0, 1.0),
        ) / 2 + 0.5
    }

    fun noise(x: Double, y: Double, z: Double): Double {
		// Find unit cube that contains point
		val xi = floor(x).toInt() and 255
		val yi = floor(y).toInt() and 255
		val zi = floor(z).toInt() and 255

		// Find relative x, y, z of point in cube
		val xx = x - floor(x)
		val yy = y - floor(y)
		val zz = z - floor(z)

		// Compute fade curves for each of xx, yy, zz
		val u = fade(xx)
		val v = fade(yy)
		val w = fade(zz)

		// Hash co-ordinates of the 8 cube corners
		// and add blended results from 8 corners of cube

		val a = p[xi] + yi
		val aa = p[a] + zi
		val ab = p[a + 1] + zi
		val b = p[xi + 1] + yi
		val ba = p[b] + zi
		val bb = p[b + 1] + zi

		return lerp(
			w, lerp(
				v, lerp(
					u, grad(p[aa], xx, yy, zz),
					grad(p[ba], xx - 1, yy, zz)
				),
				lerp(
					u, grad(p[ab], xx, yy - 1, zz),
					grad(p[bb], xx - 1, yy - 1, zz)
				)
			),
			lerp(
				v, lerp(
					u, grad(p[aa + 1], xx, yy, zz - 1),
					grad(p[ba + 1], xx - 1, yy, zz - 1)
				),
				lerp(
					u, grad(p[ab + 1], xx, yy - 1, zz - 1),
					grad(p[bb + 1], xx - 1, yy - 1, zz - 1)
				)
			)
		)
	}

	private fun fade(t: Double) = t * t * t * (t * (t * 6 - 15) + 10)

	private fun lerp(t: Double, a: Double, b: Double) = a + t * (b - a)

	private fun grad(hash: Int, x: Double, y: Double, z: Double): Double {
		// Convert low 4 bits of hash code into 12 gradient directions
		val h = hash and 15
		val u = if (h < 8) x else y
		val v = if (h < 4) y else if (h == 12 || h == 14) x else z
		return (if ((h and 1) == 0) u else -u) +
			(if ((h and 2) == 0) v else -v)
	}
}
