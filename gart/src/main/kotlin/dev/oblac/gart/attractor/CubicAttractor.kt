package dev.oblac.gart.attractor

import org.jetbrains.skia.Point3

class CubicAttractor(
    val input: String
) : Attractor {
    init {
        if (input.length != 20) {
            throw IllegalArgumentException("Input must be 20 characters long")
        }
    }

    private val a = buildCoefficients(input)

    override fun compute(p: Point3, dt: Float): Point3 {
        val x = p.x
        val y = p.y
        val z = p.z

        val x1 =
            a[0] + a[1] * x + a[2] * x * x + a[3] * x * x * x + a[4] * x * x * y + a[5] * x * y + a[6] * x * y * y + a[7] * y + a[8] * y * y + a[9] * y * y * y
        val y1 =
            a[10] + a[11] * x + a[12] * x * x + a[13] * x * x * x + a[14] * x * x * y + a[15] * x * y + a[16] * x * y * y + a[17] * y + a[18] * y * y + a[19] * y * y * y
        val z1 = z

        return Point3(x1, y1, z1)
    }

    companion object {
        val initialPoint = Point3(0f, 0f, 0f)
        val ONE = CubicAttractor("ISMHQCHPDFKFBKEALIFD")
    }
}
