package dev.oblac.gart.attractor

import org.jetbrains.skia.Point3

class FourWingAttractor(
    val a: Float = 0.2f,
    val b: Float = -0.01f,
    val c: Float = -0.4f,
) : Attractor {
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = x + dt * (a * x + y * z)
        val y1 = y + dt * (b * x + c * y - x * z)
        val z1 = z + dt * (-z - x * y)

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(1.3f, 0.18f, -0.4f)
    }
}
