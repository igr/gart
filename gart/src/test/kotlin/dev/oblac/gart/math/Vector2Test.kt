package dev.oblac.gart.math

import dev.oblac.gart.angles.Radians
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Vector2Test {

    @Test
    fun testAngle() {
        assertEquals(Radians.ZERO.toFloat(), Vector2(1f, 0f).angle.toFloat(), 0.0001f)
        assertEquals(Radians.PI_HALF.toFloat(), Vector2(0f, 1f).angle.toFloat(), 0.0001f)
        assertEquals(Radians.PI.toFloat(), Vector2(-1f, 0f).angle.toFloat(), 0.0001f)
        assertEquals(Radians(-PIf / 2).toFloat(), Vector2(0f, -1f).angle.toFloat(), 0.0001f)
    }
}
