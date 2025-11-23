package dev.oblac.gart.rectapart2

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.*
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.f
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.sqrt

fun main() {
    val gart = Gart.of("rectpack1", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    val draw = MyDraw3(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDraw3(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}


private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)

    val n = 180 // Number of radial lines
    val center = d.leftBottom.offset(-80f, 80f)

    val maxLength = sqrt((d.w * d.w + d.h * d.h).toFloat()) * 1.2f

    val linePaint = strokeOf(1f, RetroColors.white01)
    val rectPaint = strokeOf(2f, RetroColors.white01)

    val m = 25 // Number of rectangles per line
    val rectWidth = 30f
    val rectHeight = 30f

    // Draw n radial lines
    repeat(n) {
        val angle = Degrees.of(360 * it / n)
        val endX = center.x + maxLength * cosf(angle)
        val endY = center.y - maxLength * sinf(angle) // Subtract because Y increases downward

        // Line
        val line = Line(center, Point(endX, endY))
        c.drawLine(line.shortenByLen(60f + 2f * sin(angle * 8).f()), linePaint)

        // Draw rotated rectangles at each point
        val points = line.toPath().toPoints(m).slice(3 until m)
        val lineAngle = line.angle()
        points.forEach { point ->
            c.drawRotatedRect(point, rectWidth, rectHeight, lineAngle, rectPaint)
            c.drawRotatedRect(point, rectWidth + 4, rectHeight, lineAngle, rectPaint)
        }
    }
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
fun Canvas.drawRotatedRect(center: Point, width: Float, height: Float, angle: Angle, paint: Paint) {
    this.save()
    this.rotate(angle.degrees, center.x, center.y)
    val rect = Rect.ofCenter(center, width, height)
    this.drawRect(rect, paint)
    this.restore()
}
