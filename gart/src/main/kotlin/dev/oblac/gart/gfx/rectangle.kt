package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import org.jetbrains.skia.RRect
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

fun Rect.path(): Path {
    val points = this.points()
    return Path().apply {
        moveTo(points[0])
        lineTo(points[1])
        lineTo(points[2])
        lineTo(points[3])
        close()
    }
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

/**
 * Returns dimensions of the rectangle.
 */
fun Rect.dimension() =
    Dimension(this.width.toInt(), this.height.toInt())

fun Rect.topLeftPoint() = Point(this.left, this.top)

fun Rect.Companion.ofPWH(left: Float, top: Float, w: Float, h: Float): Rect = Rect(left, top, left + w, top + h)
fun Rect.Companion.ofPWH(left: Number, top: Number, w: Number, h: Number): Rect = Rect.makeXYWH(left.toFloat(), top.toFloat(), w.toFloat(), h.toFloat())
fun Rect.Companion.of(x0: Number, y0: Number, x1: Number, y1: Number): Rect = Rect(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat())


fun Rect.toRRect(round: Float) =
    RRect.makeLTRB(this.left, this.top, this.right, this.bottom, round, round, round, round)
