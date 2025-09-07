package dev.oblac.gart.lines.swing2

import dev.oblac.gart.*
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.gravitron.Gravitron
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path

fun main() {
    val gart = Gart.of("swing2", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw(g)

//    g.draw(draw)
//    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDraw(g: Gartvas) : Drawing(g) {
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        draw2(c, d)
    }
}

private val g1 = Gravitron(
    x = 760f,
    y = 400f,
    radius = 200f,
    angle = Degrees.of(180f)
)
private val g1Prim = Gravitron(
    x = 760f,
    y = 400f,
    radius = 200f,
    angle = Degrees.of(197f)
)
private val g2 = Gravitron(
    x = 300f,
    y = 600f,
    radius = 200f,
    angle = Degrees.of(160f)
)

private fun draw2(c: Canvas, d: Dimension) {
    c.clear(Colors.black)
    c.drawCircle(g1.circle, strokeOfGreen(2f))
    c.drawCircle(g2.circle, strokeOfGreen(2f))

    c.drawCircle(750f, 810f, 130f, fillOf(RetroColors.white01))

    repeat(15) {
        val l = Line(
            a = Point(0f, 0f + 20 * it),
            b = Point(d.wf, 100f + 20 * it)
        )
        drawSwingLine(c, l, RetroColors.white01, it)
        if (it == 1) {
            c.drawCircle(320f, 90f, 80f, fillOf(RetroColors.red01))
        }
    }

    repeat(8) {
        val l = Line(
            a = Point(0f, 300f + 20 * it),
            b = Point(d.wf, 100f + 20 * it)
        )
        drawSwingLine2(c, l, RetroColors.red01, it, 2)
    }
}

private fun drawSwingLine(c: Canvas, l: Line, color: Int, i: Int, delta: Int = 0) {
    val p = Path()
    p.moveTo(l.a.x, l.a.y)

    val l2 = g1.applyTo(l, p)
    if (l2 != null) {
        val l3 = g2.applyTo(l2, p)
        if (l3 != null) {
            // draw the last line
            p.lineTo(l3.b.x, l3.b.y)
        } else {
            // if the last line is null, it means we are outside the radius
            p.lineTo(l2.b.x, l2.b.y)
        }
    }
    c.drawPath(p, strokeOf(color, 10f + delta))
}
private fun drawSwingLine2(c: Canvas, l: Line, color: Int, i: Int, delta: Int = 0) {
    val p = Path()
    p.moveTo(l.a.x, l.a.y)

    val l2 = g1Prim.applyTo(l, p)
    if (l2 != null) {
        val l3 = g2.applyTo(l2, p)
        if (l3 != null) {
            // draw the last line
            p.lineTo(l3.b.x, l3.b.y)
        } else {
            // if the last line is null, it means we are outside the radius
            p.lineTo(l2.b.x, l2.b.y)
        }
    }
    c.drawPath(p, strokeOf(color, 10f + delta))
}
