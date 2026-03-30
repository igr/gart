package dev.oblac.gart.vector

data class Vec4(val x: Float, val y: Float, val z: Float, val w: Float) {
    companion object {
        fun of(vec3: Vec3, w: Float) = Vec4(vec3.x, vec3.y, vec3.z, w)
    }
}

