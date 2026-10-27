package dev.oblac.gart.gfx

import dev.oblac.gart.vector.Vec3
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SdfTest {

    @Test
    fun `ellipsoid with equal radii is the sphere distance`() {
        val r = Vec3(2f, 2f, 2f)
        assertEquals(1f, sdEllipsoid(Vec3(3f, 0f, 0f), r))
        assertEquals(-1f, sdEllipsoid(Vec3(1f, 0f, 0f), r))
    }

    @Test
    fun `ellipsoid is zero on the surface and minus the smallest radius at the centre`() {
        val r = Vec3(4f, 2f, 1f)
        assertEquals(0f, sdEllipsoid(Vec3(0f, 2f, 0f), r))
        assertEquals(-1f, sdEllipsoid(Vec3(0f, 0f, 0f), r))
    }
}
