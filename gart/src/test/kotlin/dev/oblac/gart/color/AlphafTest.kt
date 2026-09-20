package dev.oblac.gart.color

import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AlphafTest {

    @Test
    fun aUnitFloatScalesToTheByteItTruncatesTo() {
        val c = 0x113A7BD5
        assertEquals(alpha(c, 0), alphaf(c, 0f))
        assertEquals(alpha(c, 63), alphaf(c, 0.25f))
        assertEquals(alpha(c, 127), alphaf(c, 0.5f))
        assertEquals(alpha(c, 191), alphaf(c, 0.75f))
        assertEquals(alpha(c, 255), alphaf(c, 1f))
        // truncates, like argb(af, rf, gf, bf) does - it does not round
        assertEquals(alpha(c, 254), alphaf(c, 0.999f))
    }

    @Test
    fun itKeepsTheRgbAndReplacesTheAlpha() {
        assertEquals(0x7F3A7BD5, alphaf(0x113A7BD5, 0.5f))
        assertEquals(0x003A7BD5, alphaf(0xFF3A7BD5.toInt(), 0f))
    }

    @Test
    fun outOfRangePinsInsteadOfWrapping() {
        val c = 0x003A7BD5
        assertEquals(0xFF3A7BD5.toInt(), alphaf(c, 1f))
        assertEquals(0xFF3A7BD5.toInt(), alphaf(c, 4.2f))
        assertEquals(0x003A7BD5, alphaf(c, -3f))
    }

    @Test
    fun theExtensionIsTheSameCall() {
        assertEquals(alphaf(0x113A7BD5, 0.4f), 0x113A7BD5.alphaf(0.4f))
    }

    @Test
    fun itReadsBackThroughTheWholeRange() {
        for (a in 0..255) {
            val back = alpha(alphaf(0x113A7BD5, a / 255f))
            assertTrue(abs(back - a) <= 1, "alpha $a came back as $back")
        }
    }
}
