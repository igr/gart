package dev.oblac.gart.sixsix.bw

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.doubleLoop
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.PathOp
import org.jetbrains.skia.RRect
import kotlin.math.sin

fun main() {
    val gart = Gart.of("sixsixBW", 1080, 1080)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    val draw = MyDraw(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

private val white = RetroColors.white01
private val black = RetroColors.black01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(white)

    val cellWidth = d.wf / 6f
    val cellHeight = d.hf / 6f

    doubleLoop(6, 6) { (row, col) ->
        val index = (row * 6 + col)
        val cellImage = draw(Dimension.of(cellWidth, cellHeight), index)
        val x = col * cellWidth
        val y = row * cellHeight
        c.drawImage(cellImage, x, y)
        //c.drawWhiteText("${index + 1}", x + 10f, y + 20f)
    }
}

private fun draw(d: Dimension, index: Int): Image {
    val gartvas = Gartvas(d)
    val c = gartvas.canvas

    val gap = 10f

    val rRect = RRect.makeXYWH(gap, gap, d.wf - 2 * gap, d.hf - 2 * gap, 2 * gap, 2 * gap)
    c.clipRRect(rRect)
    c.drawRect(d.rect, fillOf(black))


    val x = d.wf / 36 * index
    val k = sin(index * PIf / 36)
    val y = d.hf - k * d.hf / 1.2f

    val radius = 40f + d.cx * k * 0.3f
    val circle = Circle(Point(x, y), radius)

    c.drawCircle(circle, fillOf(white).apply {
        //this.pathEffect = PathEffect.makeDiscrete(10f, 2f, 0)
    })
    c.drawCircle(circle.resize(40f), fillOf(RetroColors.yellow01))
    c.drawCircle(circle.resize(20f), fillOf(RetroColors.orange01))

    combinePathsWithOp(PathOp.DIFFERENCE, d.rect.grow(50f).path(), circle.toPath())
        .apply {
            //c.drawPath(this, strokeOf(RetroColors.red01, 40f))
        }

    return gartvas.snapshot()
}
