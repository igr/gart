package dev.oblac.gart.gfx

import dev.oblac.gart.skia.Paint
import dev.oblac.gart.skia.Point
import org.jetbrains.skia.Canvas

fun Canvas.drawPoints(points: Collection<Point>, stroke: Paint) = this.drawPoints(points.toTypedArray(), stroke)

fun Canvas.drawPointsAsCircles(points: Collection<Point>, stroke: Paint, radius: Float = 2f) {
    points.forEach { this.drawCircle(it.x, it.y, radius, stroke) }
}
