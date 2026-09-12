package dev.oblac.gart.color

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UnpremultiplyTest {

    @Test
    fun liftsTheStraightColourOut() {
        // half-covered pure red premultiplies to 128,0,0; back out it is 255,0,0 at the same alpha
        assertEquals(argb(128, 255, 0, 0), unpremultiply(argb(128, 128, 0, 0)))
        assertEquals(argb(128, 128, 64, 0), unpremultiply(argb(128, 64, 32, 0)))
    }

    @Test
    fun opaqueAndClearPassThrough() {
        assertEquals(argb(255, 10, 20, 30), unpremultiply(argb(255, 10, 20, 30)))
        assertEquals(0, unpremultiply(0))
    }

    @Test
    fun roundsAndNeverOverflows() {
        assertEquals(argb(3, 255, 85, 0), unpremultiply(argb(3, 3, 1, 0)))
    }
}
