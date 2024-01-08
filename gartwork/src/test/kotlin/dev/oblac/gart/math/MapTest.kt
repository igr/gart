package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MapTest {

	@Test
	fun testMap() {
		assertEquals(50f, map(25, 0, 100, 0, 200), 0.01f)
		assertEquals(0f, map(25, 0, 50, -200, 200), 0.01f)
	}
}
