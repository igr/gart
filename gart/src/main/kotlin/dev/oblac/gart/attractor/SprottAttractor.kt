package dev.oblac.gart.attractor

import org.jetbrains.skia.Point3

class SprottAttractor(
    val a: Float = 2.07f,
    val b: Float = 1.79f,
) : Attractor {
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = x + dt * (y + a * x * y + x * z)
        val y1 = y + dt * (1 - b * x * x + y * z)
        val z1 = z + dt * (x - x * x - y * y)

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(0.63f, 0.47f, -0.54f)
    }
}
