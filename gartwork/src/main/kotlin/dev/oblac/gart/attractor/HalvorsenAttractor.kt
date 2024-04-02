package dev.oblac.gart.attractor

import dev.oblac.gart.skia.Point3

class HalvorsenAttractor(
    val a: Float = 1.89f,
) : Attractor {
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = x + dt * (-a * x - 4 * y - 4 * z - y * y)
        val y1 = y + dt * (-a * y - 4 * z - 4 * x - z * z)
        val z1 = z + dt * (-a * z - 4 * x - 4 * y - x * x)

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(-1.48f, 1.51f, 2.04f)
    }
}
