package dev.oblac.gart.vector

typealias Vec4 = Vector4

data class Vector4(val x: Float, val y: Float, val z: Float, val w: Float) {
    companion object {
        fun of(vector3: Vector3, w: Float) = Vector4(vector3.x, vector3.y, vector3.z, w)
    }
}

