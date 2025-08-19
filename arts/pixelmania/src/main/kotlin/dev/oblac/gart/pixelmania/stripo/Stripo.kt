package dev.oblac.gart.pixelmania.stripo

import dev.oblac.gart.*
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.Line
import dev.oblac.gart.gfx.draw
import dev.oblac.gart.gfx.drawLine
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.pixels.halftone.HalftoneConfiguration
import dev.oblac.gart.pixels.halftone.halftoneProcess
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PathEffect
import org.jetbrains.skia.Shader

fun main() {
    val gart = Gart.of("stripo", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw(g)

    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDraw(val g: Gartvas) : Drawing(g) {
    val b = Gartmap(g)
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        draw(g.canvas, d)
        b.updatePixelsFromCanvas()
        val m = halftoneProcess(b, HalftoneConfiguration(
            dotSize = 14,
            dotResolution = 7
        ))
        b.copyPixelsFrom(m)
        b.drawToCanvas()
        c.draw(g)
    }
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.white01)

    val total = 10
    val gap = 48f
    val lineW = (d.h - 2 * gap) / total
    repeat(total) {
        val line = if (it % 2 == 0) {
            Line.of(
                x1 = gap,
                y1 = gap + it * lineW + lineW / 2,
                x2 = d.w - gap,
                y2 = gap + it * lineW + lineW / 2,
            )
        } else {
            Line.of(
                x2 = gap,
                y2 = gap + it * lineW + lineW / 2,
                x1 = d.w - gap,
                y1 = gap + it * lineW + lineW / 2,
            )
        }

        c.drawLine(line, paint().apply {
            this.strokeWidth = lineW + 16
            this.shader = Shader.makeLinearGradient(
                x0 = line.x1, y0 = line.y1,
                x1 = line.x2, y1 = line.y2,
                colors = intArrayOf(
                    RetroColors.white01,
                    RetroColors.black01,
                    RetroColors.red01,
                    RetroColors.white01,
                ),
                positions = floatArrayOf(0f, 0.2f,  0.25f + it * 0.06f, 1f)
            )
            this.pathEffect = PathEffect.makeDash(
                intervals = floatArrayOf(80f, 1f),
                phase = it * 0.1f
            )
            this.pathEffect = PathEffect.makeDiscrete(100f, 10f, 0)
            this.strokeCap = PaintStrokeCap.BUTT
        })
    }

}
