package ac.obl.gart

import ac.obl.gart.gfx.toARGB
import ac.obl.gart.gfx.toRGBA
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColorTest {

	@Test
	fun testRGBAtoARGB() {
		assertEquals(0x44112233, 0x11223344.toARGB())
		assertEquals(0xFF001122.toInt(), 0x001122FF.toARGB())
		assertEquals(0xFF000000.toInt(), 0x00000FF.toARGB())
		assertEquals(0x000000FF, 0x0000FF00.toARGB())
	}

	@Test
	fun testARGBtoRGBA() {
		assertEquals(0x22334411, 0x11223344.toRGBA())
		assertEquals(0xFF001122.toInt(), 0x22FF0011.toRGBA())
		assertEquals(0xFF000000.toInt(), 0x00FF0000.toRGBA())
		assertEquals(0x000000FF, 0xFF000000.toInt().toRGBA())
	}

}