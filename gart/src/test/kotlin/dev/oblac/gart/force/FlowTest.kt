package dev.oblac.gart.force

import dev.oblac.gart.angles.Radians
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlowTest {

    @Test
    fun addOppositeFlows() {
        val right = Flow(Radians.ZERO, 1f)
        val left = Flow(Radians.PI, 1f)

        val result = right + left

        assertEquals(Math.PI.toFloat() / 2, result.direction.toFloat())
        assertEquals(1f, result.magnitude)
    }
}
