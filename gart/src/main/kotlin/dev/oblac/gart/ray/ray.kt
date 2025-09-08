package dev.oblac.gart.ray

import dev.oblac.gart.gfx.DLine
import org.jetbrains.skia.Point

data class Ray(
    val dline: DLine,
    val intensity: Float = 1.0f
)

data class RayTrace(
    val iteration: Int,
    val ray: Ray,
    val from: Point,
    val to: Point?
)
