package dev.oblac.gart.color

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColorSaturateTest {

    @Test
    fun testSaturateRed() {
        val red = ColorHSLA(0f, 0.5f, 0.5f, 1f)
        val moreSaturated = red.saturate(1.5f)
        assertEquals(0f, moreSaturated.h, 0.05f)
        assertEquals(0.75f, moreSaturated.s, 0.05f)
        assertEquals(0.5f, moreSaturated.l, 0.05f)
        assertEquals(1f, moreSaturated.a)
    }

    @Test
    fun testSaturateGreen() {
        val green = ColorHSLA(120f, 0.6f, 0.4f, 1f)
        val moreSaturated = green.saturate(1.2f)
        assertEquals(120f, moreSaturated.h, 0.05f)
        assertEquals(0.72f, moreSaturated.s, 0.05f)
        assertEquals(0.4f, moreSaturated.l, 0.05f)
        assertEquals(1f, moreSaturated.a)
    }

    @Test
    fun testSaturateBlue() {
        val blue = ColorHSLA(240f, 0.8f, 0.6f, 1f)
        val lessSaturated = blue.saturate(0.5f)
        assertEquals(240f, lessSaturated.h, 0.05f)
        assertEquals(0.4f, lessSaturated.s, 0.05f)
        assertEquals(0.6f, lessSaturated.l, 0.05f)
        assertEquals(1f, lessSaturated.a)
    }

    @Test
    fun testSaturateGray() {
        val gray = ColorHSLA(0f, 0f, 0.5f, 1f)
        val saturated = gray.saturate(2f)
        assertEquals(0f, saturated.h, 0.05f)
        assertEquals(0f, saturated.s, 0.05f)
        assertEquals(0.5f, saturated.l, 0.05f)
        assertEquals(1f, saturated.a)
    }

    @Test
    fun testSaturateDesaturate() {
        val color = ColorHSLA(180f, 0.8f, 0.4f, 0.9f)
        val desaturated = color.saturate(0.25f)
        assertEquals(180f, desaturated.h, 0.05f)
        assertEquals(0.2f, desaturated.s, 0.05f)
        assertEquals(0.4f, desaturated.l, 0.05f)
        assertEquals(0.9f, desaturated.a)
    }

    @Test
    fun testSaturateNoChange() {
        val color = ColorHSLA(60f, 0.7f, 0.3f, 1f)
        val unchanged = color.saturate(1f)
        assertEquals(60f, unchanged.h, 0.05f)
        assertEquals(0.7f, unchanged.s, 0.05f)
        assertEquals(0.3f, unchanged.l, 0.05f)
        assertEquals(1f, unchanged.a)
    }

    @Test
    fun testSaturateZero() {
        val color = ColorHSLA(300f, 0.6f, 0.5f, 1f)
        val desaturated = color.saturate(0f)
        assertEquals(300f, desaturated.h, 0.05f)
        assertEquals(0f, desaturated.s, 0.05f)
        assertEquals(0.5f, desaturated.l, 0.05f)
        assertEquals(1f, desaturated.a)
    }

    @Test
    fun testSaturateFullSaturation() {
        val color = ColorHSLA(45f, 0.3f, 0.6f, 1f)
        val fullySaturated = color.saturate(3.33f)
        assertEquals(45f, fullySaturated.h, 0.05f)
        assertEquals(1f, fullySaturated.s, 0.05f)
        assertEquals(0.6f, fullySaturated.l, 0.05f)
        assertEquals(1f, fullySaturated.a)
    }
}
