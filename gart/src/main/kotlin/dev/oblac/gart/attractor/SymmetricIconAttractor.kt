package dev.oblac.gart.attractor

import org.jetbrains.skia.Point3

class SymmetricIconAttractor(
    val l: Float = 2.409f,
    val a: Float = -2.5f,
    val b: Float = 0f,
    val g: Float = 0.9f,
    val o: Float = 0f,
    val d: Int = 23,
) : Attractor {
    override fun compute(point: Point3, dt: Float): Point3 {
        val x = point.x
        val y = point.y
        val z = point.z

        val zzbar = x * x + y * y
        var p = a * zzbar + l

        var zReal = x
        var zImag = y

        for (j in 0 until d - 2) {
            val za = zReal * x - zImag * y
            val zb = zImag * x + zReal * y
            zReal = za
            zImag = zb
        }

        val zn = zReal * x - zImag * y

        p += b * zn

        val x1 = p * x + g * zReal - o * y
        val y1 = p * y - g * zImag + o * x
        val z1 = z  // 2D attractor, no z change

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(0.01f, 0.01f, 0f)
    }
}
