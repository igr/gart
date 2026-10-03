package dev.oblac.gart.color

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TintTest {

    @Test
    fun greysAndBlackGiveTheNeutralTint() {
        for (c in listOf(0xFF808080, 0xFFFFFFFF, 0xFF000000, 0xFF0A0A0A)) {
            assertEquals(Tint(1f, 1f, 1f), tintOf(c.toInt()), c.toString(16))
        }
        assertEquals(Tint(1f, 1f, 1f), Tint.NEUTRAL)
    }

    @Test
    fun primariesKeepOneChannelOverItsWeight() {
        // linear red is (1, 0, 0) with luminance 0.2126, so the tint is 1 / 0.2126 on red only
        assertEquals(Tint(1f / 0.2126f, 0f, 0f), tintOf(0xFFFF0000.toInt()))
        assertEquals(Tint(0f, 1f / 0.7152f, 0f), tintOf(0xFF00FF00.toInt()))
        assertEquals(Tint(0f, 0f, 1f / 0.0722f), tintOf(0xFF0000FF.toInt()))
    }

    @Test
    fun aColourComesOutAtLuminanceOne() {
        for (c in listOf(0xFFFFB050, 0xFF4A4E9A, 0xFF5A6A9A, 0xFF13333C)) {
            val t = tintOf(c.toInt())
            assertEquals(1f, 0.2126f * t.r + 0.7152f * t.g + 0.0722f * t.b, 1e-5f, c.toString(16))
        }
    }

    @Test
    fun alphaDoesNotCount() {
        assertEquals(tintOf(0xFFFFB050.toInt()), tintOf(0x00FFB050))
    }

    @Test
    fun channelsReadByIndexInRgbOrder() {
        val t = Tint(0.5f, 2f, 7f)
        assertEquals(0.5f, t[0])
        assertEquals(2f, t[1])
        assertEquals(7f, t[2])
        assertFailsWith<IndexOutOfBoundsException> { t[3] }
        assertFailsWith<IndexOutOfBoundsException> { t[-1] }
    }
}
