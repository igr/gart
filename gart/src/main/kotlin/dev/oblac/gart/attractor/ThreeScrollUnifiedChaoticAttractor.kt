package dev.oblac.gart.attractor

import org.jetbrains.skia.Point3

class ThreeScrollUnifiedChaoticAttractor(
    val a: Float = 32.48f,
    val b: Float = 45.84f,
    val c: Float = 1.18f,
    val d: Float = 0.13f,
    val e: Float = 0.57f,
    val f: Float = 14.7f,
) : Attractor {
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = x + dt * (a * (y - x) + d * x * z)
        val y1 = y + dt * (b * x - x * z + f * y)
        val z1 = z + dt * (c * z + x * y - e * x * x)

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(-0.29f, -0.25f, -0.59f)
    }
}
