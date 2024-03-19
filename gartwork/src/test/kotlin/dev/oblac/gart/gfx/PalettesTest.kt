package dev.oblac.gart.gfx

import kotlin.test.Test
import kotlin.test.assertEquals

class PalettesTest {

    @Test
    fun testPaletteToGradient() {
        val p = Palettes.cool27
        val g = Palettes.gradient(p, 256)
        assertEquals(256, g.size)
    }
}
