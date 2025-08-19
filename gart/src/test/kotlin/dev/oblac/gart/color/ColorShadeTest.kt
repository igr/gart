package dev.oblac.gart.color

import dev.oblac.gart.color.space.ColorHSLA
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColorShadeTest {

    @Test
    fun testShadeRed() {
        val red = ColorHSLA(0f, 1f, 0.5f, 1f)
        val darker = red.shade(0.5f)
        assertEquals(0f, darker.h, 0.05f)
        assertEquals(1f, darker.s, 0.05f)
        assertEquals(0.25f, darker.l, 0.05f)
        assertEquals(1f, darker.a)
    }

    @Test
    fun testShadeGreen() {
        val green = ColorHSLA(120f, 1f, 0.5f, 1f)
        val darker = green.shade(0.3f)
        assertEquals(120f, darker.h, 0.05f)
        assertEquals(1f, darker.s, 0.05f)
        assertEquals(0.15f, darker.l, 0.05f)
        assertEquals(1f, darker.a)
    }

    @Test
    fun testShadeBlue() {
        val blue = ColorHSLA(240f, 1f, 0.5f, 1f)
        val darker = blue.shade(0.8f)
        assertEquals(240f, darker.h, 0.05f)
        assertEquals(1f, darker.s, 0.05f)
        assertEquals(0.4f, darker.l, 0.05f)
        assertEquals(1f, darker.a)
    }

    @Test
    fun testShadeWhite() {
        val white = ColorHSLA(0f, 0f, 1f, 1f)
        val gray = white.shade(0.5f)
        assertEquals(0f, gray.h, 0.05f)
        assertEquals(0f, gray.s, 0.05f)
        assertEquals(0.5f, gray.l, 0.05f)
        assertEquals(1f, gray.a)
    }

    @Test
    fun testShadeGray() {
        val gray = ColorHSLA(0f, 0f, 0.5f, 1f)
        val darker = gray.shade(0.6f)
        assertEquals(0f, darker.h, 0.05f)
        assertEquals(0f, darker.s, 0.05f)
        assertEquals(0.3f, darker.l, 0.05f)
        assertEquals(1f, darker.a)
    }

    @Test
    fun testShadeNoChange() {
        val color = ColorHSLA(180f, 0.8f, 0.4f, 0.9f)
        val unchanged = color.shade(1f)
        assertEquals(180f, unchanged.h, 0.05f)
        assertEquals(0.8f, unchanged.s, 0.05f)
        assertEquals(0.4f, unchanged.l, 0.05f)
        assertEquals(0.9f, unchanged.a)
    }

    @Test
    fun testShadeZero() {
        val color = ColorHSLA(60f, 0.7f, 0.6f, 1f)
        val black = color.shade(0f)
        assertEquals(60f, black.h, 0.05f)
        assertEquals(0.7f, black.s, 0.05f)
        assertEquals(0f, black.l, 0.05f)
        assertEquals(1f, black.a)
    }
}
