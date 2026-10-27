package dev.oblac.gart.vector

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Vec3Test {

    @Test
    fun testDivByVec3() {
        assertEquals(Vec3(2f, -3f, 0.5f), Vec3(4f, 9f, 2f) / Vec3(2f, -3f, 4f))
    }

    @Test
    fun testAverage() {
        assertEquals(Vec3(2f, 3f, -1f), listOf(Vec3(1f, 2f, 0f), Vec3(3f, 4f, -2f)).average())
        assertTrue(emptyList<Vec3>().average().x.isNaN())
    }
}
