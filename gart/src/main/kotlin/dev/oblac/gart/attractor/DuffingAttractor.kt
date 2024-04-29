package dev.oblac.gart.attractor

import org.jetbrains.skia.Point3
import kotlin.math.cos

class DuffingAttractor(
    val a: Float = 0.35f,
    val b: Float = 0.3f,
    val w: Float = 1f
) : Attractor {

    var t = 0f
    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = x + dt * y
        val y1 = y + dt * (x - x * x * x - a * y + b * cos(w * t))
        val z1 = 0f  // 2D attractor, no z change

        t += dt

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(0.1f, 0.1f, 0f)
    }
}
