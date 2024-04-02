package dev.oblac.gart.attractor

import dev.oblac.gart.skia.Point3

class QuadraticAttractor(
    val input: String
) : Attractor {
    init {
        if (input.length != 12) {
            throw IllegalArgumentException("Input must be 12 characters long")
        }
    }

    private val a = buildCoefficients(input)

    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 = a[0] + a[1] * x + a[2] * x * x + a[3] * x * y + a[4] * y + a[5] * y * y
        val y1 = a[6] + a[7] * x + a[8] * x * x + a[9] * x * y + a[10] * y + a[11] * y * y
        val z1 = z

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(0f, 0f, 0f)
        val ONE = QuadraticAttractor("CVQKGHQTPHTE")
    }
}
