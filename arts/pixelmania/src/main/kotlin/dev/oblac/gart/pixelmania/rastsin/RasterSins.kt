package dev.oblac.gart.pixelmania.rastsin

import dev.oblac.gart.*
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.math.f
import dev.oblac.gart.pixels.dither.ditherOrdered4By4Bayer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Shader
import kotlin.math.sin

fun main() {
    val gart = Gart.of("rastersin", 1024 * GOLDEN_RATIO, 1024)
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
        ditherOrdered4By4Bayer(b, 4, 12)
        b.drawToCanvas()
        c.draw(g)
    }
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.white01)

    val skew = 100f
    val total = 4
    repeat(total) {
        c.drawCircle(
            x = 200f + it * 200f - skew,
            y = 220f + it * 220f,
            radius = 100f + it * 20f,
            fillOf(RetroColors.white01)
        )


        // draw sine wave lines
        val baseY = 200f + 200f * it
        for (x in 0 until d.w + skew.toInt()) {
            val y = baseY + 80f * sin(x * (0.005 + it * 0.0002) + it * 1.8f)
            val lineToDraw = Line.of(
                x.f() - skew, d.hf,
                x.f(), y.f()
            )
            val lineToShade = if (it == total -1) {
                Line.of(
                    lineToDraw.a.x, d.hf,
                    lineToDraw.b.x, lineToDraw.b.y
                )
            } else {
                Line.of(
                    lineToDraw.a.x, y.f() + 260f,
                    lineToDraw.b.x, lineToDraw.b.y
                )
            }
            c.drawLine(
                lineToDraw,
                paint().apply {
                    this.strokeWidth = 2f
                    this.shader = Shader.makeLinearGradient(
                        x0 = lineToShade.x1, y0 = lineToShade.y1,
                        x1 = lineToShade.x2, y1 = lineToShade.y2,
                        colors = arrayOf(
                            RetroColors.black01,
                            RetroColors.red01,
                            RetroColors.white01,
                        ).toIntArray(),
                        positions = floatArrayOf(
                            0f, 0.82f, 1f
                        )
                    )
                }
            )
        }
    }

}
