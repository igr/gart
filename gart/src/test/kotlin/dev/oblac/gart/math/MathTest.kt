package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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
}
