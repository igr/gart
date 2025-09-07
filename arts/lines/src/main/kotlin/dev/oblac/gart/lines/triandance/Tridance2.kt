package dev.oblac.gart.lines.triandance

import dev.oblac.gart.*
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathEffect
import kotlin.math.sin

fun main() {
    val gart = Gart.of("triadance2", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw2(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

/**
 * Hot reload requires a real class to be created, not a lambda.
 */
private class MyDraw2(val g: Gartvas) : Drawing(g) {
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
private val palettes = Palettes.cool20.expand(total + 1)

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

            drawTriangleEchos(c, t, color)
            c.drawTriangle(t, fillOf(color).apply {
                alpha = 200
            })
    }

    drawStripes(c, d)
}

private fun drawTriangleEchos(c: Canvas, t: Triangle, color: Int) {
    val iterations = 8
    var currentTriangle = t
    
    repeat(iterations) { i ->
        // Draw current triangle
        c.drawTriangle(currentTriangle, fillOf(color).apply {
            alpha = (205 - i * 25).coerceAtLeast(50)
            pathEffect = PathEffect.makeDiscrete(20f, 10f, 20)
        })
        
        // Calculate scale factor that grows with each iteration
        val scaleFactor = 1 + (i * 0.05f) // Scale gets smaller each iteration
        currentTriangle = currentTriangle.scaled(scaleFactor)
    }
}

private fun drawStripes(c: Canvas, d: Dimension) {
    val stripes = 80
    val step = 1f / stripes
    (0..stripes).forEach { i ->
        val f = i * step
        val line = Line(
            Point(f * d.w, f * d.h + sin(i/16f) * 150f + 100f),
            Point(f * d.w, d.h)
        )
        c.drawLine(line, strokeOf(RetroColors.black01, 2f).apply {
            this.pathEffect = PathEffect.makeDash(floatArrayOf(100f, 10f), rndf(0f, 100f))
        })
    }
}
