package dev.oblac.gart.color

import dev.oblac.gart.color.space.ColorOKLCH
import org.jetbrains.skia.Color4f
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColorOKLCHTest {

    @Test
    fun testBlackToOKLCH() {
        val oklch = ColorOKLCH.of(Color4f(0f, 0f, 0f, 1f))
        assertEquals(0f, oklch.l, 0.01f)
        assertEquals(0f, oklch.c, 0.01f)
    }

    @Test
    fun testWhiteToOKLCH() {
        val oklch = ColorOKLCH.of(Color4f(1f, 1f, 1f, 1f))
        assertEquals(1f, oklch.l, 0.01f)
        assertEquals(0f, oklch.c, 0.01f)
    }

    @Test
    fun testRedToOKLCH() {
        val oklch = ColorOKLCH.of(Color4f(1f, 0f, 0f, 1f))
        assertEquals(0.63f, oklch.l, 0.02f)
        // chroma and hue for red
        assert(oklch.c > 0.2f)
        assertEquals(29f, oklch.h, 2f)
    }

    @Test
    fun testOKLCHtoRGB_Black() {
        val color = ColorOKLCH(l = 0f, c = 0f, h = 0f).toColor4f()
        assertEquals(0f, color.r, 0.01f)
        assertEquals(0f, color.g, 0.01f)
        assertEquals(0f, color.b, 0.01f)
    }

    @Test
    fun testOKLCHtoRGB_White() {
        val color = ColorOKLCH(l = 1f, c = 0f, h = 0f).toColor4f()
        assertEquals(1f, color.r, 0.01f)
        assertEquals(1f, color.g, 0.01f)
        assertEquals(1f, color.b, 0.01f)
    }

    @Test
    fun testRoundTrip() {
        val original = Color4f(0.3f, 0.6f, 0.9f, 1f)
        val oklch = ColorOKLCH.of(original)
        val result = oklch.toColor4f()
        assertEquals(original.r, result.r, 0.01f)
        assertEquals(original.g, result.g, 0.01f)
        assertEquals(original.b, result.b, 0.01f)
    }

    @Test
    fun testRoundTrip_DarkColor() {
        val original = Color4f(0.1f, 0.2f, 0.05f, 1f)
        val oklch = ColorOKLCH.of(original)
        val result = oklch.toColor4f()
        assertEquals(original.r, result.r, 0.01f)
        assertEquals(original.g, result.g, 0.01f)
        assertEquals(original.b, result.b, 0.01f)
    }

    @Test
    fun testAlphaPreserved() {
        val original = Color4f(0.5f, 0.5f, 0.5f, 0.7f)
        val oklch = ColorOKLCH.of(original)
        assertEquals(0.7f, oklch.alpha, 0.01f)
        val result = oklch.toColor4f()
        assertEquals(0.7f, result.a, 0.01f)
    }

    @Test
    fun testGrayHasZeroChroma() {
        val oklch = ColorOKLCH.of(Color4f(0.5f, 0.5f, 0.5f, 1f))
        assertEquals(0f, oklch.c, 0.01f)
    }
}
