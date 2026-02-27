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
}
