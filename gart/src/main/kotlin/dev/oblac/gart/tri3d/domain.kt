package dev.oblac.gart.tri3d

import dev.oblac.gart.vector.Vec3

/**
 * A triangle in 3D space, defined by its three vertices.
 * The winding order (a, b, c) defines the face normal via cross product:
 * normal = (b - a) x (c - a).
 */
data class Face(
    val a: Vec3,
    val b: Vec3,
    val c: Vec3,
    val color: Int,
) {
    /**
     * Face normal derived from vertex winding order.
     */
    fun normal(): Vec3 = (b - a).cross(c - a)
}

data class Mesh(
    val faces: List<Face>,
)
