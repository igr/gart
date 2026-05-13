package dev.oblac.gart.tri3d

import dev.oblac.gart.vector.Vec3
import org.jetbrains.skia.Point
import kotlin.math.sqrt

class Camera(
    val screenCx: Float,
    val screenCy: Float,
    val scale: Float,
    val distance: Float,
) {

    val focalLength = scale * distance

    /** Camera eye position (looking toward +z). */
    val eye = Vec3(0f, 0f, -distance)

    fun project(v: Vec3): Point {
        val s = distance / (distance + v.z)
        return Point(screenCx + v.x * scale * s, screenCy + v.y * scale * s)
    }

    /**
     * Inverse of [project]: given a screen-space (x, y) and the ZBuffer
     * depth value at that pixel (= distance + v.z), reconstructs the
     * world-space [Vec3] that projects to that screen point.
     *
     * @param depth value as stored in the z-buffer; must be positive
     */
    fun unproject(screenX: Float, screenY: Float, depth: Float): Vec3 {
        val vz = depth - distance
        // From project: screenX = screenCx + v.x * scale * (distance / depth)
        // So v.x = (screenX - screenCx) * depth / (scale * distance)
        val k = depth / (scale * distance)
        val vx = (screenX - screenCx) * k
        val vy = (screenY - screenCy) * k
        return Vec3(vx, vy, vz)
    }

    /**
     * Normalized world-space ray direction from [eye] through pixel (screenX, screenY).
     * Equivalent to `normalize(unproject(screenX, screenY, d) - eye)` for any d > 0.
     */
    fun rayDirection(screenX: Float, screenY: Float): Vec3 {
        // Use depth = distance (i.e. v.z = 0, the focal plane). At that depth,
        // s = 1, so v.x = (screenX - screenCx) / scale, v.y = (screenY - screenCy) / scale.
        // Ray from eye (0,0,-distance) through (vx, vy, 0) has direction (vx, vy, distance).
        val dx = (screenX - screenCx) / scale
        val dy = (screenY - screenCy) / scale
        val dz = distance
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        return Vec3(dx / len, dy / len, dz / len)
    }

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
