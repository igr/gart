package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RRect

fun Canvas.drawBorder(d: Dimension, stroke: Paint) {
    val width = stroke.strokeWidth
    val w2 = width / 2
    this.drawLine(0f, w2, d.wf, w2, stroke)
    this.drawLine(w2, w2, w2, d.hf - w2, stroke)
    this.drawLine(d.wf - w2, w2, d.wf - w2, d.hf - w2, stroke)
    this.drawLine(0f, d.hf - w2, d.wf, d.hf - w2, stroke)
}

fun Canvas.drawBorder(d: Dimension, width: Float, color: Int) {
    this.drawBorder(d, strokeOf(color, width))
}

fun Canvas.drawRoundBorder(d: Dimension, radius: Float = 10f, width: Float = 20f, color: Int) {
    val x = width / 2
    val rr = RRect.makeXYWH(x, x, d.wf - x * 2, d.hf - x * 2, radius, radius)
    this.save()
    this.clipRRect(rr, ClipMode.DIFFERENCE, true)
    this.clear(color)
    this.restore()
}
