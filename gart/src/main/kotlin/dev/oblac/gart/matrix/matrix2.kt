package dev.oblac.gart.matrix

import dev.oblac.gart.vector.Vec2

data class Matrix22(val a: Float, val b: Float, val c: Float, val d: Float) {
    operator fun times(v: Vec2) = Vec2(a * v.x + b * v.y, c * v.x + d * v.y)
}

