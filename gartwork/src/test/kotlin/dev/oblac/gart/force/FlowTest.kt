package dev.oblac.gart.force

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlowTest {

    @Test
    fun addOppositeFlows() {
        val right = Flow(0f, 1f)
        val left = Flow(Math.PI.toFloat(), 1f)

        val result = right + left

        assertEquals(Math.PI.toFloat() / 2, result.direction)
        assertEquals(1f, result.magnitude)
    }
}
