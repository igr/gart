package dev.oblac.gart.circledots.awaking

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.smooth.toCardinalSpline
import dev.oblac.gart.util.pairs
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("awaking", 1024, 1024)
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

//private val pal = Palettes.cool56
private val pal = Palettes.cool37
//private val pal = Palettes.cool47

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)

    val off = 20f

    createNtagonPoints(6, d.cx, d.cy, 800f, 15f)
        .toClosedPath()
        .toPoints(200)
        .map { Line(d.center, it).toPath() }
//        .map { it.toPoints(60, EaseFn.BounceOut) }
        .map { it.toPoints(80, EaseFn.CubicInOut) }
        .forEach { line ->
            val line2 = line.map { it.offset(randomPoint(Point(-off, -off), Point(off, off))) }
            val spline = line2.toCardinalSpline(0.1f, 50).toPoints(500).pairs().map { Line(it.first, it.second) }

            //c.drawPath(spline, strokeOf(1f, RetroColors.white01))
            spline.forEachIndexed { ndx, it ->
                c.drawLine(it, strokeOf(2f - (ndx/200), pal.safe(ndx/16)).apply {
                    alpha = if (ndx > 400) 255-(ndx/3) else 255
                })
                if (ndx in 100..200) {
                    c.drawLine(it, strokeOf(10f, pal.safe(ndx/16)).apply {
                        alpha = 80
                    })
                }
            }
        }
}
