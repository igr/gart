package dev.oblac.gart.color

import dev.oblac.gart.color.space.ColorHSI
import org.jetbrains.skia.Color4f
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColorHSITest {

    @Test
    fun testRedToHSI() {
        val hsi = ColorHSI.of(Color4f(1f, 0f, 0f, 1f))
        assertEquals(0f, hsi.h, 0.5f)
        assertEquals(1f, hsi.s, 0.01f)
        assertEquals(1f / 3f, hsi.i, 0.01f)
    }

    @Test
    fun testGreenToHSI() {
        val hsi = ColorHSI.of(Color4f(0f, 1f, 0f, 1f))
        assertEquals(120f, hsi.h, 0.5f)
        assertEquals(1f, hsi.s, 0.01f)
        assertEquals(1f / 3f, hsi.i, 0.01f)
    }

    @Test
    fun testBlueToHSI() {
        val hsi = ColorHSI.of(Color4f(0f, 0f, 1f, 1f))
        assertEquals(240f, hsi.h, 0.5f)
        assertEquals(1f, hsi.s, 0.01f)
        assertEquals(1f / 3f, hsi.i, 0.01f)
    }

    @Test
    fun testWhiteToHSI() {
        val hsi = ColorHSI.of(Color4f(1f, 1f, 1f, 1f))
        assertEquals(0f, hsi.s, 0.01f)
        assertEquals(1f, hsi.i, 0.01f)
    }

    @Test
    fun testBlackToHSI() {
        val hsi = ColorHSI.of(Color4f(0f, 0f, 0f, 1f))
        assertEquals(0f, hsi.s, 0.01f)
        assertEquals(0f, hsi.i, 0.01f)
    }

    @Test
    fun testHSItoRGB_Red() {
        val color = ColorHSI(h = 0f, s = 1f, i = 1f / 3f).toColor4f()
        assertEquals(1f, color.r, 0.01f)
        assertEquals(0f, color.g, 0.01f)
        assertEquals(0f, color.b, 0.01f)
    }

    @Test
    fun testHSItoRGB_Green() {
        val color = ColorHSI(h = 120f, s = 1f, i = 1f / 3f).toColor4f()
        assertEquals(0f, color.r, 0.01f)
        assertEquals(1f, color.g, 0.01f)
        assertEquals(0f, color.b, 0.01f)
    }

    @Test
    fun testHSItoRGB_Blue() {
        val color = ColorHSI(h = 240f, s = 1f, i = 1f / 3f).toColor4f()
        assertEquals(0f, color.r, 0.01f)
        assertEquals(0f, color.g, 0.01f)
        assertEquals(1f, color.b, 0.01f)
    }

    @Test
    fun testHSItoRGB_Gray() {
        val color = ColorHSI(h = 0f, s = 0f, i = 0.5f).toColor4f()
        assertEquals(0.5f, color.r, 0.01f)
        assertEquals(0.5f, color.g, 0.01f)
        assertEquals(0.5f, color.b, 0.01f)
    }

    @Test
    fun testRoundTrip() {
        val original = Color4f(0.3f, 0.6f, 0.9f, 1f)
        val hsi = ColorHSI.of(original)
        val result = hsi.toColor4f()
        assertEquals(original.r, result.r, 0.01f)
        assertEquals(original.g, result.g, 0.01f)
        assertEquals(original.b, result.b, 0.01f)
    }

    @Test
    fun testAlphaPreserved() {
        val original = Color4f(0.5f, 0.5f, 0.5f, 0.7f)
        val hsi = ColorHSI.of(original)
        assertEquals(0.7f, hsi.a, 0.01f)
        val result = hsi.toColor4f()
        assertEquals(0.7f, result.a, 0.01f)
    }
}
