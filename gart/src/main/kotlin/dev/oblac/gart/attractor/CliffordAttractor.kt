package dev.oblac.gart.attractor

import dev.oblac.gart.skia.Point3
import kotlin.math.cos
import kotlin.math.sin

class CliffordAttractor(
    val a: Float = -2f,
    val b: Float = -2.4f,
    val c: Float = 1.1f,
    val d: Float = -0.9f
) : Attractor {
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = sin(a * y) + c * cos(a * x)
        val y1 = sin(b * x) + d * cos(b * y)
        val z1 = z  // 2D attractor, no z change

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(0.1f, 0.1f, 0f)
    }

}
