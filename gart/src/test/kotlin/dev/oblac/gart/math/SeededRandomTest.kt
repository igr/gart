package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeededRandomTest {

    @Test
    fun theSeedIsMultipliedByTheGoldenGamma() {
        // pinned on purpose: a new number changes the image of every piece on seededRandom
        for (seed in listOf(-7, 0, 1, 13, 1_000_000)) {
            val a = seededRandom(seed)
            val b = Random(seed * -0x61c8864680b583ebL)
            repeat(16) { assertEquals(b.nextInt(), a.nextInt()) }
        }
    }

    @Test
    fun seedsNextToEachOtherFlipTheCoinAboutHalfTheTime() {
        // Random(seed) fails here: after one float the coin follows the last bit of the seed,
        // after two or three floats it changes one time in 8 seeds
        for (floats in 0..4) {
            val coins = (1..256).map { seed ->
                val rnd = seededRandom(seed)
                repeat(floats) { rnd.nextFloat() }
                rnd.nextBoolean()
            }
            val changes = coins.zipWithNext().count { (a, b) -> a != b }
            assertTrue(changes in 90..165, "after $floats floats: $changes changes in 255 pairs")
        }
    }
}
