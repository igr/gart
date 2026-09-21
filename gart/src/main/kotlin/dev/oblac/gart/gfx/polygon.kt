package dev.oblac.gart.gfx

import org.jetbrains.skia.Point

/**
 * Returns the signed area of the closed polygon through [poly], with the shoelace formula.
 * The last point connects back to the first.
 *
 * The sign gives the winding: positive when the points go clockwise on screen (y down),
 * negative when they go counter-clockwise.
 */
fun signedArea(poly: List<Point>): Float {
    var s = 0f
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[(i + 1) % poly.size]
        s += a.x * b.y - b.x * a.y
    }
    return s / 2f
}
