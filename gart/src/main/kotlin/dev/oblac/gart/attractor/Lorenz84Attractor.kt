package dev.oblac.gart.attractor

import org.jetbrains.skia.Point3

class Lorenz84Attractor(
    val a: Float = 0.95f,
    val b: Float = 7.91f,
    val f: Float = 4.83f,
    val g: Float = 4.66f,
) : Attractor {
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = x + dt * (-a * x - y * y - z * z + a * f)
        val y1 = y + dt * (-y + x * y - b * x * z + g)
        val z1 = z + dt * (-z + b * x * y + x * z)

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(-0.2f, -2.82f, 2.71f)
    }
}
