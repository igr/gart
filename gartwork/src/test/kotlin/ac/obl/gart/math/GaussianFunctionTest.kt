package ac.obl.gart.math

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GaussianFunctionTest {
	@Test
	fun testGauss() {
		val gauss = GaussianFunction(100, 0, 20)

		assertEquals(0.0f, gauss(-100), 0.01f)
		assertEquals(4.4f, gauss(-50), 0.01f)
		assertEquals(45.78f, gauss(-25), 0.01f)
		assertEquals(100f, gauss(0), 0.01f)
		assertEquals(45.78f, gauss(25), 0.01f)
		assertEquals(4.4f, gauss(50), 0.01f)
		assertEquals(0.0f, gauss(100), 0.01f)

	}
}