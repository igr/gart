package dev.oblac.gart.gfx

import dev.oblac.gart.color.Palettes
import kotlin.test.Test
import kotlin.test.assertEquals

class PalettesTest {

    @Test
    fun testPaletteToExpand() {
        val p = Palettes.cool27
        val g = p.expand(256)
        assertEquals(256, g.size)
    }

    @Test
    fun testPaletteToExpand2() {
        val g = Palettes.cool20.expand(500)
        assertEquals(500, g.size)
    }
}
