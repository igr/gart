package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.test.assertEquals

class MathCosTest {
	@Test
	fun testCos() {
		val mcos = MathCos()

		var deg = -720f
		while (deg < 720f) {
			val realCos = cos(deg.toRadian())
			val tableCos = mcos[deg]

			assertEquals(realCos, tableCos, 0.002f)

			deg += 0.05f
		}

	}
}
