package dev.oblac.gart.noise

import dev.oblac.gart.math.primes40

// The optimal weights used for scrambling of the first 40 dimension (for each prime).
private val WEIGHTS = intArrayOf(
    1, 2, 3, 3, 8, 11, 12, 14, 7, 18, 12, 13, 17, 18, 29, 14, 18, 43, 41,
    44, 40, 30, 47, 65, 71, 28, 40, 60, 79, 89, 56, 50, 52, 61, 108, 56,
    66, 63, 60, 66
)

/**
 * Implementation of a Halton sequence.
 * <p>
 * A Halton sequence is a low-discrepancy sequence generating points in the interval [0, 1] according to
 * <pre>
 *   H(n) = d_0 / b + d_1 / b^2 .... d_j / b^j+1
 *
 *   with
 *
 *   n = d_j * b^j-1 + ... d_1 * b + d_0 * b^0
 * </pre>
 *
 * For higher dimensions, subsequent prime numbers are used as base, e.g. { 2, 3, 5 } for a Halton sequence in R^3.
 * <p>
 * Halton sequences are known to suffer from linear correlation for larger prime numbers, thus the individual digits
 * are usually scrambled. This implementation already comes with support for up to 40 dimensions with optimal weight
 * numbers from <a href="http://etd.lib.fsu.edu/theses/available/etd-07062004-140409/unrestricted/dissertation1.pdf">
 * H. Chi: Scrambled quasirandom sequences and their applications</a>.
 */
class HaltonSequenceGenerator(
    private val dimension: Int,             // the space dimension (1 to bases.size)
    private val base: IntArray = primes40,  // the base number for each dimension, entries should be (pairwise) prime
    private val weight: IntArray? = WEIGHTS // the weights used during scrambling, may be null in which case no scrambling will be performed
) {

    private var count = 0       // The current index in the sequence.

    init {
        if (dimension < 1 || dimension > base.size) {
            throw IllegalArgumentException("Invalid dimension: $dimension not in [1, ${primes40.size}]")
        }
        if (weight != null && weight.size != base.size) {
            throw IllegalStateException("Invalid weights dimension: ${weight.size} != ${base.size}")
        }
    }

    /**
     * Returns Halton number for given index.
     */
    fun get(): DoubleArray {
        val v = DoubleArray(dimension)
        for (i in 0 until dimension) {
            var index = count
            var f = 1.0 / base[i]

            val j = 0   // not used
            while (index > 0) {
                val digit = scramble(i, j, base[i], index % base[i])
                v[i] += f * digit
                index /= base[i]            // floor(index / base)
                f /= base[i].toDouble()
            }
        }
        count++
        return v
    }

    /**
     * Performs scrambling of digit `d_j` according to the formula: `(weight_i * d_j) mod base`
     * Override this to do a different scrambling.
     *
     * @param i the dimension index
     * @param j the digit index
     * @param b the base for this dimension
     * @param digit the j-th digit
     * @return the scrambled digit
     */
    private fun scramble(i: Int, j: Int, b: Int, digit: Int): Int {
        return if (weight != null) weight[i] * digit % b else digit
    }

    /**
     * Skip to the i-th point in the Halton sequence.
     * This operation can be performed in O(1).
     *
     * @param index the index in the sequence to skip to
     * @return the i-th point in the Halton sequence
     */
    fun skipTo(index: Int): DoubleArray {
        count = index
        return get()
    }

    /**
     * Returns the index i of the next point in the Halton sequence that will be returned
     * by calling [.get].
     *
     * @return the index of the next point
     */
    fun nextIndex() = count
}
