package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals

class DistanceTest {

    @Test
    fun lengthOfThreeFourIsFive() {
        assertEquals(5f, length(3f, 4f))
        assertEquals(5.0, length(3.0, 4.0))
    }

    @Test
    fun lengthIsTheWrittenOutSqrtToTheBit() {
        for ((x, y) in listOf(1f to 1f, 0.1f to 0.7f, -3.3f to 12.9f, 1e-3f to 2e3f)) {
            assertEquals(sqrt(x * x + y * y), length(x, y))
            assertEquals(sqrt(x.toDouble() * x + y.toDouble() * y), length(x.toDouble(), y.toDouble()))
        }
    }
}
