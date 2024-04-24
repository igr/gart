package dev.oblac.gart.gfx

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RGBATest {

	@Test
	fun testCreateRGB() {
		val rgba = RGBA.of(0x11223344)

		assertEquals(0x11, rgba.a)
		assertEquals(0x22, rgba.r)
		assertEquals(0x33, rgba.g)
		assertEquals(0x44, rgba.b)
	}

	@Test
	fun testConvertToHSL() {
		val rgba = RGBA.of(0x11223344)

		val hsl = rgba.toHSL()

		assertEquals(6.66f, hsl.a, 0.01f)
		assertEquals(210.0f, hsl.h, 0.01f)
		assertEquals(33.33f, hsl.s, 0.01f)
		assertEquals(20.00f, hsl.l, 0.01f)
	}

    @Test
    fun testConverts() {
        for (i in 0xFF000000..0xFFFFFFFF) {
            val rgba = RGBA.of(i)
            val hsl = rgba.toHSL()
            val rgba2 = hsl.toRGBA()

            assertEquals(rgba.r.toDouble(), rgba2.r.toDouble(), 1.0)
            assertEquals(rgba.g.toDouble(), rgba2.g.toDouble(), 1.0)
            assertEquals(rgba.b.toDouble(), rgba2.b.toDouble(), 1.0)
            assertEquals(rgba.a.toDouble(), rgba2.a.toDouble(), 1.0)
        }
    }

	@Test
	fun testConvertToHSLColors() {
		val rgbs = arrayOf(
			RGBA.of(0xFF000000),
			RGBA.of(0xFFFFFFFF),
			RGBA.of(0xFFFF0000),
			RGBA.of(0xFFFFFF00),
		)
		val hsls = arrayOf(
			HSLA(0f, 0f, 0f),
			HSLA(0f, 0f, 100f ),
			HSLA(0f, 100f, 50f),
			HSLA(60f, 100f, 50f),
		)

        for ((index, _) in rgbs.withIndex()) {
			val h = rgbs[index].toHSL()
			val h2 = hsls[index]

			assertEquals(h.h, h2.h, 0.01f)
			assertEquals(h.s, h2.s, 0.01f)
			assertEquals(h.l, h2.l, 0.01f)
		}
	}
}
