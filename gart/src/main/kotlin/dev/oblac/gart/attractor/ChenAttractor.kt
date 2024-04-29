package dev.oblac.gart.attractor

import org.jetbrains.skia.Point3

class ChenAttractor(
    val alpha: Float = 5.0f,
    val beta: Float = -10.0f,
    val delta: Float = -0.38f
) : Attractor {
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = x + dt * (alpha * x - y * z)
        val y1 = y + dt * (beta * y + x * z)
        val z1 = z + dt * (delta * z + x * y / 3.0f)

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(5f, 10f, 10f)
    }
}
