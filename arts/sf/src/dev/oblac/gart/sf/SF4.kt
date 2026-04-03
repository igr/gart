package dev.oblac.gart.sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.doubleLoop
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("sf4", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)

    gart.saveImage(g)

    w.showImage(g)
}

private val colorBack = RetroColors.black01
private val colorInk = RetroColors.white01
private val colorBold = RetroColors.red01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)

    val r1 = 160f
    val r2 = d.cx - 80f

    val circle1 = Circle.of(d.center, r1)
    val circle2 = Circle.of(d.center, r2)

    val rin1 = 240f
    val circleIn1 = Circle.of(d.center.offset((rin1 - r1), 0f), rin1)
    var rin2 = rin1 + (r2 - r1) / 2
    val circleIn2 = Circle.of(d.center.offset(-50f, 0f), rin2)

//    c.drawCircle(circle1, strokeOfGreen(2f))
//    c.drawCircle(circle2, strokeOfGreen(2f))
//    c.drawCircle(circleIn1, strokeOfYellow(2f))
//    c.drawCircle(circleIn2, strokeOfRed(2f))

    c.drawCircle(d.center.offset(0f, 0f), 40f, fillOf(colorBold))

    circle2.points(100).forEach {
        Line(it, d.center).toPath().toPoints(120)
            .filter { p -> !circle1.contains(p) }
            .forEach { p ->
                val strokeSize = if (circleIn1.contains(p)) {
                    2.4f
                } else if (circleIn2.contains(p)) {
                    4.4f
                } else {
                    1.5f
                }
                c.drawPoint(p, strokeOf(colorInk, strokeSize))
            }
    }

    val step = 10f
    doubleLoop(Pair(0f, 0f), d.wf, d.hf, step = Pair(step, step)) { (x, y) ->
        val p = Point(x, y)
//        if (!circle2.contains(p) || circle1.contains(p)) {
//            c.drawCircle(p, 1f, fillOf(colorBold))
//        }
//        if (circle1.contains(p)) {
//            c.drawCircle(p, 1f, fillOf(colorBold))
//        }
    }

}

