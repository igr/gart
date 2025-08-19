package dev.oblac.gart.color

import dev.oblac.gart.color.space.ColorHSLA
import dev.oblac.gart.color.space.ColorRGBA
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ColorHSLATest {

    @Test
    fun testRGBAtoHSLA_red() {
        val red = ColorRGBA(1f, 0f, 0f, 1f).toHSLA()
        assertEquals(0f, red.h, 0.05f)
        assertEquals(1f, red.s, 0.05f)
        assertEquals(0.5f, red.l, 0.05f)
        assertEquals(1f, red.a)
    }

    @Test
    fun testRGBAtoHSLA_green() {
        val green = ColorRGBA(0f, 1f, 0f, 1f).toHSLA()
        assertEquals(120f, green.h, 0.05f)
        assertEquals(1f, green.s, 0.05f)
        assertEquals(0.5f, green.l, 0.05f)
        assertEquals(1f, green.a)
    }

    @Test
    fun testRGBAtoHSLA_blue() {
        val blue = ColorRGBA(0f, 0f, 1f, 1f).toHSLA()
        assertEquals(240f, blue.h, 0.05f)
        assertEquals(1f, blue.s, 0.05f)
        assertEquals(0.5f, blue.l, 0.05f)
        assertEquals(1f, blue.a)
    }

    @Test
    fun testRGBAtoHSLA_white() {
        val white = ColorRGBA(1f, 1f, 1f, 1f).toHSLA()
        assertEquals(0f, white.h, 0.05f)
        assertEquals(0f, white.s, 0.05f)
        assertEquals(1f, white.l, 0.05f)
        assertEquals(1f, white.a)
    }

    @Test
    fun testRGBAtoHSLA_black() {
        val black = ColorRGBA(0f, 0f, 0f, 1f).toHSLA()
        assertEquals(0f, black.h, 0.05f)
        assertEquals(0f, black.s, 0.05f)
        assertEquals(0f, black.l, 0.05f)
        assertEquals(1f, black.a)
    }

    @Test
    fun testRGBAtoHSLA_gray() {
        val gray = ColorRGBA(0.5f, 0.5f, 0.5f, 1f).toHSLA()
        assertEquals(0f, gray.h, 0.05f)
        assertEquals(0f, gray.s, 0.05f)
        assertEquals(0.5f, gray.l, 0.05f)
        assertEquals(1f, gray.a)
    }

    @Test
    fun testHSLAtoRGBA_red() {
        val red = ColorHSLA(0f, 1f, 0.5f, 1f).toColorRGBA()
        assertEquals(1f, red.r, 0.05f)
        assertEquals(0f, red.g, 0.05f)
        assertEquals(0f, red.b, 0.05f)
        assertEquals(1f, red.a)
    }

    @Test
    fun testHSLAtoRGBA_green() {
        val green = ColorHSLA(120f, 1f, 0.5f, 1f).toColorRGBA()
        assertEquals(0f, green.r, 0.05f)
        assertEquals(1f, green.g, 0.05f)
        assertEquals(0f, green.b, 0.05f)
    }

    @Test
    fun testHSLAtoRGBA_blue() {
        val blue = ColorHSLA(240f, 1f, 0.5f, 1f).toColorRGBA()
        assertEquals(0f, blue.r, 0.05f)
        assertEquals(0f, blue.g, 0.05f)
        assertEquals(1f, blue.b, 0.05f)
    }

    @Test
    fun testHSLAtoRGBA_white() {
        val white = ColorHSLA(0f, 0f, 1f, 1f).toColorRGBA()
        assertEquals(1f, white.r, 0.05f)
        assertEquals(1f, white.g, 0.05f)
        assertEquals(1f, white.b, 0.05f)
    }

    @Test
    fun testHSLAtoRGBA_black() {
        val black = ColorHSLA(0f, 0f, 0f, 1f).toColorRGBA()
        assertEquals(0f, black.r, 0.05f)
        assertEquals(0f, black.g, 0.05f)
        assertEquals(0f, black.b, 0.05f)
    }

    @Test
    fun testHSLAtoRGBA_gray() {
        val gray = ColorHSLA(0f, 0f, 0.5f, 1f).toColorRGBA()
        assertEquals(0.5f, gray.r, 0.05f)
        assertEquals(0.5f, gray.g, 0.05f)
        assertEquals(0.5f, gray.b, 0.05f)
    }
}
