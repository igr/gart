package dev.oblac.gart.lines.swing2

import dev.oblac.gart.*
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.Line
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.gravitron.Gravitron
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.Rect

fun main() {
    val gart = Gart.of("swing2", 1024, 1024)
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
    y = 400f,
    radius = 200f,
    angle = Degrees.of(180f)
)
private val g2 = Gravitron(
    x = 300f,
    y = 600f,
    radius = 200f,
    angle = Degrees.of(210f)
)

private fun draw2(c: Canvas, d: Dimension) {
    c.clear(Colors.black)
//    c.drawCircle(g1.circle, strokeOfGreen(2f))
//    c.drawCircle(g2.circle, strokeOfGreen(2f))

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
        drawSwingLine(c, l, RetroColors.red01, it, 2)
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

private val rndfs = Array(40) { rndf(50f, 200f) }
private val lenghts = Array(40) { rndf(50f, 80f) }

private data class Swing(
    val y: Float,
    val right: Float,
    val gap: Float,
    val left: Float
)

private fun topLevelPath(d: Dimension, swing: Swing): Path {
    val path = Path()
    val y = swing.y
    path.moveTo(-201f, y)
    path.lineTo(swing.right, y)
    val middleY = y + swing.gap
    path.arcTo(Rect(swing.right, y, swing.right + swing.gap, middleY), 270f, 180f, false)
    path.lineTo(swing.left, middleY)
    val lastY = middleY + swing.gap
    path.arcTo(Rect(swing.left - swing.gap, middleY, swing.left, lastY), 270f, -180f, false)
    path.lineTo(d.wf + 201f, lastY)
    return path
}

