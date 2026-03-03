package dev.oblac.gart.noise

import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Simplex noise implementation based on Stefan Gustavson's reference.
 * Faster than Perlin in higher dimensions, fewer directional artifacts.
 * Returns values roughly in -1..1 range.
 *
 * Simplex noise uses a grid with integer spacing. Sampling at exact integer coordinates hits the lattice corners, where gradient contributions
 * are highly regular. You get a repeating, structured pattern instead of smooth variation.
 * for smooth noise, use fractional steps to sample between grid points.
 *
 */
object SimplexNoise {

	private val perm = IntArray(512) { permutation[it and 255] }
	private val permMod12 = IntArray(512) { perm[it] % 12 }

	// Gradient vectors for 2D
	private val grad3 = arrayOf(
		doubleArrayOf(1.0, 1.0, 0.0), doubleArrayOf(-1.0, 1.0, 0.0), doubleArrayOf(1.0, -1.0, 0.0),
		doubleArrayOf(-1.0, -1.0, 0.0), doubleArrayOf(1.0, 0.0, 1.0), doubleArrayOf(-1.0, 0.0, 1.0),
		doubleArrayOf(1.0, 0.0, -1.0), doubleArrayOf(-1.0, 0.0, -1.0), doubleArrayOf(0.0, 1.0, 1.0),
		doubleArrayOf(0.0, -1.0, 1.0), doubleArrayOf(0.0, 1.0, -1.0), doubleArrayOf(0.0, -1.0, -1.0)
	)

	// Skewing and unskewing factors for 2D and 3D
	private val F2 = 0.5 * (sqrt(3.0) - 1.0)
	private val G2 = (3.0 - sqrt(3.0)) / 6.0
	private val F3 = 1.0 / 3.0
	private val G3 = 1.0 / 6.0

	private fun dot(g: DoubleArray, x: Double, y: Double): Double = g[0] * x + g[1] * y

	private fun dot(g: DoubleArray, x: Double, y: Double, z: Double): Double = g[0] * x + g[1] * y + g[2] * z

	fun noise(x: Double, y: Double): Double {
		// Skew the input space to determine which simplex cell we're in
		val s = (x + y) * F2
		val i = floor(x + s).toInt()
		val j = floor(y + s).toInt()

		val t = (i + j) * G2
		// Unskew the cell origin back to (x,y) space
		val x0 = x - (i - t)
		val y0 = y - (j - t)

		// Determine which simplex we are in
		val i1: Int
		val j1: Int
		if (x0 > y0) {
			i1 = 1; j1 = 0 // lower triangle, XY order: (0,0)->(1,0)->(1,1)
		} else {
			i1 = 0; j1 = 1 // upper triangle, YX order: (0,0)->(0,1)->(1,1)
		}

		// Offsets for middle corner in (x,y) unskewed coords
		val x1 = x0 - i1 + G2
		val y1 = y0 - j1 + G2
		// Offsets for last corner in (x,y) unskewed coords
		val x2 = x0 - 1.0 + 2.0 * G2
		val y2 = y0 - 1.0 + 2.0 * G2

		// Work out the hashed gradient indices of the three simplex corners
		val ii = i and 255
		val jj = j and 255
		val gi0 = permMod12[ii + perm[jj]]
		val gi1 = permMod12[ii + i1 + perm[jj + j1]]
		val gi2 = permMod12[ii + 1 + perm[jj + 1]]

		// Calculate the contribution from the three corners
		var n0 = 0.0
		var t0 = 0.5 - x0 * x0 - y0 * y0
		if (t0 >= 0) {
			t0 *= t0
			n0 = t0 * t0 * dot(grad3[gi0], x0, y0)
		}

		var n1 = 0.0
		var t1 = 0.5 - x1 * x1 - y1 * y1
		if (t1 >= 0) {
			t1 *= t1
			n1 = t1 * t1 * dot(grad3[gi1], x1, y1)
		}

		var n2 = 0.0
		var t2 = 0.5 - x2 * x2 - y2 * y2
		if (t2 >= 0) {
			t2 *= t2
			n2 = t2 * t2 * dot(grad3[gi2], x2, y2)
		}

		// Scale to [-1, 1]
		return 70.0 * (n0 + n1 + n2)
	}

	fun noise(x: Double, y: Double, z: Double): Double {
		// Skew the input space to determine which simplex cell we're in
		val s = (x + y + z) * F3
		val i = floor(x + s).toInt()
		val j = floor(y + s).toInt()
		val k = floor(z + s).toInt()

		val t = (i + j + k) * G3
		// Unskew the cell origin back to (x,y,z) space
		val x0 = x - (i - t)
		val y0 = y - (j - t)
		val z0 = z - (k - t)

		// Determine which simplex we are in
		val i1: Int; val j1: Int; val k1: Int
		val i2: Int; val j2: Int; val k2: Int
		if (x0 >= y0) {
			if (y0 >= z0) {
				i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0
			} else if (x0 >= z0) {
				i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1
			} else {
				i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1
			}
		} else {
			if (y0 < z0) {
				i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1
			} else if (x0 < z0) {
				i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1
			} else {
				i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0
			}
		}

		// Offsets for second corner in (x,y,z) coords
		val x1 = x0 - i1 + G3
		val y1 = y0 - j1 + G3
		val z1 = z0 - k1 + G3
		// Offsets for third corner
		val x2 = x0 - i2 + 2.0 * G3
		val y2 = y0 - j2 + 2.0 * G3
		val z2 = z0 - k2 + 2.0 * G3
		// Offsets for last corner
		val x3 = x0 - 1.0 + 3.0 * G3
		val y3 = y0 - 1.0 + 3.0 * G3
		val z3 = z0 - 1.0 + 3.0 * G3

		// Work out the hashed gradient indices of the four simplex corners
		val ii = i and 255
		val jj = j and 255
		val kk = k and 255
		val gi0 = permMod12[ii + perm[jj + perm[kk]]]
		val gi1 = permMod12[ii + i1 + perm[jj + j1 + perm[kk + k1]]]
		val gi2 = permMod12[ii + i2 + perm[jj + j2 + perm[kk + k2]]]
		val gi3 = permMod12[ii + 1 + perm[jj + 1 + perm[kk + 1]]]

		// Calculate the contribution from the four corners
		var n0 = 0.0
		var t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0
		if (t0 >= 0) {
			t0 *= t0
			n0 = t0 * t0 * dot(grad3[gi0], x0, y0, z0)
		}

		var n1 = 0.0
		var t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1
		if (t1 >= 0) {
			t1 *= t1
			n1 = t1 * t1 * dot(grad3[gi1], x1, y1, z1)
		}

		var n2 = 0.0
		var t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2
		if (t2 >= 0) {
			t2 *= t2
			n2 = t2 * t2 * dot(grad3[gi2], x2, y2, z2)
		}

		var n3 = 0.0
		var t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3
		if (t3 >= 0) {
			t3 *= t3
			n3 = t3 * t3 * dot(grad3[gi3], x3, y3, z3)
		}

		// Scale to [-1, 1]
		return 32.0 * (n0 + n1 + n2 + n3)
	}
}
