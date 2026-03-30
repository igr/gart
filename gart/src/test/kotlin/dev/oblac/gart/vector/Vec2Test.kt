package dev.oblac.gart.vector

import dev.oblac.gart.angle.Radians
import dev.oblac.gart.math.PIf
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Vec2Test {

    @Test
    fun testAngle() {
        assertEquals(Radians.ZERO.value, Vec2(1f, 0f).angle.radians, 0.0001f)
        assertEquals(Radians.PI_HALF.value, Vec2(0f, 1f).angle.radians, 0.0001f)
        assertEquals(Radians.PI.value, Vec2(-1f, 0f).angle.radians, 0.0001f)
        assertEquals(Radians(-PIf / 2).value, Vec2(0f, -1f).angle.radians, 0.0001f)
    }
}
