package dev.oblac.gart.color

import dev.oblac.gart.color.space.ColorCMYK
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColorCMYKTest {

    @Test
    fun testPureCyanInk() {
        val cmyk = ColorCMYK(c = 0.5f, m = 0f, y = 0f, k = 0f)
        val color = cmyk.toColor4f()
        assertEquals(0.5f, color.r, 0.01f)   // (1 - 0.5) * (1 - 0) = 0.5
        assertEquals(1f, color.g, 0.01f)
        assertEquals(1f, color.b, 0.01f)
    }

    @Test
    fun testPureMagentaInk() {
        val cmyk = ColorCMYK(c = 0f, m = 0.5f, y = 0f, k = 0f)
        val color = cmyk.toColor4f()
        assertEquals(1f, color.r, 0.01f)
        assertEquals(0.5f, color.g, 0.01f)   // (1 - 0.5) * (1 - 0) = 0.5
        assertEquals(1f, color.b, 0.01f)
    }

    @Test
    fun testPureYellowInk() {
        val cmyk = ColorCMYK(c = 0f, m = 0f, y = 0.5f, k = 0f)
        val color = cmyk.toColor4f()
        assertEquals(1f, color.r, 0.01f)
        assertEquals(1f, color.g, 0.01f)
        assertEquals(0.5f, color.b, 0.01f)   // (1 - 0.5) * (1 - 0) = 0.5
    }

    @Test
    fun testPureBlackInk() {
        val cmyk = ColorCMYK(c = 0f, m = 0f, y = 0f, k = 0.5f)
        val color = cmyk.toColor4f()
        assertEquals(0.5f, color.r, 0.01f)   // (1 - 0) * (1 - 0.5) = 0.5
        assertEquals(0.5f, color.g, 0.01f)
        assertEquals(0.5f, color.b, 0.01f)
    }

    @Test
    fun testFullCyanInk() {
        val cmyk = ColorCMYK(c = 1f, m = 0f, y = 0f, k = 0f)
        val color = cmyk.toColor4f()
        assertEquals(0f, color.r, 0.01f)
        assertEquals(1f, color.g, 0.01f)
        assertEquals(1f, color.b, 0.01f)
    }

    @Test
    fun testFullBlackInk() {
        val cmyk = ColorCMYK(c = 0f, m = 0f, y = 0f, k = 1f)
        val color = cmyk.toColor4f()
        assertEquals(0f, color.r, 0.01f)
        assertEquals(0f, color.g, 0.01f)
        assertEquals(0f, color.b, 0.01f)
    }

    @Test
    fun testWhite() {
        val cmyk = ColorCMYK(c = 0f, m = 0f, y = 0f, k = 0f)
        val color = cmyk.toColor4f()
        assertEquals(1f, color.r, 0.01f)
        assertEquals(1f, color.g, 0.01f)
        assertEquals(1f, color.b, 0.01f)
    }

    @Test
    fun testMixedColor() {
        val cmyk = ColorCMYK(c = 0.2f, m = 0.3f, y = 0.4f, k = 0.1f)
        val color = cmyk.toColor4f()
        assertEquals((1f - 0.2f) * (1f - 0.1f), color.r, 0.01f)  // 0.72
        assertEquals((1f - 0.3f) * (1f - 0.1f), color.g, 0.01f)  // 0.63
        assertEquals((1f - 0.4f) * (1f - 0.1f), color.b, 0.01f)  // 0.54
    }
}
