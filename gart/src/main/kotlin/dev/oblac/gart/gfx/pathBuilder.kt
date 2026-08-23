package dev.oblac.gart.gfx

import org.jetbrains.skia.PathBuilder

fun PathBuilder.addCircle(circle: Circle) =
    this.addCircle(circle.center.x, circle.center.y, circle.radius)

/**
 * [PathBuilder.moveTo] when [first], [PathBuilder.lineTo] otherwise - for loops that build a
 * polyline out of sampled coordinates without a `List<Point>` in between.
 */
fun PathBuilder.lineOrMove(first: Boolean, x: Float, y: Float): PathBuilder =
    if (first) moveTo(x, y) else lineTo(x, y)
