package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GaussianIntTest {

    @Test
    fun equalPartsAreEqualValues() {
        assertEquals(GaussianInt(-7, 2), GaussianInt(-7, 2))
        assertEquals(setOf(GaussianInt(1, -1)), hashSetOf(GaussianInt(1, -1), GaussianInt(1, -1)))
    }

    @Test
    fun timesMatchesComplex() {
        val a = GaussianInt(3, -2)
        val b = GaussianInt(-5, 7)
        assertEquals(a.toComplex() * b.toComplex(), (a * b).toComplex())
    }

    @Test
    fun timesIGoesRoundInFourLeftTurns() {
        var d = GaussianInt.ONE
        val turns = List(4) {
            d *= GaussianInt.I
            d
        }
        assertEquals(listOf(GaussianInt.I, -GaussianInt.ONE, -GaussianInt.I, GaussianInt.ONE), turns)
    }

    @Test
    fun plusAndMinusUndoEachOther() {
        val a = GaussianInt(12, -40)
        val b = GaussianInt(-3, 9)
        assertEquals(GaussianInt(9, -31), a + b)
        assertEquals(a, a + b - b)
    }

    @Test
    fun normIsTheValueTimesItsConjugate() {
        val z = GaussianInt(3, -4)
        assertEquals(25L, z.norm())
        assertEquals(GaussianInt(25, 0), z * z.conjugate())
    }

    @Test
    fun normOfBigPartsFitsTheLong() {
        assertEquals(2L * Int.MAX_VALUE * Int.MAX_VALUE, GaussianInt(Int.MAX_VALUE, -Int.MAX_VALUE).norm())
    }

    @Test
    fun isPrimeMatchesTrialDivisionNearZero() {
        // the slow way: no divisor with a norm between 1 and the norm of z
        fun divides(w: GaussianInt, z: GaussianInt): Boolean {
            val q = z * w.conjugate()
            val n = w.norm().toInt()
            return q.re % n == 0 && q.im % n == 0
        }
        val box = -12..12
        val all = box.flatMap { re -> box.map { im -> GaussianInt(re, im) } }
        val primes = PrimeSieve(2 * 12 * 12)
        for (z in all) {
            val n = z.norm()
            val slow = n > 1 && all.none { w -> w.norm() in 2 until n && divides(w, z) }
            assertEquals(slow, z.isPrime(primes), "$z")
        }
    }

    @Test
    fun isPrimeKnowsTheSmallCases() {
        val primes = PrimeSieve(100)
        assertTrue(GaussianInt(1, 1).isPrime(primes)) // norm 2
        assertTrue(GaussianInt(0, -7).isPrime(primes)) // 7 is 4k + 3
        assertFalse(GaussianInt(5, 0).isPrime(primes)) // (2 + i)(2 - i)
        assertFalse(GaussianInt(2, 0).isPrime(primes)) // -i (1 + i)^2
        assertFalse(GaussianInt.ONE.isPrime(primes))
        assertFalse(GaussianInt.ZERO.isPrime(primes))
    }

    @Test
    fun isPrimeFailsWhenTheSieveIsTooShort() {
        assertFailsWith<IllegalArgumentException> { GaussianInt(9, 10).isPrime(PrimeSieve(100)) }
    }

    @Test
    fun toStringReadsLikeANumber() {
        assertEquals("3+4i", GaussianInt(3, 4).toString())
        assertEquals("3-4i", GaussianInt(3, -4).toString())
        assertEquals("-2i", GaussianInt(0, -2).toString())
        assertEquals("5", GaussianInt(5, 0).toString())
        assertEquals("0", GaussianInt.ZERO.toString())
    }
}
