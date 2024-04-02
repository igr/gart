package dev.oblac.gart.attractor

import dev.oblac.gart.skia.Point3

class LangfordAizawaAttractor(
    val a: Float = 0.95f,
    val b: Float = 0.7f,
    val c: Float = 0.6f,
    val d: Float = 3.5f,
    val e: Float = 0.25f,
    val f: Float = 0.1f
) : Attractor {
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = x + dt * ((z - b) * x - d * y)
        val y1 = y + dt * (d * x + (z - b) * y)
        val z1 = z + dt * (c + a * z - (z * z * z) / 3 - (x * x + y * y) * (1 + e * z) + f * z * x * x * x)

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(0.1f, 1f, 0.01f)
    }
}
