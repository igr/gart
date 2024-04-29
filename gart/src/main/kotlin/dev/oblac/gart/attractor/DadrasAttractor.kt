package dev.oblac.gart.attractor

import org.jetbrains.skia.Point3

class DadrasAttractor(
    val a: Float = 3f,
    val b: Float = 2.7f,
    val c: Float = 1.7f,
    val d: Float = 2f,
    val e: Float = 9f
) : Attractor {
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = x + dt * (y - a * x + b * y * z)
        val y1 = y + dt * (c * y - x * z + z)
        val z1 = z + dt * (d * x * y - e * z)

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(1.1f, 2.1f, -2.0f)
    }
}
