package dev.oblac.gart.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoolPalettesTest {

    @Test
    fun everyPaletteItReturnsHasExactlyThatManyColors() {
        for (size in 1..20) {
            assertTrue(Palettes.coolPalettesOfSize(size).all { it.size == size }, "size $size")
        }
    }

    @Test
    fun itKeepsTheCoolOrder() {
        val fives = Palettes.coolPalettesOfSize(5)
        assertEquals(Palettes.cool2, fives.first())
        assertTrue(fives.indexOf(Palettes.cool127) < fives.indexOf(Palettes.cool214))
    }

    @Test
    fun itPicksOnlyThePalettesOfThatSize() {
        val fours = Palettes.coolPalettesOfSize(4)
        assertTrue(Palettes.cool199 in fours)
        assertFalse(Palettes.cool2 in fours)
    }

    @Test
    fun everyCoolPaletteLandsInExactlyOneSize() {
        assertEquals(Palettes.COOL_COUNT, (0..64).sumOf { Palettes.coolPalettesOfSize(it).size })
    }

    @Test
    fun aSizeNoPaletteHasGivesAnEmptyList() {
        assertTrue(Palettes.coolPalettesOfSize(0).isEmpty())
        assertTrue(Palettes.coolPalettesOfSize(99).isEmpty())
    }

    @Test
    fun theCountIsTheLastCoolPalette() {
        Palettes.coolPalette(Palettes.COOL_COUNT)
        assertFailsWith<IllegalArgumentException> { Palettes.coolPalette(Palettes.COOL_COUNT + 1) }
    }

    @Test
    fun theNavigatorWalksEveryCoolPalette() {
        val nav = Palettes.navigator()
        nav.nextSet()
        nav.previousPalette()
        assertEquals("cool${Palettes.COOL_COUNT}", nav.name())
    }
}
