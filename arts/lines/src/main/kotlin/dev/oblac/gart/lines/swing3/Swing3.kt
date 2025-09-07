package dev.oblac.gart.lines.swing3

import dev.oblac.gart.*
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.gravitron.Gravitron
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path

fun main() {
    val gart = Gart.of("swing3", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw(g)

    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDraw(g: Gartvas) : Drawing(g) {
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        draw2(c, d)
    }
}

private val g1 = Gravitron(
    x = 760f,
    y = 280f,
    radius = 350f,
    angle = Degrees.of(170f)
)
private val g2 = Gravitron(
    x = 300f,
    y = 600f,
    radius = 440f,
    angle = Degrees.of(150f)
)
private val g3 = Gravitron(
    x = 660f,
    y = 760f,
    radius = 400f,
    angle = Degrees.of(70f)
)

private fun draw2(c: Canvas, d: Dimension) {
    c.clear(Colors.black)
    //c.drawCircle(g1.circle, strokeOfGreen(2f))
//    c.drawCircle(g2.circle, strokeOfGreen(2f))
//    c.drawCircle(g3.circle, strokeOfGreen(2f))

    repeat(46) {
        val l = Line(
            a = Point(0f, -340f + 8 * it),
            b = Point(d.wf, -10f + 8 * it)
        )
        drawSwingLine(c, d, l, it)
    }
}

private fun drawSwingLine(c: Canvas,  d: Dimension, l: Line, i: Int) {
    val p = Path()
    p.moveTo(l.a.x, l.a.y)

    var currentLine = l

    // Apply g1 transformation
    currentLine = g1.applyTo(currentLine, p) ?: run {
        //c.drawPath(p, strokeOf(color, 2f))
        return
    }

    // Apply g2 transformation
    currentLine = g2.applyTo(currentLine, p) ?: run {
        p.lineTo(currentLine.b.x, currentLine.b.y)
        //c.drawPath(p, strokeOf(color, 2f))
        return
    }

    // Draw line after g2
    //p.lineTo(currentLine.b.x, currentLine.b.y)

    // Apply g3 transformation and draw final line if successful
    val finalLine = g3.applyTo(currentLine, p)
    if (finalLine != null) {
        p.lineTo(finalLine.b.x, finalLine.b.y)
    }

    c.drawPointsAsCircles(p.toPoints(100).map {it.offset(0f,7f)}.subList(26, 51), strokeOf(RetroColors.white01, 10f))
    c.drawPointsAsCircles(p.toPoints(100).map {it.offset(0f,7f)}.subList(26, 51), fillOfWhite())
    c.drawPath(p, strokeOf(RetroColors.red01, 6f))
}

