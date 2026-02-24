package dev.oblac.gart.color

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ColorNamesTest {

    @Test
    fun testRed() {
        val color = CssColors.color4f("red")
        assertEquals(1f, color.r, 0.01f)
        assertEquals(0f, color.g, 0.01f)
        assertEquals(0f, color.b, 0.01f)
    }

    @Test
    fun testWhite() {
        val color = CssColors.color4f("white")
        assertEquals(1f, color.r, 0.01f)
        assertEquals(1f, color.g, 0.01f)
        assertEquals(1f, color.b, 0.01f)
    }

    @Test
    fun testBlack() {
        val color = CssColors.color4f("black")
        assertEquals(0f, color.r, 0.01f)
        assertEquals(0f, color.g, 0.01f)
        assertEquals(0f, color.b, 0.01f)
    }

    @Test
    fun testCaseInsensitive() {
        val color = CssColors.color4f("DodgerBlue")
        assertEquals(0x1e / 255f, color.r, 0.01f)
        assertEquals(0x90 / 255f, color.g, 0.01f)
        assertEquals(1f, color.b, 0.01f)
    }

    @Test
    fun testCoral() {
        val color = CssColors.color4f("coral")
        assertEquals(1f, color.r, 0.01f)
        assertEquals(0x7f / 255f, color.g, 0.01f)
        assertEquals(0x50 / 255f, color.b, 0.01f)
    }

    @Test
    fun testUnknownNameThrows() {
        assertThrows<IllegalArgumentException> {
            CssColors.color4f("notacolor")
        }
    }
}
