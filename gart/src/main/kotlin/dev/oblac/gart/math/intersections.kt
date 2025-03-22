package dev.oblac.gart.math

import dev.oblac.gart.gfx.Circle
import dev.oblac.gart.gfx.Line
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
