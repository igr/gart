package dev.oblac.gart.tri3d

import dev.oblac.gart.vector.Vec3

/**
 * Camera extrinsics: world-space position plus orientation as yaw (around Y)
 * and pitch (around X). Pairs with [Camera], which holds the projection
 * intrinsics (screen center, scale, focal distance).
 *
 * The view transform applied by [toCameraSpace] is: translate by `-position`,
 * rotate by `-yaw` around Y, then rotate by `pitch` around X.
 */
data class CameraPose(
    val position: Vec3,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
) {
    fun toCameraSpace(p: Vec3): Vec3 =
        rotateX(rotateY(p - position, -yaw), pitch)

    fun toCameraSpace(face: Face): Face = Face(
        toCameraSpace(face.a),
        toCameraSpace(face.b),
        toCameraSpace(face.c),
        face.color,
    )

    fun toCameraSpace(mesh: Mesh): Mesh = Mesh(mesh.faces.map(::toCameraSpace))
}
