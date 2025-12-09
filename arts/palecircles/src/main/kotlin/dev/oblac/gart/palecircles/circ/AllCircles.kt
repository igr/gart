package dev.oblac.gart.palecircles.circ

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("all-circles", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    val draw = MyDraw3(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

/**
 * This version draws static image.
 */
private class MyDraw3(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.white01)

    gridOfDimension(d, 10, 10).forEach {
        val r = it.rect
        val cx = r.center().x
        val cy = r.center().y

        val i = it.row
        val j = it.col
        val radius = (4 * i + 4 * j + 14f) * 0.9f

        c.save()
        c.clipRect(r)
        val circle = Circle(cx, cy, radius)
        c.drawCircle(circle, fillOf(RetroColors.black01))

        c.save()
        c.clipCircle(circle)
        c.drawCircle(620f, 620f, 284f, fillOf(RetroColors.red01))
        c.restore()

        c.restore()
    }

    c.drawBorder(d, 40f, RetroColors.white01)
}
