package dev.oblac.gart.color

import dev.oblac.gart.color.space.ColorOKLAB
import org.jetbrains.skia.Color4f
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColorOKLABTest {

    @Test
    fun testBlackToOKLAB() {
        val oklab = ColorOKLAB.of(Color4f(0f, 0f, 0f, 1f))
        assertEquals(0f, oklab.l, 0.01f)
        assertEquals(0f, oklab.a, 0.01f)
        assertEquals(0f, oklab.b, 0.01f)
    }

    @Test
    fun testWhiteToOKLAB() {
        val oklab = ColorOKLAB.of(Color4f(1f, 1f, 1f, 1f))
        assertEquals(1f, oklab.l, 0.01f)
        assertEquals(0f, oklab.a, 0.01f)
        assertEquals(0f, oklab.b, 0.01f)
    }

    @Test
    fun testRedToOKLAB() {
        // Red in OKLab: L~0.6279, a~0.2248, b~0.1258
        val oklab = ColorOKLAB.of(Color4f(1f, 0f, 0f, 1f))
        assertEquals(0.63f, oklab.l, 0.02f)
        assertEquals(0.22f, oklab.a, 0.02f)
        assertEquals(0.13f, oklab.b, 0.02f)
    }

    @Test
    fun testOKLABtoRGB_Black() {
        val color = ColorOKLAB(l = 0f, a = 0f, b = 0f).toColor4f()
        assertEquals(0f, color.r, 0.01f)
        assertEquals(0f, color.g, 0.01f)
        assertEquals(0f, color.b, 0.01f)
    }

    @Test
    fun testOKLABtoRGB_White() {
        val color = ColorOKLAB(l = 1f, a = 0f, b = 0f).toColor4f()
        assertEquals(1f, color.r, 0.01f)
        assertEquals(1f, color.g, 0.01f)
        assertEquals(1f, color.b, 0.01f)
    }

    @Test
    fun testRoundTrip() {
        val original = Color4f(0.3f, 0.6f, 0.9f, 1f)
        val oklab = ColorOKLAB.of(original)
        val result = oklab.toColor4f()
        assertEquals(original.r, result.r, 0.01f)
        assertEquals(original.g, result.g, 0.01f)
        assertEquals(original.b, result.b, 0.01f)
    }

    @Test
    fun testRoundTrip_DarkColor() {
        val original = Color4f(0.1f, 0.2f, 0.05f, 1f)
        val oklab = ColorOKLAB.of(original)
        val result = oklab.toColor4f()
        assertEquals(original.r, result.r, 0.01f)
        assertEquals(original.g, result.g, 0.01f)
        assertEquals(original.b, result.b, 0.01f)
    }

    @Test
    fun testAlphaPreserved() {
        val original = Color4f(0.5f, 0.5f, 0.5f, 0.7f)
        val oklab = ColorOKLAB.of(original)
        assertEquals(0.7f, oklab.alpha, 0.01f)
        val result = oklab.toColor4f()
        assertEquals(0.7f, result.a, 0.01f)
    }

    @Test
    fun testGrayMidpoint() {
        val oklab = ColorOKLAB.of(Color4f(0.5f, 0.5f, 0.5f, 1f))
        assertEquals(0f, oklab.a, 0.01f)
        assertEquals(0f, oklab.b, 0.01f)
    }
}
