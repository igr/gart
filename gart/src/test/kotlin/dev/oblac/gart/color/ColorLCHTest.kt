package dev.oblac.gart.color

import dev.oblac.gart.color.space.ColorLCH
import org.jetbrains.skia.Color4f
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColorLCHTest {

    @Test
    fun testBlackToLCH() {
        val lch = ColorLCH.of(Color4f(0f, 0f, 0f, 1f))
        assertEquals(0f, lch.l, 0.1f)
        assertEquals(0f, lch.c, 0.1f)
    }

    @Test
    fun testWhiteToLCH() {
        val lch = ColorLCH.of(Color4f(1f, 1f, 1f, 1f))
        assertEquals(100f, lch.l, 0.1f)
        assertEquals(0f, lch.c, 0.5f)
    }

    @Test
    fun testRedToLCH() {
        // Red: L~53.23, C~104.55, H~40.0
        val lch = ColorLCH.of(Color4f(1f, 0f, 0f, 1f))
        assertEquals(53.23f, lch.l, 1f)
        assertEquals(104.55f, lch.c, 1f)
        assertEquals(40f, lch.h, 1f)
    }

    @Test
    fun testLCHtoRGB_Black() {
        val color = ColorLCH(l = 0f, c = 0f, h = 0f).toColor4f()
        assertEquals(0f, color.r, 0.01f)
        assertEquals(0f, color.g, 0.01f)
        assertEquals(0f, color.b, 0.01f)
    }

    @Test
    fun testLCHtoRGB_White() {
        val color = ColorLCH(l = 100f, c = 0f, h = 0f).toColor4f()
        assertEquals(1f, color.r, 0.01f)
        assertEquals(1f, color.g, 0.01f)
        assertEquals(1f, color.b, 0.01f)
    }

    @Test
    fun testRoundTrip() {
        val original = Color4f(0.3f, 0.6f, 0.9f, 1f)
        val lch = ColorLCH.of(original)
        val result = lch.toColor4f()
        assertEquals(original.r, result.r, 0.01f)
        assertEquals(original.g, result.g, 0.01f)
        assertEquals(original.b, result.b, 0.01f)
    }

    @Test
    fun testRoundTrip_DarkColor() {
        val original = Color4f(0.1f, 0.2f, 0.05f, 1f)
        val lch = ColorLCH.of(original)
        val result = lch.toColor4f()
        assertEquals(original.r, result.r, 0.01f)
        assertEquals(original.g, result.g, 0.01f)
        assertEquals(original.b, result.b, 0.01f)
    }

    @Test
    fun testAlphaPreserved() {
        val original = Color4f(0.5f, 0.5f, 0.5f, 0.7f)
        val lch = ColorLCH.of(original)
        assertEquals(0.7f, lch.alpha, 0.01f)
        val result = lch.toColor4f()
        assertEquals(0.7f, result.a, 0.01f)
    }

    @Test
    fun testGrayHasZeroChroma() {
        val lch = ColorLCH.of(Color4f(0.5f, 0.5f, 0.5f, 1f))
        assertEquals(0f, lch.c, 0.5f)
    }
}
