package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import dev.oblac.gart.angle.Angle
import org.jetbrains.skia.*
import org.jetbrains.skia.Point

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
    return PathBuilder().apply {
        moveTo(points[0])
        lineTo(points[1])
        lineTo(points[2])
        lineTo(points[3])
        closePath()
    }.detach()
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

fun Rect.contains(point: Point): Boolean {
    return this.left <= point.x && this.top <= point.y && this.right >= point.x && this.bottom >= point.y
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
fun Rect.topRightPoint() = Point(this.right, this.top)
fun Rect.bottomLeftPoint() = Point(this.left, this.bottom)
fun Rect.bottomRightPoint() = Point(this.right, this.top)
fun Rect.leftSide() = Line(this.topLeftPoint(), this.bottomLeftPoint())

fun Rect.Companion.ofXYWH(left: Float, top: Float, w: Float, h: Float): Rect = Rect(left, top, left + w, top + h)
fun Rect.Companion.ofXYWH(left: Float, top: Float, d: Dimension): Rect = Rect(left, top, left + d.w, top + d.h)
fun Rect.Companion.ofXYWH(left: Number, top: Number, w: Number, h: Number): Rect = Rect.makeXYWH(left.toFloat(), top.toFloat(), w.toFloat(), h.toFloat())
fun Rect.Companion.ofXYWH(p: Point, w: Number, h: Number): Rect = Rect.makeXYWH(p.x, p.y, w.toFloat(), h.toFloat())
fun Rect.Companion.of(x0: Number, y0: Number, x1: Number, y1: Number): Rect = Rect(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat())
fun Rect.Companion.ofCenter(center: Point, width: Float, height: Float) = makeXYWH(
    center.x - width / 2,
    center.y - height / 2,
    width,
    height
)

fun Rect.toRRect(round: Float) =
    RRect.makeLTRB(this.left, this.top, this.right, this.bottom, round, round, round, round)

val Rect.Companion.EMPTY: Rect
    get() = Rect(0.0f, 0.0f, 0.0f, 0.0f)

fun Canvas.drawRectWH(x: Float, y: Float, w: Float, h: Float, paint: Paint) {
    this.drawRect(x, y, x + w, y + h, paint)
}

/**
 * Draws a rectangle centered at the given point, rotated by the specified angle.
 *
 * @param center The center point of the rectangle
 * @param width The width of the rectangle
 * @param height The height of the rectangle
 * @param angle The rotation angle around the center point
 * @param paint The paint to use for drawing
 */
fun Canvas.drawRotatedRect(center: Point, width: Float, height: Float, angle: Angle, paint: Paint) {
    this.save()
    this.rotate(angle.degrees, center.x, center.y)
    val rect = Rect.ofCenter(center, width, height)
    this.drawRect(rect, paint)
    this.restore()
}
