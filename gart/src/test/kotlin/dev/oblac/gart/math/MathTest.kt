package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MathTest {

	@Test
	fun subDeg() {
		assertEquals(40f, 50f.subDeg(10f), 0.001f)
		assertEquals(340f, (-10f).subDeg(10f), 0.001f)
		assertEquals(355f, (-355f).subDeg(10f), 0.001f)
	}

	@Test
	fun divOrZeroDivides() {
		assertEquals(2f, divOrZero(6f, 3f))
		assertEquals(0.3f / 0.7f, divOrZero(0.3f, 0.7f, 1e-3f))
	}

	@Test
	fun divOrZeroIsZeroWhenTheDivisorIsNotMoreThanEps() {
		assertEquals(0f, divOrZero(1f, 0f))
		assertEquals(0f, divOrZero(1f, -2f))
		assertEquals(0f, divOrZero(1f, 1e-3f, 1e-3f))
		assertEquals(1f / 2e-3f, divOrZero(1f, 2e-3f, 1e-3f))
	}

	@Test
	fun isPowerOfTwoOnlyForPositivePowers() {
		for (n in listOf(1, 2, 4, 64, 1 shl 30)) assertTrue(n.isPowerOfTwo(), "$n")
		// MIN_VALUE is a single bit too, the bit trick alone would let it in, and 0 with it
		for (n in listOf(0, 3, 6, 12, 100, -2, -8, Int.MIN_VALUE, Int.MAX_VALUE)) assertFalse(n.isPowerOfTwo(), "$n")
	}

	@Test
	fun isOddForNegativesToo() {
		// -3 % 2 is -1 on the jvm, not 1
		for (n in listOf(-3, -1, 1, 3, Int.MIN_VALUE + 1, Int.MAX_VALUE)) assertTrue(n.isOdd(), "$n")
		for (n in listOf(-4, -2, 0, 2, Int.MIN_VALUE)) assertFalse(n.isOdd(), "$n")
	}
}
