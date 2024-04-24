package dev.oblac.gart.gfx

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColorTest {

	@Test
	fun testRGB() {
		assertEquals(0xFF112233.toInt(), rgb(0x11, 0x22, 0x33))
	}

	@Test
	fun testRGBAtoARGB() {
        assertEquals(0x44112233, 0x11223344.convertRGBAtoARGB())
        assertEquals(0xFF001122.toInt(), 0x001122FF.convertRGBAtoARGB())
        assertEquals(0xFF000000.toInt(), 0x00000FF.convertRGBAtoARGB())
        assertEquals(0x000000FF, 0x0000FF00.convertRGBAtoARGB())
	}

	@Test
	fun testARGBtoRGBA() {
        assertEquals(0x22334411, 0x11223344.covertARGBtoRGBA())
        assertEquals(0xFF001122.toInt(), 0x22FF0011.covertARGBtoRGBA())
        assertEquals(0xFF000000.toInt(), 0x00FF0000.covertARGBtoRGBA())
        assertEquals(0x000000FF, 0xFF000000.toInt().covertARGBtoRGBA())
	}

}
