package dev.oblac.gart.gfx

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point

fun Canvas.drawPoints(points: Collection<Point>, stroke: Paint) = this.drawPoints(points.toTypedArray(), stroke)

fun Canvas.drawPointsAsCircles(points: Collection<Point>, stroke: Paint, radius: Float = 2f) {
    points.forEach { this.drawCircle(it.x, it.y, radius, stroke) }
}
