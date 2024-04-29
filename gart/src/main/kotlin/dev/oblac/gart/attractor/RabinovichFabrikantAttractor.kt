package dev.oblac.gart.attractor

import org.jetbrains.skia.Point3

class RabinovichFabrikantAttractor(
    val alpha: Float = 0.14f,
    val gamma: Float = 0.10f,
) : Attractor {
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = x + dt * (y * (z - 1 + x * x) + gamma * x)
        val y1 = y + dt * (x * (3 * z + 1 - x * x) + gamma * y)
        val z1 = z + dt * (-2 * z * (alpha + x * y))

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(-1.0f, 0.0f, 0.5f)
    }
}
