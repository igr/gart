package dev.oblac.gart.circledots.gentle

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.createWaveBetweenPoints
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.f
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndi
import dev.oblac.gart.smooth.toCardinalSpline
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("gentle", 1024, 1024)
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

/**
 * This version draws static image.
 */
private class MyDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}


private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)

    repeat(20) { lines ->
        val s = 80f
        val rp = createWaveBetweenPoints(
            d.leftBottom.offset(0f, lines * s),
            d.rightTop.offset(0f, 40f + lines * s),
            steps = rndi(6, 18),
            maxRadius = rndf(80f, 120f),
        )
        repeat(s.toInt()) { ndx ->
            fun isSpecialPoint(i: Int) = i in 10..14
            val color = if (isSpecialPoint(ndx)) RetroColors.red01 else RetroColors.white01
            val alpha = if (isSpecialPoint(ndx)) 255 else rndi(50, 155)
            val spline = rp.mapIndexed { i, p -> p.offset(ndx.f() * (1f + i.mod(2)), ndx.f() * (1f + i.mod(2))) }.toCardinalSpline(0.1f, 30)
            c.drawPath(spline, strokeOf(3f, color).apply {
                this.alpha = alpha
            })
        }
    }

//    rp.shuffled().toSmoothQuadraticPath().apply {
//        c.drawPath(this, strokeOf(20f, RetroColors.red01))
//    }

//    c.drawCircle(d.cx, d.cy, 300f, fillOf(RetroColors.red01).apply {
//        this.pathEffect = PathEffect.makeDiscrete(20f, 8f, 123)
//    })

//    rp.shuffled().toBSpline(30).apply {
//        c.drawPath(this, strokeOf(15f, RetroColors.amber01))
//    }
}
