package dev.oblac.gart.pixelmania.liqf

import dev.oblac.gart.*
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.dist
import dev.oblac.gart.pixels.liquify
import dev.oblac.gart.util.sequenceLoop
import org.jetbrains.skia.Canvas
import kotlin.math.min

fun main() {
    val gart = Gart.of("liqf", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDraw(val g: Gartvas) : Drawing(g) {
    val b = Gartmap(g)
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        draw(g.canvas, d)
        b.updatePixelsFromCanvas()
        liquify(b, Circle(Point(d.w3x2, d.h3x2), 500f), -7f)
        liquify(b, Circle(Point(200f, d.h3), 300f), 20f)
        b.drawToCanvas()
        c.draw(g)
    }
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.white01)

    val center = d.center

    val numSquares = 72
    val maxSize = 1024 * 1.4f
    val inc = 20f

    sequenceLoop(1, numSquares)
        .map { i ->
            val reverseIndex = numSquares - 1 - i
            val size = maxSize - reverseIndex * inc

            val angle = Degrees.of(20 + reverseIndex)

            Poly4.squareAroundPoint(center, size, angle)
        }
        .toList().reversed()
        .filterIndexed { index, _ -> index % 3 == 0 }
        .forEachIndexed { index, it ->
            if (index % 3 == 0) {
                c.drawPoly4(it, fillOf(RetroColors.white01))
            } else {
                c.drawPoly4(it, fillOf(RetroColors.black01))
//                c.drawPoly4(it, strokeOf(RetroColors.red01, 4f).apply {
//                    this.pathEffect = PathEffect.makeDiscrete(1.2f, 4f, 10)
//                })
                val point = it.center()
                val poly = it
                val width = dist(poly.a, poly.c)
                val height = dist(poly.b, poly.d)
                c.save()
                c.clipPath(poly.path)
                val radius = width.coerceAtMost(height) / 1.7f
                c.drawCircle(point.offset(-width / 2 + 20f, 0f), radius, fillOf(RetroColors.red01))
                c.drawCircle(point.offset(-width / 2 + 10f, 0f), min(50f, 30f + (radius - 30f) / 2), fillOf(RetroColors.orange01))
                c.drawCircle(point.offset(-width / 2 + 10f, 0f), 30f, fillOf(RetroColors.yellow01))
                c.restore()

            }
        }

}
