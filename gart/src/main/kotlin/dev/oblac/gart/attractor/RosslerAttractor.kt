package dev.oblac.gart.attractor

import org.jetbrains.skia.Point3

class RosslerAttractor(
    val a: Float = 0.2f,
    val b: Float = 0.2f,
    val c: Float = 5.7f,
) : Attractor {
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = x + dt * (-y - z)
        val y1 = y + dt * (x + a * y)
        val z1 = z + dt * (b + z * (x - c))

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(10f, 0.0f, 10.0f)
    }
}
