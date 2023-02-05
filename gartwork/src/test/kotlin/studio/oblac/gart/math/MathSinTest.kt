package studio.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.math.sin
import kotlin.test.assertEquals

class MathSinTest {

	@Test
	fun testSin() {
		val msin = MathSin()

		var deg = -720f
		while (deg < 720f) {
			val realSin = sin(deg.toRadian())
			val tableSin = msin[deg]

			assertEquals(realSin, tableSin, 0.002f)

			deg += 0.05f
		}
	}
}
