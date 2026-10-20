package dev.oblac.gart.math

/**
 * The first 40 primes.
 */
val primes40 = intArrayOf(
    2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67,
    71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139,
    149, 151, 157, 163, 167, 173
)

/**
 * The sieve of Eratosthenes up to [limit]. After the sieve, [isPrime] is one array read.
 */
class PrimeSieve(val limit: Int) {
    private val composite: BooleanArray

    init {
        require(limit >= 1) { "limit must be 1 or more, got $limit" }
        composite = BooleanArray(limit + 1)
        composite[0] = true
        composite[1] = true
        var i = 2
        while (i.toLong() * i <= limit) {
            if (!composite[i]) {
                var j = i.toLong() * i
                while (j <= limit) {
                    composite[j.toInt()] = true
                    j += i
                }
            }
            i++
        }
    }

    /**
     * Tells if [n] is a prime. [n] must be in `0..limit`.
     */
    fun isPrime(n: Int): Boolean = !composite[n]
}
