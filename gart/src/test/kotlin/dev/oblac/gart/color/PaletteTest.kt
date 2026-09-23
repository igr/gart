package dev.oblac.gart.color

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class PaletteTest {

    private val p = Palette(0xFF000001, 0xFF000002, 0xFF000003, 0xFF000004)

    @Test
    fun pickKeepsTheOrderGiven() {
        assertEquals(listOf(p[3], p[0], p[2]), p.pick(3, 0, 2).map { it })
    }

    @Test
    fun pickMayRepeat() {
        assertEquals(listOf(p[1], p[1], p[1]), p.pick(1, 1, 1).map { it })
    }

    @Test
    fun pickOfNothingIsEmpty() {
        assertEquals(0, p.pick().size)
    }

    @Test
    fun pickOutOfRangeThrows() {
        assertFailsWith<IllegalArgumentException> { p.pick(4) }
        assertFailsWith<IllegalArgumentException> { p.pick(0, -1) }
    }

    @Test
    fun sampleOklabOfOneColourIsThatColour() {
        assertEquals(0xFF336699.toInt(), Palette(0xFF336699).sampleOklab(0.3f))
    }

    @Test
    fun sampleOklabClampsAndHoldsTheEnds() {
        val bw = Palette(0xFF000000, 0xFFFFFFFF)
        assertEquals(bw.sampleOklab(0f), bw.sampleOklab(-1f))
        assertEquals(bw.sampleOklab(1f), bw.sampleOklab(2f))
        assertEquals(0f, red(bw.sampleOklab(0f)).toFloat(), 1f)
        assertEquals(255f, red(bw.sampleOklab(1f)).toFloat(), 1f)
    }

    @Test
    fun sampleOklabHalfwayIsHalfTheLightness() {
        // oklab L 0.5 is 0.125 linear, sRGB 99 - a straight RGB blend would give 127
        val mid = Palette(0xFF000000, 0xFFFFFFFF).sampleOklab(0.5f)
        assertEquals(99f, red(mid).toFloat(), 1f)
        assertEquals(red(mid).toFloat(), green(mid).toFloat(), 1f)
        assertEquals(red(mid).toFloat(), blue(mid).toFloat(), 1f)
    }
}
