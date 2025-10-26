package dev.oblac.gart.circledots.xy

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.Line
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.math.Matrix
import dev.oblac.gart.math.f
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("xy", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // Hot reload requires a real class to be created, not a lambda!

    val draw = MyDraw(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

/**
 * This version draws static image.
 */
private class MyDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

//private val pal = Palettes.colormap059.let { it + it.reversed() }
//private val pal = Palettes.colormap051.let { it + it.reversed() }
private val pal = Palettes.colormap045.let { it + it.reversed() }

private fun draw(c: Canvas, d: Dimension) {
    c.clear(BgColors.elegantDark)

    val n = 22      // number of circles per row/column
    val nc = n / 2  // center index
    val gap = 200   // gap from the edges

    val stepX = (d.w - gap * 2) / (n - 1)
    val stepY = (d.h - gap * 2) / (n - 1)

    // Collect circles in a matrix
    val matrix = Matrix(n, n) { i, j ->
        val x = gap + i * stepX
        val y = gap + j * stepY
        Point(x.f(), y.f())
    }

    // Iterate matrix and draw circles
    repeat(10) { layer ->
        matrix.forEach { i, j, point ->
            val distanceIndex = kotlin.math.max(kotlin.math.abs(i - nc), kotlin.math.abs(j - nc)) + 1f
            val line = Line(d.center, point).extendBy(layer * distanceIndex)
            val color = pal.safe(i + j * n)
            c.drawCircle(line.b, 20f, fillOf(color).apply {
                alpha = 60
            })
            //c.drawCircle(line.b, 10f, strokeOf(1f, RetroColors.black01))
        }
    }
}
