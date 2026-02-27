package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.i
import dev.oblac.gart.text.drawStringInRect
import dev.oblac.gart.util.forSequence
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.PathBuilder
import kotlin.math.sin

private val stroke = strokeOf(NipponColors.col029_GINSYU, 6f).apply {
    this.imageFilter = ImageFilter.makeDropShadow(19f, 19f, 23f, 23f, NipponColors.col044_BENIHI)
}

val slide00 = DrawFrame { c, d, f ->
    c.clear(NipponColors.col190_HANADA)

    val path = PathBuilder()
    val yForm = d.hf * 0.3f
    val yTo = d.hf * 0.7f
    val frequency = PIf * 4 / d.wf
    forSequence(yForm.i(), yTo.i(), 20).forEachIndexed { i, y ->
        val amplitude = 100f + i * 10f
        path.moveTo(Point(0, y))
        val step = 4
        val yDelta = (y - yForm)
        for (x in 0 until d.w step step) {
            val py = y + amplitude * sin(frequency * x * 6 + (f.timeSeconds * yDelta * 0.01f) / 10 + i * 0.1f + yDelta * 0.2f)
            path.lineTo(Point(x.toFloat() * step, py))
        }
        val pd = path.detach()
        c.drawPath(pd, stroke)
    }
    c.drawStringInRect("Skiko", activeRect, introFont, titleColor.toFillPaint())
}
