package dev.oblac.gart.gfx

import dev.oblac.gart.math.fastSqrt
import org.jetbrains.skia.Point

fun intersectionsOf(
    line: Line,
    circle: Circle
): Array<Point> {
    val x1 = line.a.x
    val y1 = line.a.y
    val x2 = line.b.x
    val y2 = line.b.y
    val cx = circle.x
    val cy = circle.y
    val r = circle.radius

    val dx = x2 - x1
    val dy = y2 - y1

    // Quadratic equation coefficients
    val a = dx * dx + dy * dy
    val b = 2 * (dx * (x1 - cx) + dy * (y1 - cy))
    val c = (x1 - cx) * (x1 - cx) + (y1 - cy) * (y1 - cy) - r * r

    // Compute discriminant
    val discriminant = b * b - 4 * a * c
    if (discriminant < 0) return arrayOf<Point>() // No intersection

    val sqrtD = fastSqrt(discriminant)
    val t1 = (-b + sqrtD) / (2 * a)
    val t2 = (-b - sqrtD) / (2 * a)

    // Compute intersection points
    val intersections = mutableListOf<Point>()
    for (t in listOf(t1, t2)) {
        if (t in 0.0..1.0) { // Ensure the intersection is within the segment
            val x = x1 + t * dx
            val y = y1 + t * dy
            intersections.add(Point(x, y))
        }
    }

    return intersections.toTypedArray()
}

fun intersectionOf(
    line1: Line,
    line2: Line
): Point? {
    val x1 = line1.a.x
    val y1 = line1.a.y
    val x2 = line1.b.x
    val y2 = line1.b.y
    val x3 = line2.a.x
    val y3 = line2.a.y
    val x4 = line2.b.x
    val y4 = line2.b.y

    // Calculate the determinants
    val denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
    if (denom == 0f) return null // Lines are parallel

    val tNum = (x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)
    val uNum = (x1 - x3) * (y1 - y2) - (y1 - y3) * (x1 - x2)

    // Normalize parameters
    val t = tNum / denom
    val u = uNum / denom

    // Ensure intersection is within segment bounds
    if (t in 0f..1f && u in 0f..1f) {
        val intersectionX = x1 + t * (x2 - x1)
        val intersectionY = y1 + t * (y2 - y1)
        return Point(intersectionX, intersectionY)
    }

    return null // No intersection within the segments
}

// todo calculate intersection point directly without creating a temporary line (!)
fun intersectionOf(
    dline: DLine,
    line2: Line
): Point? {
    val rayLine = dline.toLine(dline.p, 10_000f)    // grow in both directions
    return intersectionOf(rayLine, line2)
}

/**
 * Returns true if open segments `p1→p2` and `p3→p4` cross properly (no shared
 * endpoint or collinear touch counts). Allocation-free orientation-test variant
 * suitable for hot loops; for the intersection point itself use [intersectionOf].
 */
fun segmentsCross(
    p1x: Float, p1y: Float, p2x: Float, p2y: Float,
    p3x: Float, p3y: Float, p4x: Float, p4y: Float,
): Boolean {
    fun orient(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Int {
        val v = (by - ay) * (cx - bx) - (bx - ax) * (cy - by)
        return when {
            v > 0f -> 1
            v < 0f -> -1
            else -> 0
        }
    }
    val o1 = orient(p1x, p1y, p2x, p2y, p3x, p3y)
    val o2 = orient(p1x, p1y, p2x, p2y, p4x, p4y)
    val o3 = orient(p3x, p3y, p4x, p4y, p1x, p1y)
    val o4 = orient(p3x, p3y, p4x, p4y, p2x, p2y)
    return o1 != o2 && o3 != o4 && o1 != 0 && o2 != 0 && o3 != 0 && o4 != 0
}

fun segmentsCross(a1: Point, a2: Point, b1: Point, b2: Point): Boolean =
    segmentsCross(a1.x, a1.y, a2.x, a2.y, b1.x, b1.y, b2.x, b2.y)
