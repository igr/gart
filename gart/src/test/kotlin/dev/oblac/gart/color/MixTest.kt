package dev.oblac.gart.color

import dev.oblac.gart.color.CssColors.blue
import dev.oblac.gart.color.CssColors.red
import dev.oblac.gart.color.space.*
import org.jetbrains.skia.Color4f
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.math.pow
import kotlin.math.sqrt

class MixTest {

    @Test
    fun testMix() {
        val mix1 = RGBA.of(red).mix(RGBA.of(blue))
        assertEquals(0xFF800080.toInt(), mix1.value)

        val mix2 = red.color4f().mixLrgb(blue.color4f())
        assertEquals(0xFFb400b4.toInt(), mix2.toColor())

        val mix3 = ColorHSL.of(red.color4f()).mix(ColorHSL.of(blue.color4f()))
        assertEquals(0xFFFF00FF.toInt(), mix3.toColor4f().toColor())

        val mix4 = ColorLAB.of(red.color4f()).mix(ColorLAB.of(blue.color4f()))
        assertEquals(0xFFCA0088.toInt(), mix4.toColor4f().toColor())

        val mix5 = ColorLCH.of(red.color4f()).mix(ColorLCH.of(blue.color4f()))
        assertEquals(0xFFFA0080.toInt(), mix5.toColor4f().toColor())
    }

    @Test
    fun mixLrgbGammaPicksTheCurve() {
        val black = Color4f(0f, 0f, 0f, 1f)
        val white = Color4f(1f, 1f, 1f, 1f)
        assertEquals(sqrt(0.5f), black.mixLrgb(white).r, 1e-6f)
        assertEquals(0.5f.pow(1f / 2.2f), black.mixLrgb(white, gamma = 2.2f).r, 1e-6f)
        assertEquals(0.5f, black.mixLrgb(white, gamma = 1f).r, 1e-6f)
    }

    @Test
    fun mixLrgbEndsAreTheTwoColors() {
        val a = Color4f(0.1f, 0.6f, 0.9f, 1f)
        val b = Color4f(0.8f, 0.2f, 0.4f, 0.5f)
        for (gamma in listOf(1f, 2f, 2.2f)) {
            assertEquals(a.r, a.mixLrgb(b, 0f, gamma).r, 1e-6f)
            assertEquals(b.b, a.mixLrgb(b, 1f, gamma).b, 1e-6f)
            assertEquals(0.75f, a.mixLrgb(b, 0.5f, gamma).a, 1e-6f)
        }
    }

    @Test
    fun mixLrgbRejectsAGammaOfZero() {
        assertThrows(IllegalArgumentException::class.java) { Color4f(0f, 0f, 0f, 1f).mixLrgb(Color4f(1f, 1f, 1f, 1f), gamma = 0f) }
    }
}
