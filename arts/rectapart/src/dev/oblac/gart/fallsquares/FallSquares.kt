package dev.oblac.gart.fallsquares

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

fun main() {
    val gart = Gart.of("fall-squares", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    val draw = MyDraw3(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw)
    //w.show(draw).hotReload(g)
}

private class MyDraw3(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

private val pal = Palette.of(RetroColors.black01, RetroColors.white01)
private val square = 120f   //square width/height

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)

    Triangle(d.leftTop, d.rightTop, d.leftBottom).also {
        c.drawTriangle(it, fillOf(RetroColors.white01))
    }

    c.save()
    c.rotate(-45f, d.cx, d.cy)

    // middle line
    val line0 = Line(d.leftMiddle, d.rightMiddle.offset(100f, 0f)).shortenByLen(-200f)
    line0.rectsOnLine(square, square, Degrees(0f), square).forEachIndexed { i, rect ->
        val color = pal.safe(i)
        c.drawPoly4(rect, fillOf(color))
    }

    repeat(10) {
        val dy = square * it - (it * it * 5)
        val lineR = Line(line0.a.offset(0f, dy), line0.b.offset(0f, dy))
        lineR.rectsOnLine(square, square, Degrees(it * 7f), square).forEachIndexed { i, rect ->
            if ((it + i) % 2 == 0) return@forEachIndexed
            val color = pal.safe(i + it)
            c.drawPoly4(rect, fillOf(color))
        }
    }
    c.drawCircle(Circle(300f, 400f, 100f), fillOf(RetroColors.red01))
    repeat(12) {
        if (it < 1) return@repeat
        val dy = -square * it + (it * it * 5)
        val lineR = Line(line0.a.offset(0f, dy), line0.b.offset(0f, dy))
        lineR.rectsOnLine(square, square, Degrees(it * 7f), square).forEachIndexed { i, rect ->
            if ((it + i) % 2 == 1) return@forEachIndexed
            val color = pal.safe(i + it)
            c.drawPoly4(rect, fillOf(color))
        }
    }

    c.restore()

}

/**
 * Draws a rectangle centered at the given point, rotated by the specified angle.
 *
 * @param center The center point of the rectangle
 * @param width The width of the rectangle
 * @param height The height of the rectangle
 * @param angle The rotation angle around the center point
 * @param paint The paint to use for drawing
 */
private fun Canvas.drawRotatedRect(center: Point, width: Float, height: Float, angle: Angle, paint: Paint) {
    this.save()
    this.rotate(angle.degrees, center.x, center.y)
    val rect = Rect.ofCenter(center, width, height)
    this.drawRect(rect, paint)
    this.restore()
}

/**
 * Returns rectangles centered on points along the line, spaced by [distance].
 * Each rectangle has given [rectWidth] x [rectHeight] and is rotated by [angle].
 */
private fun Line.rectsOnLine(rectWidth: Float, rectHeight: Float, angle: Angle, distance: Float): List<Poly4> {
    val lineLen = length()
    val count = (lineLen / distance).toInt() + 1
    return (0 until count).map { i ->
        val center = pointFromStartLen(i * distance)
        Poly4.rectAroundPoint(center, rectWidth, rectHeight, angle)
    }
}

