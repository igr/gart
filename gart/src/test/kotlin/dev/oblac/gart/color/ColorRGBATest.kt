package dev.oblac.gart.color

import dev.oblac.gart.color.space.ColorRGBA
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColorRGBATest {

    @Test
    fun testColorRGBA1() {
        val rgba = ColorRGBA(0.5f, 0.5f, 0.5f, 1f).toRGBA()
        assertEquals(127, rgba.r)
        assertEquals(127, rgba.g)
        assertEquals(127, rgba.b)
        assertEquals(255, rgba.a)

        val c = ColorRGBA.of(rgba)
        assertEquals(0.5f, c.r, 0.05f)
        assertEquals(0.5f, c.g, 0.05f)
        assertEquals(0.5f, c.b, 0.05f)
        assertEquals(1f, c.a)
    }

    @Test
    fun testColorRGBA2() {
        val rgba = ColorRGBA(0.1f, 0.2f, 0.3f, 1f).toRGBA()
        assertEquals(25, rgba.r)
        assertEquals(51, rgba.g)
        assertEquals(76, rgba.b)
        assertEquals(255, rgba.a)

        val c = ColorRGBA.of(rgba)
        assertEquals(0.1f, c.r, 0.05f)
        assertEquals(0.2f, c.g, 0.05f)
        assertEquals(0.3f, c.b, 0.05f)
        assertEquals(1f, c.a)
    }
}
