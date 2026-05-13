package dev.oblac.gart.tri3d

import dev.oblac.gart.vector.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RayMeshTest {

    // A unit triangle at z = 5, in the x/y plane:
    //   a = (-1, -1, 5), b = (1, -1, 5), c = (0, 1, 5)
    private val triangle = Mesh(
        listOf(
            Face(
                Vec3(-1f, -1f, 5f),
                Vec3(1f, -1f, 5f),
                Vec3(0f, 1f, 5f),
                color = 0xFFFFFFFF.toInt(),
            )
        )
    )

    @Test
    fun rayThroughCenterIsOccluded() {
        // Ray from origin straight down +z hits the triangle centroid at z=5
        val origin = Vec3(0f, 0f, 0f)
        val dir = Vec3(0f, 0f, 1f) // normalized
        val occluded = RayMesh.isOccluded(origin, dir, triangle, maxT = 10f)
        assertTrue(occluded)
    }

    @Test
    fun rayMissingTriangleIsNotOccluded() {
        // Ray pointing well outside the triangle bounds at z=5
        val origin = Vec3(0f, 0f, 0f)
        val dir = Vec3(0.9f, 0.9f, 1f).normalize() // hits (4.5, 4.5, 5) — outside triangle
        val occluded = RayMesh.isOccluded(origin, dir, triangle, maxT = 10f)
        assertFalse(occluded)
    }

    @Test
    fun rayHitBeyondMaxTIsNotOccluded() {
        // Ray would hit at t=5, but maxT=4 cuts it off
        val origin = Vec3(0f, 0f, 0f)
        val dir = Vec3(0f, 0f, 1f)
        val occluded = RayMesh.isOccluded(origin, dir, triangle, maxT = 4f)
        assertFalse(occluded)
    }

    @Test
    fun rayStartingOnTriangleGoingAwayIsNotOccluded() {
        // Origin on the triangle, ray points away from any other geometry.
        // Epsilon should reject self-hits at t ~= 0.
        val origin = Vec3(0f, 0f, 5f)
        val dir = Vec3(0f, 0f, 1f) // moving further in +z
        val occluded = RayMesh.isOccluded(origin, dir, triangle, maxT = 10f)
        assertFalse(occluded)
    }

    @Test
    fun rayParallelToTriangleIsNotOccluded() {
        // Ray in the plane of the triangle (z=5), moving in +x: never crosses the triangle plane
        val origin = Vec3(-5f, 0f, 5f)
        val dir = Vec3(1f, 0f, 0f)
        val occluded = RayMesh.isOccluded(origin, dir, triangle, maxT = 10f)
        assertFalse(occluded)
    }

    @Test
    fun emptyMeshIsNeverOccluded() {
        val empty = Mesh(emptyList())
        val occluded = RayMesh.isOccluded(Vec3(0f, 0f, 0f), Vec3(0f, 0f, 1f), empty, maxT = 100f)
        assertFalse(occluded)
    }

    @Test
    fun firstHitReturnsNullForMiss() {
        val origin = Vec3(0f, 0f, 0f)
        val dir = Vec3(0.9f, 0.9f, 1f).normalize()
        val hit = RayMesh.firstHit(origin, dir, triangle, maxT = 10f)
        assertEquals(null, hit)
    }

    @Test
    fun firstHitReturnsNullForEmptyMesh() {
        val empty = Mesh(emptyList())
        val hit = RayMesh.firstHit(Vec3(0f, 0f, 0f), Vec3(0f, 0f, 1f), empty, maxT = 100f)
        assertEquals(null, hit)
    }

    @Test
    fun firstHitReturnsTOfNearestFace() {
        // Two parallel unit triangles in the x/y plane, at z=5 and z=8.
        // A ray straight along +z must hit z=5 first.
        val near = Face(
            Vec3(-1f, -1f, 5f), Vec3(1f, -1f, 5f), Vec3(0f, 1f, 5f),
            color = 0xFF0000FF.toInt(),
        )
        val far = Face(
            Vec3(-1f, -1f, 8f), Vec3(1f, -1f, 8f), Vec3(0f, 1f, 8f),
            color = 0xFFFF0000.toInt(),
        )
        val mesh = Mesh(listOf(far, near)) // declare far first; nearest must still win

        val hit = RayMesh.firstHit(Vec3(0f, 0f, 0f), Vec3(0f, 0f, 1f), mesh, maxT = 20f)

        assertNotNull(hit)
        assertEquals(5f, hit.t, 1e-4f)
        assertEquals(near, hit.face)
    }

    @Test
    fun firstHitRespectsMaxT() {
        // Hit at t=5, but maxT=4 cuts it off.
        val hit = RayMesh.firstHit(Vec3(0f, 0f, 0f), Vec3(0f, 0f, 1f), triangle, maxT = 4f)
        assertEquals(null, hit)
    }

    @Test
    fun firstHitConsistentWithIsOccluded() {
        // For a handful of rays, firstHit != null iff isOccluded(maxT = Float.MAX_VALUE).
        val origins = listOf(Vec3(0f, 0f, 0f), Vec3(0.5f, 0.2f, 0f), Vec3(-2f, -2f, 0f))
        val dirs = listOf(
            Vec3(0f, 0f, 1f),
            Vec3(0.2f, 0.1f, 1f).normalize(),
            Vec3(0.9f, 0.9f, 1f).normalize(),
        )
        for (o in origins) for (d in dirs) {
            val occ = RayMesh.isOccluded(o, d, triangle, maxT = Float.MAX_VALUE)
            val hit = RayMesh.firstHit(o, d, triangle, maxT = Float.MAX_VALUE)
            assertEquals(occ, hit != null, "mismatch at origin=$o dir=$d")
        }
    }
}
