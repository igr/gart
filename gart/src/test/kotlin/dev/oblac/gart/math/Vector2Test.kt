package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Vector2Test {

    @Test
    fun testAngle() {
        assertEquals(0f, Vector2(1f, 0f).angle, 0.0001f)
        assertEquals(PIf / 2, Vector2(0f, 1f).angle, 0.0001f)
        assertEquals(PIf, Vector2(-1f, 0f).angle, 0.0001f)
        assertEquals(-PIf / 2, Vector2(0f, -1f).angle, 0.0001f)
    }
}
