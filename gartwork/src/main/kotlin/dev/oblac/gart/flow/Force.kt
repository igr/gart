package dev.oblac.gart.flow

import dev.oblac.gart.skia.Point

interface Force<T : Force<T>> {
    val direction: Float
    val magnitude: Float
    fun offset(p: Point): Point
    operator fun plus(other: T): T
}
