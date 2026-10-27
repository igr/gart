package dev.oblac.gart.math

import dev.oblac.gart.vector.Vec3

/**
 * A 3D grid of floats: [nx] x [ny] x [nz] samples, [step] apart, the first one at [origin]. For a
 * field that is costly to compute and that gets read many times, like the density of a volume
 * that the light and the view both march through.
 *
 * The samples go as in a 2D image, row by row, and each sample of a row is a run of [nz] along z:
 * z is the fastest, then x, then y. So a band of y rows is one block of memory, as
 * `parallelForRows` gives them out, and a ray along z reads memory in order. [idx] gives the
 * index into [values].
 */
class Grid3(val step: Float, val origin: Vec3, val nx: Int, val ny: Int, val nz: Int) {
    init {
        require(nx >= 2 && ny >= 2 && nz >= 2) { "a grid needs 2 samples or more on each axis, not $nx x $ny x $nz" }
        require(step > 0f) { "step must be > 0" }
    }

    val values = FloatArray(nx * ny * nz)

    fun idx(i: Int, j: Int, k: Int) = (j * nx + i) * nz + k

    /** Trilinear between the 8 samples round [p]. Reads outside the grid clamp to its edge. */
    fun at(p: Vec3): Float {
        val d = p - origin
        val fx = (d.x / step).coerceIn(0f, nx - 1.001f)
        val fy = (d.y / step).coerceIn(0f, ny - 1.001f)
        val fz = (d.z / step).coerceIn(0f, nz - 1.001f)
        val i = fx.toInt()
        val j = fy.toInt()
        val k = fz.toInt()
        val tz = fz - k
        fun l(a: Int, b: Int): Float {
            val o = idx(a, b, k)
            return values[o] + (values[o + 1] - values[o]) * tz
        }
        val a = l(i, j) + (l(i + 1, j) - l(i, j)) * (fx - i)
        val b = l(i, j + 1) + (l(i + 1, j + 1) - l(i, j + 1)) * (fx - i)
        return a + (b - a) * (fy - j)
    }
}
