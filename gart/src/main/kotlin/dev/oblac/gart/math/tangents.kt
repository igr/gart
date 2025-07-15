package dev.oblac.gart.math

import dev.oblac.gart.gfx.Circle
import dev.oblac.gart.gfx.Line
import org.jetbrains.skia.Point
import kotlin.math.*

enum class TangentType {
    EXTERNAL, INTERNAL
}

data class Tangent(
    val type: TangentType,
    val alpha: Float, // the angle (in radians) for the unit normal n
    val line: Triple<Float, Float, Float>, //  a tuple (a, b, c) representing the line a*x + b*y = c
    val point1: Point,
    val point2: Point
) {
    fun toLine(): Line = Line(point1, point2)
}

/**
 * Calculate the tangents between two circles.
 */
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

/**
 * Calculate the tangent lines from an external point to a circle.
 * Returns a list of tangent lines (usually 2) from the point to the circle.
 */
fun pointToCircleTangents(point: Point, circle: Circle): List<Line> {
    val px = point.x
    val py = point.y
    val cx = circle.x
    val cy = circle.y
    val r = circle.radius

    // Vector from external point to circle center
    val dx = cx - px
    val dy = cy - py
    val d = hypot(dx, dy)

    // If point is inside the circle, no tangents exist
    if (d <= r) return emptyList()

    // If point is on the circle, only one tangent exists
    if (abs(d - r) < 0.001f) {
        val tangentLine = circle.tangentAtPoint(point)
        // Convert DLine to Line by extending it
        val start = tangentLine.pointFromEnd(100f)
        val end = tangentLine.pointFromStart(100f)
        return listOf(Line(start, end))
    }

    // Calculate the tangent points using the correct geometric formula
    // Based on the fact that tangent is perpendicular to radius at point of tangency

    // Distance squared from external point to center
    val dSquared = d * d
    val rSquared = r * r

    // Calculate the two tangent points on the circle
    // Using the formula for external tangents to a circle
    val a = rSquared / dSquared
    val b = r * sqrt(dSquared - rSquared) / dSquared

    // First tangent point
    val t1x = cx + a * (px - cx) + b * (py - cy)
    val t1y = cy + a * (py - cy) - b * (px - cx)
    val tangentPoint1 = Point(t1x, t1y)

    // Second tangent point
    val t2x = cx + a * (px - cx) - b * (py - cy)
    val t2y = cy + a * (py - cy) + b * (px - cx)
    val tangentPoint2 = Point(t2x, t2y)

    // Create tangent lines from external point to tangent points
    return listOf(
        Line(point, tangentPoint1),
        Line(point, tangentPoint2)
    )
}
