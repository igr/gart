package dev.oblac.gart.pixelmania.foo

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.shader.createNoiseGrain2Filter
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Shader.Companion.makeSweepGradient

fun main() {
    val gart = Gart.of("circl", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw(g)

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
    c.clear(CssColors.white)

    val centerPoint = d.center

    // Configuration
    val numberOfPoints = 10
    val orbitRadius = 150f
    val circleRadius = 840f

    // Calculate N points around the center
    val points = createCircleOfPoints(centerPoint, orbitRadius, numberOfPoints)

    // Draw a guide circle showing the orbit
    c.drawCircle(centerPoint.x, centerPoint.y, orbitRadius, strokeOf(CssColors.lightGray, 1f))

    // Draw circles at each calculated point
    points.forEachIndexed { index, point ->
        val color = CssColors.black
        val circle = Circle(point, circleRadius * (1f - index * 0.1f))
        c.save()
        c.clipCircle(circle)
        c.saveLayer(createNoiseGrain2Filter(0.4f, d))
        c.drawCircle(circle, fillOf(color).apply {
            this.alpha = 200
            this.shader = makeSweepGradient(
                center = point.offset(100f, 100f),
                colors = intArrayOf(color, CssColors.white, color),
                positions = floatArrayOf(0.4f, 0.7f, 1f)
            )
        })
        c.restore()
        c.restore()
    }
}


