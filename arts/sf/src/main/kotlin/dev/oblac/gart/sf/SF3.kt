package dev.oblac.gart.sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

fun main() {
    val gart = Gart.of("sf3", 1024, 1024)
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

    val shipPoint = Point(d.cx, d.h3 / 2)
    val ignitionPoint = Point(d.cx, d.h * 5 / 6)

    for (i in 0..5) {
        drawOvalSmoke(c, d, ignitionPoint, i)
    }

    c.drawCircle(ignitionPoint, 20f, fillOf(colorBack))
    drawImpactRay(c, shipPoint, ignitionPoint)

    val len = shipPoint.distanceTo(ignitionPoint)

    repeat(4) {
        val x = rndf(50f, d.w.toFloat() - 50f)
        val y = rndf(50f, d.h3 + 50f)

        val start = Point(x, y - len)
        val end = Point(x, y)
        drawImpactRay(c, start, end)
    }

    c.drawRoundBorder(d, 10f, 40f, colorInk)
}

private fun drawImpactRay(
    c: Canvas,
    startPoint: Point,
    targetPoint: Point
) {
    val ps = Line(targetPoint, startPoint.offset(60f, 0f)).toPath().toPoints(400)
    ps.forEachIndexed { i, p ->
        c.drawCircle(p.offset(rndf(-2f, 2f), rndf(-2f, 2f)), 8f - 1f * i / 50f, fillOf(colorBold))
    }
}

private fun drawOvalSmoke(c: Canvas, d: Dimension, ip: Point, index: Int) {
    c.save()

    val pc = ip.offset(0f, -index * 20f)
    val rectW = 210f * index
    val rect = Rect.ofCenter(pc, rectW, rectW / GOLDEN_RATIO.toFloat())
    c.rotate(rndf(0f, 5f) * if (index % 2 == 0) 1 else -1, pc.x, pc.y)

    c.drawOval(rect, fillOf(colorInk).apply {
        alpha = 10 + index * 22
    })
    //c.drawOval(rect, strokeOf(colorBold, 2f))

    c.restore()
}
