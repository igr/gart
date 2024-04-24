package dev.oblac.gart.gfx

import dev.oblac.gart.color.Palettes
import kotlin.test.Test
import kotlin.test.assertEquals

class PalettesTest {

    @Test
    fun testPaletteToGradient() {
        val p = Palettes.cool27
        val g = Palettes.gradient(p, 256)
        assertEquals(256, g.size)
    }

    @Test
    fun testPaletteToGradient2() {
        val g = Palettes.gradient(Palettes.cool20, 500)
        assertEquals(500, g.size)
    }
}
