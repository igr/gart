package dev.oblac.gart.color

import dev.oblac.gart.color.space.*
import org.jetbrains.skia.Color4f
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BigColorTest {

    @Test
    fun testAllSpaces() {
        val color = CssColors.color("orange")
        val color4f = Color4f.of(color)
        val color2 = CssColors.color("skyblue")
        val color4f2 = Color4f.of(color2)

        val orange = RGBA.of(color)
        assertEquals(255, orange.r)
        assertEquals(165, orange.g)
        assertEquals(0, orange.b)

        val hsl = ColorHSL.of(color4f)
        assertEquals(38.82f, hsl.h, 0.01f)
        assertEquals(1.0f, hsl.s, 0.01f)
        assertEquals(0.5f, hsl.l, 0.01f)

        val hsv = ColorHSV.of(color4f)
        assertEquals(38.82f, hsv.h, 0.01f)
        assertEquals(1.0f, hsv.s, 0.01f)
        assertEquals(1.0f, hsv.v, 0.01f)

        val hsi = ColorHSI.of(color4f)
        assertEquals(39.64f, hsi.h, 0.01f)
        assertEquals(1.00f, hsi.s, 0.01f)
        assertEquals(0.55f, hsi.i, 0.01f)

        val lab = ColorLAB.of(color4f)
        assertEquals(74.94f, lab.l, 0.01f)
        assertEquals(23.93f, lab.a, 0.01f)
        assertEquals(78.95f, lab.b, 0.01f)

        val lch = ColorLCH.of(color4f2)
        assertEquals(79.21f, lch.l, 0.01f)
        assertEquals(25.94f, lch.c, 0.01f)
        assertEquals(235.11f, lch.h, 0.01f)

        val oklab = ColorOKLAB.of(color4f)
        assertEquals(0.79f, oklab.l, 0.01f)
        assertEquals(0.06f, oklab.a, 0.01f)
        assertEquals(0.16f, oklab.b, 0.01f)

        val oklch = ColorOKLCH.of(color4f2)
        assertEquals(0.81f, oklch.l, 0.01f)
        assertEquals(0.08f, oklch.c, 0.01f)
        assertEquals(225.74f, oklch.h, 0.01f)
    }
}
