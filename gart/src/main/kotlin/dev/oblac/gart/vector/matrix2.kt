package dev.oblac.gart.vector

typealias Mtx2 = Matrix2

data class Matrix2(val a: Float, val b: Float, val c: Float, val d: Float) {
    operator fun times(v: Vector2) = Vector2(a * v.x + b * v.y, c * v.x + d * v.y)
}

