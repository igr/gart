package dev.oblac.gart.color

import dev.oblac.gart.color.space.ColorHSV
import org.jetbrains.skia.Color4f
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColorHSVTest {

    @Test
    fun testRedToHSV() {
        val hsv = ColorHSV.of(Color4f(1f, 0f, 0f, 1f))
        assertEquals(0f, hsv.h, 0.01f)
        assertEquals(1f, hsv.s, 0.01f)
        assertEquals(1f, hsv.v, 0.01f)
    }

    @Test
    fun testGreenToHSV() {
        val hsv = ColorHSV.of(Color4f(0f, 1f, 0f, 1f))
        assertEquals(120f, hsv.h, 0.01f)
        assertEquals(1f, hsv.s, 0.01f)
        assertEquals(1f, hsv.v, 0.01f)
    }

    @Test
    fun testBlueToHSV() {
        val hsv = ColorHSV.of(Color4f(0f, 0f, 1f, 1f))
        assertEquals(240f, hsv.h, 0.01f)
        assertEquals(1f, hsv.s, 0.01f)
        assertEquals(1f, hsv.v, 0.01f)
    }

    @Test
    fun testWhiteToHSV() {
        val hsv = ColorHSV.of(Color4f(1f, 1f, 1f, 1f))
        assertEquals(0f, hsv.s, 0.01f)
        assertEquals(1f, hsv.v, 0.01f)
    }

    @Test
    fun testBlackToHSV() {
        val hsv = ColorHSV.of(Color4f(0f, 0f, 0f, 1f))
        assertEquals(0f, hsv.s, 0.01f)
        assertEquals(0f, hsv.v, 0.01f)
    }

    @Test
    fun testHSVtoRGB_Red() {
        val color = ColorHSV(h = 0f, s = 1f, v = 1f).toColor4f()
        assertEquals(1f, color.r, 0.01f)
        assertEquals(0f, color.g, 0.01f)
        assertEquals(0f, color.b, 0.01f)
    }

    @Test
    fun testHSVtoRGB_Green() {
        val color = ColorHSV(h = 120f, s = 1f, v = 1f).toColor4f()
        assertEquals(0f, color.r, 0.01f)
        assertEquals(1f, color.g, 0.01f)
        assertEquals(0f, color.b, 0.01f)
    }

    @Test
    fun testHSVtoRGB_Blue() {
        val color = ColorHSV(h = 240f, s = 1f, v = 1f).toColor4f()
        assertEquals(0f, color.r, 0.01f)
        assertEquals(0f, color.g, 0.01f)
        assertEquals(1f, color.b, 0.01f)
    }

    @Test
    fun testHSVtoRGB_Gray() {
        val color = ColorHSV(h = 0f, s = 0f, v = 0.5f).toColor4f()
        assertEquals(0.5f, color.r, 0.01f)
        assertEquals(0.5f, color.g, 0.01f)
        assertEquals(0.5f, color.b, 0.01f)
    }

    @Test
    fun testRoundTrip() {
        val original = Color4f(0.3f, 0.6f, 0.9f, 1f)
        val hsv = ColorHSV.of(original)
        val result = hsv.toColor4f()
        assertEquals(original.r, result.r, 0.01f)
        assertEquals(original.g, result.g, 0.01f)
        assertEquals(original.b, result.b, 0.01f)
    }
}
