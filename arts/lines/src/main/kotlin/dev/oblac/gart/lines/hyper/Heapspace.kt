package dev.oblac.gart.lines.hyper

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.Palette
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndf
import dev.oblac.gart.util.forSequence
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.PathEffect

fun main() {
    val gart = Gart.of("heapspace", 1024, 1024)
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

private fun draw(c: Canvas, d: Dimension) {
    forSequence(40, 10, -1).forEachIndexed { index, n ->
        drawN(c, d, 6, 500f - n * 40f, index)
    }
}

//private val pal = Palettes.cool35
//private val pal = Palettes.cool1
private val pal = Palette.of(0xFF4000, 0xFFFFFF).expand(20)

private fun drawN(c: Canvas, d: Dimension, n: Int, r: Float, index: Int) {
    val angle = rndf(0, 36f) * 10f
    c.save()
    val center = d.center
    c.rotate(angle, center.x, center.y)
    val points = createNtagonPoints(n, center.x, center.y, r, 0f)
    val path = points.toClosedPath()
    c.drawPath(path, fillOf(pal.safe(index)).apply {
        this.strokeJoin = PaintStrokeJoin.ROUND
        this.strokeCap = PaintStrokeCap.ROUND
        this.alpha = 150
    })
    c.drawPath(path, strokeOf(1f + index / 3, NipponColors.col233_SHIRONERI).alpha(100).apply {
        this.pathEffect = PathEffect.makeCorner(10f)
    })

    val connectingPoints = points.filterIndexed { index, _ -> index % 2 == 0 }.toList()
    connectingPoints.forEach {
        val line = Line(center, it)
        c.drawLine(line, strokeOf(1f + index/ 3, NipponColors.col233_SHIRONERI).alpha(200).apply {
            this.strokeCap = PaintStrokeCap.ROUND
        })
    }
    c.restore()

}
