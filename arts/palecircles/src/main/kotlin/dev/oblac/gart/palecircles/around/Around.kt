package dev.oblac.gart.palecircles.around

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.MidCenturyColors
import dev.oblac.gart.color.alpha
import dev.oblac.gart.gfx.Circle
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.f
import dev.oblac.gart.math.rndf
import dev.oblac.gart.shader.createNoiseGrainFilter
import dev.oblac.gart.smooth.catmullRomSpline
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("around", 1280, 1280)
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
    c.clear(MidCenturyColors.black)

    val n = 10
    val w = d.w / n
    val circles = Array(n) {
        val x = it * w.f() + w / 2f
        val up = it % 2 == 0
        val min = d.h / 8f
        val y = if (up)
            rndf(min, d.h / 2f - min)
        else
            rndf(d.h / 2f + min, d.h.f() - min)
        Circle(x, y, w * 0.4f)
    }
    val points = d.leftBottom.offset(-20f, 0f).asList() +
        circles.flatMapIndexed { index, circle ->
            val up = index % 2 == 0
            if (up) {
                listOf(
                    circle.leftPoint.offset(-40f, -10f),
                    circle.rightPoint.offset(40f, -10f)
                )
            } else {
                listOf(
                    circle.leftPoint.offset(-40f, 10f),
                    circle.rightPoint.offset(40f, 10f)
                )
            }
        } +
        d.rightTop.offset(10f, 0f).asList()

    val path = catmullRomSpline(points)

    val p = PathBuilder()
    p.moveTo(d.leftBottom.offset(-10f, 10f))
    p.addPath(path)
    p.lineTo(d.rightBottom.offset(10f, 10f))
    p.closePath()

    val pd = p.detach()
    c.drawPath(pd, fillOf(MidCenturyColors.red).apply {
        this.imageFilter = createNoiseGrainFilter(0.2f, d)
    })

    repeat(16) {
        val pd2 = PathBuilder(path).offset(0f, it * 10f).detach()
        c.drawPath(pd2, strokeOf(10f, MidCenturyColors.red).apply {
            this.imageFilter = ImageFilter.makeDropShadow(0f, 0f, 10f, 10f, MidCenturyColors.black)
        })
    }

    // white line
    val pd2 = PathBuilder(path).offset(20f, 20f).detach()
    c.drawPath(pd2, strokeOf(8f, MidCenturyColors.white1.alpha(160)))

    // final circles
    circles.forEachIndexed { index, it ->
        if (index % 2 == 0) return@forEachIndexed
        c.drawCircle(it,
            fillOf(MidCenturyColors.white1).apply {
                this.imageFilter = ImageFilter.makeDropShadow(8f, 8f, 8f, 8f, MidCenturyColors.white1)
            }
        )
    }
}

private fun Point.asList() = listOf(this)
