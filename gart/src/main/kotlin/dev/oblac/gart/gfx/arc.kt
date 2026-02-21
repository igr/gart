package dev.oblac.gart.gfx

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect

fun Canvas.drawArc(rect: Rect, startAngle: Float, sweepAngle: Float, includeCenter: Boolean, paint: Paint) {
    this.drawArc(rect.left, rect.top, rect.right, rect.bottom, startAngle, sweepAngle, includeCenter, paint)
}
