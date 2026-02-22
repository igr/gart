package dev.oblac.gart.palecircles.shad

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.alpha
import dev.oblac.gart.gfx.center
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.math.doubleLoop
import dev.oblac.gart.math.rndi
import dev.oblac.gart.shader.createNoiseGrainFilter
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

fun main() {
    val gart = Gart.of("shad", 1280, 1280)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    val draw = ShadDraw3(g)

    // save image
    //g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class ShadDraw3(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)

    val gap = d.w / 8
    val n = 8
    val w = (d.width - 2 * gap) / n
    val fill1 = fillOf(RetroColors.white01).apply {
        this.imageFilter = createNoiseGrainFilter(0.2f, d)
    }
    val fill2 = fillOf(RetroColors.red01)
    doubleLoop(n, n) { (i, j) ->
        val x = gap + i * w
        val y = gap + j * w
        val r = Rect.makeXYWH(x, y, w, w)
        val fill = if (i == 5) fill2 else fill1
        repeat(2) {
            val delta = (w/2) * it
            val yy = y + delta
            c.save()
            c.clipRect(Rect.makeXYWH(x, yy, w / 2, w))
            c.drawCircle(r.center().offset(0f, delta), w / 2, fill.alpha(rndAlpha()))
            c.restore()
            c.save()
            c.clipRect(Rect.makeXYWH(x + w / 2, yy, w / 2, w))
            c.drawCircle(r.center().offset(0f, delta), w / 2, fill.alpha(rndAlpha()))
            c.restore()
        }
    }
}

private fun rndAlpha() = rndi(5, 15) * 10
