package ac.obl.gart.math

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MathTest {

	@Test
	fun subDeg() {
		assertEquals(40f, 50f.subDeg(10f), 0.001f)
		assertEquals(340f, (-10f).subDeg(10f), 0.001f)
		assertEquals(355f, (-355f).subDeg(10f), 0.001f)
	}
}