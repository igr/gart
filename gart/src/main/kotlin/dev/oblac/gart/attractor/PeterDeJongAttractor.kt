package dev.oblac.gart.attractor

import org.jetbrains.skia.Point3
import kotlin.math.cos
import kotlin.math.sin

class PeterDeJongAttractor(
    val a: Float = 0.97f,
    val b: Float = -1.899f,
    val c: Float = 1.381f,
    val d: Float = -1.506f
) : Attractor {
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = sin(a * y) - cos(b * x)
        val y1 = sin(c * x) - cos(d * y)
        val z1 = z  // 2D attractor, no z change

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(0.1f, 0.1f, 0f)
    }
}
