package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrimeSieveTest {

    @Test
    fun findsTheFirstFortyPrimes() {
        val sieve = PrimeSieve(173)
        assertContentEquals(primes40, (0..173).filter { sieve.isPrime(it) }.toIntArray())
    }

    @Test
    fun zeroAndOneAreNotPrimes() {
        val sieve = PrimeSieve(1)
        assertFalse(sieve.isPrime(0))
        assertFalse(sieve.isPrime(1))
    }

    @Test
    fun theLimitItselfIsSieved() {
        assertTrue(PrimeSieve(7919).isPrime(7919))
        assertFalse(PrimeSieve(7917).isPrime(7917)) // 3 x 7 x 13 x 29
        assertFalse(PrimeSieve(169).isPrime(169)) // 13 x 13, the square of the last sieving prime
    }

    @Test
    fun numbersPastTheLimitFail() {
        assertFailsWith<IndexOutOfBoundsException> { PrimeSieve(10).isPrime(11) }
        assertFailsWith<IllegalArgumentException> { PrimeSieve(0) }
    }
}
