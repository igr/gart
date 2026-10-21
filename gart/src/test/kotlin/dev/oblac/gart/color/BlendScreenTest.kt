package dev.oblac.gart.color

import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlendScreenTest {

    @Test
    fun screensEachChannel() {
        // 128 + 127 * 128 / 255 = 191.75, cut to 191
        assertEquals(rgb(191, 191, 191), blendScreen(rgb(128, 128, 128), rgb(128, 128, 128)))
    }

    @Test
    fun amountScalesTheOverColor() {
        // 51 + 204 * 102 / 255 * 0.5 = 91.8, cut to 91
        assertEquals(rgb(91, 91, 91), blendScreen(rgb(51, 51, 51), rgb(102, 102, 102), 0.5f))
    }

    @Test
    fun blackOverOrNoAmountKeepsTheBase() {
        for (a in 0..255) {
            val base = rgb(a, 255 - a, a / 2)
            assertEquals(base, blendScreen(base, rgb(0, 0, 0)))
            assertEquals(base, blendScreen(base, rgb(200, 10, 99), 0f))
        }
    }

    @Test
    fun whiteOverGivesWhite() {
        for (a in 0..255) {
            assertEquals(rgb(255, 255, 255), blendScreen(rgb(a, a / 3, 255 - a), rgb(255, 255, 255)))
        }
    }

    @Test
    fun neverDarkerThanTheBase() {
        val rnd = Random(7)
        repeat(10_000) {
            val base = rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
            val over = rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
            val out = blendScreen(base, over, rnd.nextFloat())
            assertTrue(red(out) >= red(base) && green(out) >= green(base) && blue(out) >= blue(base))
        }
    }

    @Test
    fun alphaIsForcedOpaque() {
        assertEquals(255, alpha(blendScreen(0x40102030, 0x80FFFFFF.toInt(), 0.3f)))
    }
}
