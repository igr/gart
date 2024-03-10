package dev.oblac.gart.gfx

import dev.oblac.gart.skia.Point
import dev.oblac.gart.skia.Rect

/**
 * Converts rectangle to list of four points.
 */
fun Rect.points(): Array<Point> {
    return arrayOf(
        Point(this.left, this.top),
        Point(this.left, this.bottom),
        Point(this.right, this.bottom),
        Point(this.right, this.top),
    )
}

/**
 * Returns a center of the rectangle.
 */
fun Rect.center(): Point {
    return Point(this.left + (this.right - this.left) / 2, this.top + (this.bottom - this.top) / 2)
}
