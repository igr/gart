package dev.oblac.gart.math

import kotlin.math.abs

/**
 * A Gaussian integer `re + im i`, a point on the integer lattice.
 *
 * [Complex] holds doubles. This type holds two Ints, so the arithmetic is exact.
 * A multiplication by [I] turns a value a quarter turn to the left.
 * The parts overflow the same as an Int.
 *
 * It is a plain data class. In a hot loop the JIT removes the objects, so a walk is as fast
 * as with two Ints packed in a Long.
 */
data class GaussianInt(val re: Int, val im: Int) {

    operator fun unaryMinus() = GaussianInt(-re, -im)
    operator fun plus(z: GaussianInt) = GaussianInt(re + z.re, im + z.im)
    operator fun minus(z: GaussianInt) = GaussianInt(re - z.re, im - z.im)
    operator fun times(z: GaussianInt) = GaussianInt(re * z.re - im * z.im, re * z.im + im * z.re)

    fun conjugate() = GaussianInt(re, -im)

    /**
     * The norm `re² + im²`, as a Long. It is not [Complex.norm], which is the length `|z|`.
     */
    fun norm(): Long = re.toLong() * re + im.toLong() * im

    /**
     * Tells if this is a Gaussian prime. Off the axes, it is a prime when its norm is a prime.
     * On an axis, the part that is not 0 must be a prime of the form `4k + 3`, with any sign.
     * The [primes] sieve must reach the norm.
     */
    fun isPrime(primes: PrimeSieve): Boolean {
        val x = abs(re)
        val y = abs(im)
        if (x == 0) return y % 4 == 3 && primes.isPrime(y)
        if (y == 0) return x % 4 == 3 && primes.isPrime(x)
        val n = norm()
        require(n <= primes.limit) { "the sieve stops at ${primes.limit}, but the norm of $this is $n" }
        return primes.isPrime(n.toInt())
    }

    fun toComplex() = Complex(re, im)

    override fun toString() = when {
        im == 0 -> "$re"
        re == 0 -> "${im}i"
        im < 0 -> "$re${im}i"
        else -> "$re+${im}i"
    }

    companion object {
        val ZERO = GaussianInt(0, 0)
        val ONE = GaussianInt(1, 0)
        val I = GaussianInt(0, 1)
    }
}
