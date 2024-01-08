package dev.oblac.gart.flow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlowForceTest {

    @Test
    fun addOppositeFlows() {
        val right = FlowForce(0f, 1f)
        val left = FlowForce(Math.PI.toFloat(), 1f)

        val result = right + left

        assertEquals(Math.PI.toFloat() / 2, result.direction)
        assertEquals(1f, result.magnitude)
    }
}
