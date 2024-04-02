package dev.oblac.gart.attractor

import dev.oblac.gart.skia.Point3
import kotlin.math.sin

class ThomasAttractor(val b: Float = 0.208186f) : Attractor {
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = x + dt * (sin(y) - b * x)
        val y1 = y + dt * (sin(z) - b * y)
        val z1 = z + dt * (sin(x) - b * z)

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(1.1f, 1.1f, -0.01f)
    }
}
