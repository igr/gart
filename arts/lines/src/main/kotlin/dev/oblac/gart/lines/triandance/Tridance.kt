package dev.oblac.gart.lines.triandance

import dev.oblac.gart.*
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("triadance", 1024, 1024)
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
    init {
        draw(g.canvas, g.d)
    }
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        //draw(g.canvas, d)
        //b.updatePixelsFromCanvas()
        // draw pixels
        //b.drawToCanvas()
        c.draw(g)
    }
}

private const val total = 31
private val palettes = Palettes.cool9.expand(total + 1)

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)

    val triangle = Triangle.equilateral(d.center, 400f, Degrees.ZERO)
    val ts = mutableListOf(triangle)

    repeat(total) {
        val t = ts.last()
        val scaledT = t.scaled(0.91f)
        // flip around the random side
        t.edges.shuffle()

        val m = t.edges.map { side ->
            val newT = scaledT.flipAcross(side)
            // move a bit towards the center
            val toCenter = d.center - newT.centroid
            val moveFactor = 0.73f
            val move = toCenter * moveFactor
            val movedA = newT.a + move
            val movedB = newT.b + move
            val movedC = newT.c + move
            val movedT = Triangle(movedA, movedB, movedC)

            movedT.rotateAround(movedT.points().random(), Degrees.of(4f))
        }.minBy {
            it.isInRect(d.rect)
        }
        ts.add(m)
    }

    ts.forEachIndexed { i, t ->
        val color = palettes.safe(i)

//        if (i == 8 || i == 15 || i == 22 || i == 29) {
            val circle = t.calculateCircumcircle().scale(0.8f)
            c.drawCircle(circle, fillOf(color).apply {
                alpha = 80
            })
//        } else {
            c.drawTriangle(t, fillOf(color).apply {
                alpha = 200
            })
//        }

//        c.drawTriangle(t, strokeOf(color, 8f).apply {
//            this.pathEffect = PathEffect.makeDiscrete(2f, 6f, 0)
//            alpha = 200
//        })
    }
}
