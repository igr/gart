package dev.oblac.gart.math

import dev.oblac.gart.angles.Radians
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Vector2Test {

    @Test
    fun testAngle() {
        assertEquals(Radians.ZERO.value, Vector2(1f, 0f).angle.radians, 0.0001f)
        assertEquals(Radians.PI_HALF.value, Vector2(0f, 1f).angle.radians, 0.0001f)
        assertEquals(Radians.PI.value, Vector2(-1f, 0f).angle.radians, 0.0001f)
        assertEquals(Radians(-PIf / 2).value, Vector2(0f, -1f).angle.radians, 0.0001f)
    }
}
