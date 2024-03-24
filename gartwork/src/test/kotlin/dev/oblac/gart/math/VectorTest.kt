package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VectorTest {

    @Test
    fun testAngle() {
        assertEquals(0f, Vector(1f, 0f).angle, 0.0001f)
        assertEquals(PIf / 2, Vector(0f, 1f).angle, 0.0001f)
        assertEquals(PIf, Vector(-1f, 0f).angle, 0.0001f)
        assertEquals(-PIf / 2, Vector(0f, -1f).angle, 0.0001f)
    }
}
