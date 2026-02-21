package dev.oblac.gart.lines.ngons

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.gfx.createNtagonPoints
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.gfx.toClosedPath
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin

fun main() {
    val gart = Gart.of("ngons", 1024, 1024)
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

private class MyDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
        print("!")
    }
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(NipponColors.col248_SUMI)
    c.save()
    c.rotate(-90f, 512f, 512f)
    c.drawCircle(116 + 512f, 512f, 204f, fillOf(NipponColors.col037_SYOJYOHI))
    ntgon(c, d)
    c.restore()
}

private fun ntgon(c: Canvas, d: Dimension) {
    sequenceOf(5, 49, 80, 106, 126, 142, 156, /*168*/).forEachIndexed { index, it ->
        it.ngon(c, index + 3)
    }
}

private fun Int.ngon(c: Canvas, sides: Int) {
    createNtagonPoints(sides, 128 + 512f - this, 512f, 200f + this, 0f).also {
        c.drawPath(it.toClosedPath(), strokeOf(8f, NipponColors.col233_SHIRONERI).apply {
            this.strokeJoin = PaintStrokeJoin.ROUND
            this.strokeCap = PaintStrokeCap.ROUND
        })
    }
}
