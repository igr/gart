package dev.oblac.gart.tri3d

import dev.oblac.gart.vector.Vec3
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CameraTest {

    private fun assertVecNear(expected: Vec3, actual: Vec3, eps: Float = 1e-3f) {
        assertTrue(
            abs(expected.x - actual.x) < eps &&
                abs(expected.y - actual.y) < eps &&
                abs(expected.z - actual.z) < eps,
            "expected $expected, got $actual"
        )
    }

    @Test
    fun unprojectRoundTrip() {
        val camera = Camera(screenCx = 400f, screenCy = 400f, scale = 200f, distance = 4f)
        val v = Vec3(0.7f, -0.3f, 1.2f)

        val screen = camera.project(v)
        val depth = camera.depth(v)
        val back = camera.unproject(screen.x, screen.y, depth)

        assertVecNear(v, back)
    }

    @Test
    fun unprojectAtScreenCenterHasZeroXY() {
        val camera = Camera(screenCx = 400f, screenCy = 400f, scale = 200f, distance = 4f)

        val back = camera.unproject(400f, 400f, 5f)

        assertEquals(0f, back.x, 1e-4f)
        assertEquals(0f, back.y, 1e-4f)
        assertEquals(1f, back.z, 1e-4f) // depth - distance = 5 - 4
    }

    @Test
    fun rayDirectionAtCenterIsAlongZ() {
        val camera = Camera(screenCx = 400f, screenCy = 400f, scale = 200f, distance = 4f)

        val dir = camera.rayDirection(400f, 400f)

        assertEquals(0f, dir.x, 1e-4f)
        assertEquals(0f, dir.y, 1e-4f)
        assertEquals(1f, dir.z, 1e-4f)
    }

    @Test
    fun rayDirectionIsNormalized() {
        val camera = Camera(screenCx = 400f, screenCy = 400f, scale = 200f, distance = 4f)

        val dir = camera.rayDirection(123f, 456f)

        assertEquals(1f, dir.length(), 1e-4f)
    }

    @Test
    fun eyeIsAtNegativeDistance() {
        val camera = Camera(screenCx = 100f, screenCy = 100f, scale = 50f, distance = 7f)

        assertEquals(Vec3(0f, 0f, -7f), camera.eye)
    }
}
