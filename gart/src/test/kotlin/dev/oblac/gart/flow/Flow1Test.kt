package dev.oblac.gart.flow

import dev.oblac.gart.angle.Radians
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Flow1Test {

    @Test
    fun addOppositeFlows() {
        val right = Flow1(Radians.ZERO, 1f)
        val left = Flow1(Radians.PI, 1f)

        val result = right + left

        assertEquals(Math.PI.toFloat() / 2, result.direction.radians)
        assertEquals(1f, result.magnitude)
    }
}
