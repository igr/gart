package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import org.jetbrains.skia.*

fun Canvas.drawPoints(points: Collection<Point>, stroke: Paint) = this.drawPoints(points.toTypedArray(), stroke)

fun Canvas.drawPointsAsCircles(points: Collection<Point>, stroke: Paint, radius: Float = 2f) = points.forEach { this.drawCircle(it.x, it.y, radius, stroke) }

fun Canvas.drawLine(p1: Point, p2: Point, stroke: Paint) = this.drawLine(p1.x, p1.y, p2.x, p2.y, stroke)

fun Canvas.drawBorder(d: Dimension, width: Float, color: Int) {
    this.drawBorder(d, strokeOf(color, width))
}

fun Canvas.drawBorder(d: Dimension, stroke: Paint) {
    val width = stroke.strokeWidth
    val w2 = width / 2
    this.drawLine(0f, w2, d.wf, w2, stroke)
    this.drawLine(w2, w2, w2, d.hf - w2, stroke)
    this.drawLine(d.wf - w2, w2, d.wf - w2, d.hf - w2, stroke)
    this.drawLine(0f, d.hf - w2, d.wf, d.hf - w2, stroke)
}

fun Canvas.drawRoundBorder(d: Dimension, radius: Float = 10f, width: Float = 20f, color: Int) {
    val x = width / 2
    val rr = RRect.makeXYWH(x, x, d.wf - x * 2, d.hf - x * 2, radius, radius)
    this.save()
    this.clipRRect(rr, ClipMode.DIFFERENCE, true)
    this.clear(color)
    this.restore()
}

fun Canvas.drawPoint(p: Point, stroke: Paint) = this.drawPoint(p.x, p.y, stroke)

fun Canvas.drawCircle(p: Point, r: Float, fill: Paint) = this.drawCircle(p.x, p.y, r, fill)

fun Canvas.drawBitmap(b: Gartmap) = this.drawImage(b.image(), 0f, 0f)

fun Canvas.drawImage(image: Image) = drawImage(image, 0f, 0f)

fun Canvas.drawArc(rect: Rect, startAngle: Float, sweepAngle: Float, includeCenter: Boolean, paint: Paint) {
    this.drawArc(rect.left, rect.top, rect.right, rect.bottom, startAngle, sweepAngle, includeCenter, paint)
}

/**
 * Draws the Gartvas image to the canvas.
 */
fun Canvas.draw(g: Gartvas) = drawImage(g.snapshot(), 0f, 0f)

/**
 * Saves a new layer with the specified image filter applied.
 */
fun Canvas.saveLayer(imageFilter: ImageFilter) = this.saveLayer(null, paintOfImageFilter(imageFilter))


fun Canvas.clipCircle(circle: Circle) = clipPath(circle.toPath())
