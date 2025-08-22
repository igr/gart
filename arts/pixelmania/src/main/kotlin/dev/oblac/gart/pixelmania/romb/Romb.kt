package dev.oblac.gart.pixelmania.romb

import dev.oblac.gart.*
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import kotlin.math.min

fun main() {
    val gart = Gart.of("romb", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw(g)

    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDraw(val g: Gartvas) : Drawing(g) {
    val b = Gartmap(g)
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        draw(g.canvas, d)
//        b.updatePixelsFromCanvas()
//        ditherOrdered4By4Bayer(b, 4, 12)
//        b.drawToCanvas()
        c.draw(g)
    }
}


private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.white01)

    val rows = 8
    val cols = 8

    val size = 38f
    val rowStart = 40f
    val colStart = 40f

    var y = rowStart
    for (row in 0 until rows) {
        val height = (row + 2) * size
        var x = colStart
        for (col in 0 until cols) {
            val width = (col + 2) * size

            drawSinglePoly(x, y, width, height, c)

            x += width + size / 2
        }
        y += height + size / 2
    }
}

private fun drawSinglePoly(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    c: Canvas
) {
//    val poly1 = createRectanglePoly(Point(x-18, y-8), width, height)
//    c.drawPoly4(poly1, fillOf(RetroColors.red01))
//    val poly2 = createRectanglePoly(Point(x+1, y+1), width, height)
//    c.drawPoly4(poly2, fillOf(RetroColors.pink01))

    val point = Point(x, y)
    val poly = createRectanglePoly(point, width, height)
    c.drawPoly4(poly, fillOf(RetroColors.black01))
    c.save()
    c.clipPath(poly.path)
    val radius = width.coerceAtMost(height) / 2f
    c.drawCircle(point.offset(-width/2 + 10f, 0f), radius, fillOf(RetroColors.red01))
    c.drawCircle(point.offset(-width/2 + 10f, 0f), min(50f, 20f + (radius - 20f)/2), fillOf(RetroColors.orange01))
    c.drawCircle(point.offset(-width/2 + 10f, 0f), 20f, fillOf(RetroColors.yellow01))
    c.restore()
}


private fun createRectanglePoly(center: Point, width: Float, height: Float): Poly4 {
    val w2 = width / 2f
    val h2 = height / 2f

    val x1 = center.x
    val y1 = center.y - h2

    val x2 = center.x + w2
    val y2 = center.y

    val x3 = center.x
    val y3 = center.y + h2

    val x4 = center.x - w2
    val y4 = center.y

    return Poly4(
        Point(x1, y1),
        Point(x2, y2),
        Point(x3, y3),
        Point(x4, y4)
    )
}
