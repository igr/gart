package dev.oblac.gart.rectapart3

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.angle.cosf
import dev.oblac.gart.angle.sinf
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.Line
import dev.oblac.gart.gfx.ofCenter
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.gfx.toPoints
import dev.oblac.gart.math.rndb
import dev.oblac.gart.rectapart2.drawRotatedRect
import org.jetbrains.skia.*
import kotlin.math.sqrt

fun main() {
    val gart = Gart.of("rectapart3", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    val draw = MyDraw3(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw)
//    w.show { canvas, dimension, _ ->
//        draw(canvas, dimension)
//    }.onKey {
//        when (it) {
//            Key.KEY_Q -> {
//                rectWidth += 5f
//            }
//            Key.KEY_A -> {
//                rectWidth -= 5f
//            }
//            Key.KEY_W -> {
//                rectHeight += 5f
//            }
//            Key.KEY_S -> {
//                rectHeight -= 5f
//            }
//            else -> {
//            }
//        }
//        println("$rectWidth x $rectHeight")
//    }
    //w.show(draw).hotReload(g)
}

private var rectWidth = 155f
private var rectHeight = 15f

private class MyDraw3(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}


//private val pal = Palettes.cool19
private val pal = Palettes.cool47
//private val pal = Palettes.colormap056

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)

    val n = 180 // Number of radial lines
    val center = d.leftBottom.offset(-240f, 140f)

    val maxLength = sqrt((d.w * d.w + d.h * d.h).toFloat()) * 1.2f

    val linePaint = strokeOf(1f, RetroColors.white01)
    val rectPaint = strokeOf(2f, RetroColors.white01)

    val m = 20 // Number of rectangles per line

    // Draw n radial lines
    repeat(n) {
        val angle = Degrees.of(360 * it / n)
        val endX = center.x + maxLength * cosf(angle)
        val endY = center.y - maxLength * sinf(angle) // Subtract because Y increases downward

        // Line
        val line = Line(center, Point(endX, endY))
        //c.drawLine(line.shortenByLen(60f + 2f * sin(angle * 8).f()), linePaint)

        // Draw rotated rectangles at each point
        val points = line.toPath().toPoints(m).slice(3 until m)
        val lineAngle = line.angle()
        points.forEachIndexed { index, point ->
            val rectPaint = strokeOf(2f, pal.safe(it + index))
//            c.drawRotatedRect(point, rectWidth, rectHeight, lineAngle, rectPaint)
            if (rndb(1, 20)) {
                c.drawRotatedRect(point, rectWidth, rectHeight, lineAngle, rectPaint.also {
                    it.mode = PaintMode.STROKE_AND_FILL
                })
            } else {
                c.drawRotatedRect(point, rectWidth, rectHeight, lineAngle, rectPaint)
            }
        }
    }
//    c.drawCircle(d.w3x2, d.h3, 180f, fillOf(RetroColors.black01))
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
