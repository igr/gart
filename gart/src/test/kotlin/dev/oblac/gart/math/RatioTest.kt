package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RatioTest {

    @Test
    fun shortSideOfWideScreen() {
        assertEquals(1125, shortSide(2000, WIDE_SCREEN_RATIO))
        assertEquals(1080, shortSide(1920, WIDE_SCREEN_RATIO))
    }

    @Test
    fun longSideOfWideScreen() {
        assertEquals(1920, longSide(1080, WIDE_SCREEN_RATIO))
    }

    @Test
    fun roundsToTheNearestPixel() {
        assertEquals(1657, longSide(1024, GOLDEN_RATIO))   // 1656.87
        assertEquals(633, shortSide(1024, GOLDEN_RATIO))   // 632.87
    }

    @Test
    fun ratioWorksEitherWayRound() {
        assertEquals(1125, shortSide(2000, 9f / 16))
        assertEquals(1920, longSide(1080, 9f / 16))
    }
}
