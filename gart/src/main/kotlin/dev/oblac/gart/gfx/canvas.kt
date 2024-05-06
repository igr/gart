package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point

fun Canvas.drawPoints(points: Collection<Point>, stroke: Paint) = this.drawPoints(points.toTypedArray(), stroke)

fun Canvas.drawPointsAsCircles(points: Collection<Point>, stroke: Paint, radius: Float = 2f) = points.forEach { this.drawCircle(it.x, it.y, radius, stroke) }

fun Canvas.drawLine(p1: Point, p2: Point, stroke: Paint) = this.drawLine(p1.x, p1.y, p2.x, p2.y, stroke)

fun Canvas.drawBorder(d: Dimension, width: Float, color: Int) {
    val w2 = width / 2
    val stroke = strokeOf(color, width)
    this.drawLine(0f, w2, d.wf, w2, stroke)
    this.drawLine(w2, w2, w2, d.hf - w2, stroke)
    this.drawLine(d.wf - w2, w2, d.wf - w2, d.hf - w2, stroke)
    this.drawLine(0f, d.hf - w2, d.wf, d.hf - w2, stroke)
}

fun Canvas.drawPoint(p: Point, stroke: Paint) = this.drawPoint(p.x, p.y, stroke)

fun Canvas.drawCircle(p: Point, r: Float, fill: Paint) = this.drawCircle(p.x, p.y, r, fill)
