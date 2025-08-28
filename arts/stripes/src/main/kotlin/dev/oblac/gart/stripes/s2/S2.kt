package dev.oblac.gart.stripes.s2

import dev.oblac.gart.*
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.map
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.Path
import kotlin.math.abs
import kotlin.math.pow

fun main() {
    val gart = Gart.of("s2", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

/**
 * Hot reload requires a real class to be created, not a lambda.
 */
private class MyDraw(val g: Gartvas) : Drawing(g) {
    val b = Gartmap(g)
    val image = g.also { draw(g.canvas, g.d) }.snapshot()
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        c.drawImage(image)
        //draw(g.canvas, d)
        //b.updatePixelsFromCanvas()
        // draw pixels
        //b.drawToCanvas()
        c.draw(g)
    }
}


private fun draw(c: Canvas, d: Dimension) {
    c.clear(Color.WHITE)
    val gap = 40f
    var y = 0f

//    val palette = Palettes.colormap058.expand(512) + Palettes.colormap058.reversed().expand(512)
//    val palette = Palettes.colormap054.expand(1024)
    val palette = Palettes.colormap058.expand(1024)
    while (y < d.hf) {
        val line = Line.of(0f, y, d.wf, y)
        val points = humanLinePoints(line, 10, 4f).toChaikinSmooth()

        drawStripe(c, y + 33f, gap, d.wf, points, palette)
        //c.drawPath(points, strokeOfBlack(1f))
        //c.drawLine(0f, y, d.wf, y, strokeOfBlack(1f))
        y += gap
    }
}

private fun drawStripe(c: Canvas, y: Float, gap: Float, wf: Float, points: Path, palette: Palette) {
    val stripeHeight = gap * 0.8f
    val ps = points.toPoints(wf.toInt() * 2)
    for (x in 0 until wf.toInt()) {
        val yy = ps[x].y
        val dy = (y + stripeHeight / 2) - (y + rndf(-stripeHeight / 2, stripeHeight / 2))
        val dist = abs(dy)
        val alpha = map(dist, 0f, stripeHeight / 2, 30, 255)
        val color = palette.safe(x + y.pow(1.45f).toInt())
//        val color = palette.safe(x + y.pow(3.1f).toInt())
        c.drawLine(x.toFloat(), y, x.toFloat(), yy - stripeHeight, strokeOf(color, 1f).apply { this.alpha  = alpha.toInt() })
    }
}
