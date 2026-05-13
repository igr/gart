package dev.oblac.gart.tri3d

import dev.oblac.gart.vector.Vec3

/**
 * Closest-hit result: parametric distance [t] along the ray and the [face]
 * that was hit. The hit point is `origin + t * dir`.
 */
data class Hit(val t: Float, val face: Face)

object RayMesh {

    /**
     * Möller-Trumbore "any hit before maxT" test against the mesh.
     *
     * Returns true as soon as any face in [mesh] is intersected at a parametric
     * distance t in (epsilon, maxT). The ray is parameterised as
     * `origin + t * dir`; if [dir] is normalised, t is in world-space units.
     *
     * @param epsilon  rejects near-zero t (self-intersection guard) and degenerate
     *                 (parallel) hits. Same value used for both.
     */
    fun isOccluded(
        origin: Vec3,
        dir: Vec3,
        mesh: Mesh,
        maxT: Float,
        epsilon: Float = 1e-4f,
    ): Boolean {
        for (face in mesh.faces) {
            val t = intersectT(origin, dir, face, maxT, epsilon)
            if (t != null) return true
        }
        return false
    }

    /**
     * Closest-hit ray-mesh intersection. Returns the [Hit] with the smallest
     * t in (epsilon, maxT), or null if the ray misses every face.
     */
    fun firstHit(
        origin: Vec3,
        dir: Vec3,
        mesh: Mesh,
        maxT: Float = Float.MAX_VALUE,
        epsilon: Float = 1e-4f,
    ): Hit? {
        var bestT = Float.MAX_VALUE
        var bestFace: Face? = null
        for (face in mesh.faces) {
            val t = intersectT(origin, dir, face, maxT, epsilon) ?: continue
            if (t < bestT) {
                bestT = t
                bestFace = face
            }
        }
        return bestFace?.let { Hit(bestT, it) }
    }

    /**
     * Möller-Trumbore parametric intersection. Returns t in (epsilon, maxT)
     * if the ray hits the face, else null.
     */
    private fun intersectT(
        origin: Vec3,
        dir: Vec3,
        face: Face,
        maxT: Float,
        epsilon: Float,
    ): Float? {
        val edge1 = face.b - face.a
        val edge2 = face.c - face.a
        val h = dir.cross(edge2)
        val a = edge1.dot(h)
        if (a > -epsilon && a < epsilon) return null // ray parallel to triangle

        val f = 1f / a
        val s = origin - face.a
        val u = f * s.dot(h)
        if (u < 0f || u > 1f) return null

        val q = s.cross(edge1)
        val v = f * dir.dot(q)
        if (v < 0f || u + v > 1f) return null

        val t = f * edge2.dot(q)
        return if (t > epsilon && t < maxT) t else null
    }
}
