package dev.oblac.gart.color

import dev.oblac.gart.color.space.ColorLAB
import org.jetbrains.skia.Color4f
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColorLABTest {

    @Test
    fun testBlackToLAB() {
        val lab = ColorLAB.of(Color4f(0f, 0f, 0f, 1f))
        assertEquals(0f, lab.l, 0.1f)
        assertEquals(0f, lab.a, 0.1f)
        assertEquals(0f, lab.b, 0.1f)
    }

    @Test
    fun testWhiteToLAB() {
        val lab = ColorLAB.of(Color4f(1f, 1f, 1f, 1f))
        assertEquals(100f, lab.l, 0.1f)
        assertEquals(0f, lab.a, 0.5f)
        assertEquals(0f, lab.b, 0.5f)
    }

    @Test
    fun testRedToLAB() {
        // Red in LAB is approximately L=53.23, a=80.11, b=67.22
        val lab = ColorLAB.of(Color4f(1f, 0f, 0f, 1f))
        assertEquals(53.23f, lab.l, 1f)
        assertEquals(80.11f, lab.a, 1f)
        assertEquals(67.22f, lab.b, 1f)
    }

    @Test
    fun testLABtoRGB_Black() {
        val color = ColorLAB(l = 0f, a = 0f, b = 0f).toColor4f()
        assertEquals(0f, color.r, 0.01f)
        assertEquals(0f, color.g, 0.01f)
        assertEquals(0f, color.b, 0.01f)
    }

    @Test
    fun testLABtoRGB_White() {
        val color = ColorLAB(l = 100f, a = 0f, b = 0f).toColor4f()
        assertEquals(1f, color.r, 0.01f)
        assertEquals(1f, color.g, 0.01f)
        assertEquals(1f, color.b, 0.01f)
    }

    @Test
    fun testLABtoRGB_Red() {
        val color = ColorLAB(l = 53.23f, a = 80.11f, b = 67.22f).toColor4f()
        assertEquals(1f, color.r, 0.02f)
        assertEquals(0f, color.g, 0.02f)
        assertEquals(0f, color.b, 0.02f)
    }

    @Test
    fun testRoundTrip() {
        val original = Color4f(0.3f, 0.6f, 0.9f, 1f)
        val lab = ColorLAB.of(original)
        val result = lab.toColor4f()
        assertEquals(original.r, result.r, 0.01f)
        assertEquals(original.g, result.g, 0.01f)
        assertEquals(original.b, result.b, 0.01f)
    }

    @Test
    fun testRoundTrip_DarkColor() {
        val original = Color4f(0.1f, 0.2f, 0.05f, 1f)
        val lab = ColorLAB.of(original)
        val result = lab.toColor4f()
        assertEquals(original.r, result.r, 0.01f)
        assertEquals(original.g, result.g, 0.01f)
        assertEquals(original.b, result.b, 0.01f)
    }

    @Test
    fun testAlphaPreserved() {
        val original = Color4f(0.5f, 0.5f, 0.5f, 0.7f)
        val lab = ColorLAB.of(original)
        assertEquals(0.7f, lab.alpha, 0.01f)
        val result = lab.toColor4f()
        assertEquals(0.7f, result.a, 0.01f)
    }

    @Test
    fun testGrayMidpoint() {
        // Mid-gray should have L~53.39, a~0, b~0
        val lab = ColorLAB.of(Color4f(0.5f, 0.5f, 0.5f, 1f))
        assertEquals(53.39f, lab.l, 1f)
        assertEquals(0f, lab.a, 0.5f)
        assertEquals(0f, lab.b, 0.5f)
    }
}
