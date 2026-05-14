package dev.oblac.gart.tri3d

import dev.oblac.gart.vector.Vec3
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CameraPoseTest {

    @Test
    fun translatesPointByNegativePosition() {
        val pose = CameraPose(position = Vec3(1f, 2f, 3f))
        val p = pose.toCameraSpace(Vec3(1f, 2f, 3f))
        assertNear(0f, p.x); assertNear(0f, p.y); assertNear(0f, p.z)
    }

    @Test
    fun yawRotatesAroundYByNegativeAngle() {
        // yaw = +PI/2 → view rotates by -PI/2 around Y: (1,0,0) → (0,0,1)
        val pose = CameraPose(position = Vec3.ZERO, yaw = (PI / 2).toFloat())
        val p = pose.toCameraSpace(Vec3(1f, 0f, 0f))
        assertNear(0f, p.x); assertNear(0f, p.y); assertNear(1f, p.z)
    }

    @Test
    fun toCameraSpaceMeshTransformsEveryFace() {
        val pose = CameraPose(position = Vec3(0f, 1f, 0f))
        val face = Face(Vec3(0f, 0f, 0f), Vec3(1f, 0f, 0f), Vec3(0f, 0f, 1f), 0xFFFF0000.toInt())
        val mesh = Mesh(listOf(face))
        val out = pose.toCameraSpace(mesh)
        val expected = Vec3(0f, -1f, 0f)
        assertEquals(1, out.faces.size)
        val a = out.faces[0].a
        assertNear(expected.x, a.x); assertNear(expected.y, a.y); assertNear(expected.z, a.z)
        assertEquals(0xFFFF0000.toInt(), out.faces[0].color)
    }

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-5f) {
        assertTrue(
            kotlin.math.abs(expected - actual) <= eps,
            "expected $expected ± $eps, got $actual",
        )
    }
}
