package dev.oblac.gart.attractor

import dev.oblac.gart.skia.Point3

class LorenzAttractor(
    val sigma: Float = 10f,
    val rho: Float = 28f,
    val beta: Float = 8f / 3f
) : Attractor {
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = x + dt * (sigma * (y - x))
        val y1 = y + dt * (x * (rho - z) - y)
        val z1 = z + dt * (x * y - beta * z)

        return Point3(x1, y1, z1)
    }

    companion object {
        // val STRANGE = LorentzAttractor(10f, 99.96f, 2.666f)
        // val BUTTERFLY = LorentzAttractor(10f, 28f, 2.666f)
        val initialPoint = Point3(0f, 20f, 12f)
    }
}
