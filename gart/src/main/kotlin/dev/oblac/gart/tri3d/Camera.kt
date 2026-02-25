package dev.oblac.gart.tri3d

import dev.oblac.gart.vector.Vec3
import org.jetbrains.skia.Point

class Camera(
    val screenCx: Float,
    val screenCy: Float,
    val scale: Float,
    val distance: Float,
) {
    fun project(v: Vec3): Point {
        val s = distance / (distance + v.z)
        return Point(screenCx + v.x * scale * s, screenCy + v.y * scale * s)
    }

    /** Camera eye position (looking toward +z). */
    private val eye = Vec3(0f, 0f, -distance)

    /**
     * Returns the depth of a point from the camera eye.
     * The camera eye is at z = -distance, so depth = distance + v.z.
     * Larger values are farther from the camera.
     */
    fun depth(v: Vec3): Float = distance + v.z

    /**
     * Backface culling test.
     * Returns true if the face is facing the camera (i.e. visible).
     * Uses the face normal from vertex winding order and the vector
     * from the face centroid to the camera eye.
     */
    fun isFrontFacing(face: Face): Boolean {
        val normal = face.normal()
        val centroid = (face.a + face.b + face.c) / 3f
        val toCamera = eye - centroid
        return normal.dot(toCamera) < 0f
    }


    /**
     * Painter's Algorithm helper.
     *
     * Provides a stable, deterministic back-to-front comparator without relying on grouping.
     *
     * Sorting strategy (far -> near):
     *  1) maxDepth
     *  2) minDepth
     *  3) avgDepth
     *  4) stable deterministic id (ascending)
     *
     * Use a stable sort (Kotlin stdlib sorting is stable) together with this comparator.
     */
    data class TriangleDepthKey(
        val maxDepth: Float,
        val minDepth: Float,
        val avgDepth: Float,
        val id: Int,
    )

    fun triangleDepthKey(a: Vec3, b: Vec3, c: Vec3, id: Int): TriangleDepthKey {
        val da = depth(a)
        val db = depth(b)
        val dc = depth(c)
        val maxD = maxOf(da, db, dc)
        val minD = minOf(da, db, dc)
        val avgD = (da + db + dc) / 3f
        return TriangleDepthKey(maxD, minD, avgD, id)
    }

    private fun cmpDesc(a: Float, b: Float, eps: Float): Int {
        val d = a - b
        if (kotlin.math.abs(d) <= eps) return 0
        // Descending: larger depth (farther) first
        return if (d > 0f) -1 else 1
    }

    /**
     * Generic painter comparator for any triangle-like type.
     *
     * Example usage:
     * `triangles.sortedWith(camera.painterComparator({ it.a to it.b to it.c }, { it.id }))`
     */
    fun <T> painterComparator(
        vertices: (T) -> Triple<Vec3, Vec3, Vec3>,
        id: (T) -> Int,
        eps: Float = 1e-5f,
    ): Comparator<T> = Comparator { t1, t2 ->
        val (a1, b1, c1) = vertices(t1)
        val (a2, b2, c2) = vertices(t2)

        val k1 = triangleDepthKey(a1, b1, c1, id(t1))
        val k2 = triangleDepthKey(a2, b2, c2, id(t2))

        var r = cmpDesc(k1.maxDepth, k2.maxDepth, eps)
        if (r != 0) return@Comparator r

        r = cmpDesc(k1.minDepth, k2.minDepth, eps)
        if (r != 0) return@Comparator r

        r = cmpDesc(k1.avgDepth, k2.avgDepth, eps)
        if (r != 0) return@Comparator r

        // Deterministic tie-break: smaller id first
        return@Comparator k1.id.compareTo(k2.id)
    }
}
