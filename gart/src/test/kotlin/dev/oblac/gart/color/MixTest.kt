package dev.oblac.gart.color

import dev.oblac.gart.color.CssColors.blue
import dev.oblac.gart.color.CssColors.red
import dev.oblac.gart.color.space.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
}
