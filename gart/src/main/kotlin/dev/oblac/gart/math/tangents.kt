package dev.oblac.gart.math

import dev.oblac.gart.gfx.Circle
import org.jetbrains.skia.Point
import kotlin.math.*

enum class TangentType {
    EXTERNAL, INTERNAL
}

data class Tangent(
    val type: TangentType,
    val alpha: Float,
    val line: Triple<Float, Float, Float>,
    val point1: Point,
    val point2: Point
)

fun circleTangents(circle0: Circle, circle1: Circle): List<Tangent> {
    val x0: Float = circle0.x
    val y0: Float = circle0.y
    val r0: Float = circle0.radius
    val x1: Float = circle1.x
    val y1: Float = circle1.y
    val r1: Float = circle1.radius

    val tangents = mutableListOf<Tangent>()
    val dx = x1 - x0
    val dy = y1 - y0
    val d = hypot(dx, dy)
    val theta = atan2(dy, dx)

    // External (direct) tangents: s1 = +1, s2 = +1
    if (d >= abs(r0 - r1)) {
        val phiExt = try {
            acos((r0 - r1) / d)
        } catch (e: Exception) {
            0.0f
        }
        for (sign in listOf(1, -1)) {
            val alpha = theta + sign * phiExt
            val n = Pair(cos(alpha), sin(alpha))
            val P = Point(x0 + r0 * n.first, y0 + r0 * n.second)
            val Q = Point(x1 + r1 * n.first, y1 + r1 * n.second)
            val cLine = n.first * x0 + n.second * y0 + r0
            val line = Triple(n.first, n.second, cLine)
            tangents.add(Tangent(TangentType.EXTERNAL, alpha, line, P, Q))
        }
    }

    // Internal (transverse) tangents: s1 = +1, s2 = -1
    if (d >= (r0 + r1)) {
        val phiInt = try {
            acos((r0 + r1) / d)
        } catch (e: Exception) {
            0.0f
        }
        for (sign in listOf(1, -1)) {
            val alpha = theta + sign * phiInt
            val n = Pair(cos(alpha), sin(alpha))
            val P = Point(x0 + r0 * n.first, y0 + r0 * n.second)
            val Q = Point(x1 - r1 * n.first, y1 - r1 * n.second)
            val cLine = n.first * x0 + n.second * y0 + r0
            val line = Triple(n.first, n.second, cLine)
            tangents.add(Tangent(TangentType.INTERNAL, alpha, line, P, Q))
        }
    }

    return tangents
}
