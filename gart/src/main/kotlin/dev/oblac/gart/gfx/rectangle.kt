package dev.oblac.gart.gfx

import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

/**
 * Converts rectangle to the list of four points.
 */
fun Rect.points(): Array<Point> {
    return arrayOf(
        Point(this.left, this.top),
        Point(this.left, this.bottom),
        Point(this.right, this.bottom),
        Point(this.right, this.top),
    )
}

fun Rect.move(delta: Float): Rect {
    return Rect(this.left + delta, this.top + delta, this.right + delta, this.bottom + delta)
}


/**
 * Returns a center of the rectangle.
 */
fun Rect.center(): Point {
    return Point(this.left + (this.right - this.left) / 2, this.top + (this.bottom - this.top) / 2)
}

/**
 * Returns true if the rectangle contains the given rectangle.
 */
fun Rect.contains(rect: Rect): Boolean {
    return this.left <= rect.left && this.top <= rect.top && this.right >= rect.right && this.bottom >= rect.bottom
}

/**
 * Returns a rectangle that is a third of the given rectangle.
 * Useful for dividing the rectangle into thirds.
 */
fun Rect.thirds(): Rect {
    val thirdW = (this.right - this.left) / 3
    val thirdH = (this.bottom - this.top) / 3
    return Rect(
        this.left + thirdW,
        this.top + thirdH,
        this.right - thirdW,
        this.bottom - thirdH,
    )
}

fun Rect.shrink(delta: Float) =
    Rect(this.left + delta, this.top + delta, this.right - delta, this.bottom - delta)

fun Rect.grow(delta: Float) =
    Rect(this.left - delta, this.top - delta, this.right + delta, this.bottom + delta)
