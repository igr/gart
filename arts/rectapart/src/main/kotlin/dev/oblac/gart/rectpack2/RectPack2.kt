package dev.oblac.gart.rectpack2

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.angle.cosf
import dev.oblac.gart.angle.sinf
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.f
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.abs
import kotlin.math.sqrt

fun main() {
    val gart = Gart.of("rectpack2", 1024, 1024)
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

    val n = 44 // Number of parallel diagonal lines
    val diagonalAngle = Degrees.of(45f)

//    val linePaint = strokeOf(4f, RetroColors.white01).apply {
//        //this.blendMode = BlendMode.DIFFERENCE
//    }
    val rectPaint1 = fillOf(RetroColors.white01).apply {
//        this.blendMode = BlendMode.DIFFERENCE
    }

    val sqWidth = 160f
    val spacing = sqWidth - 120f

    // Calculate the perpendicular angle for spacing
    val perpAngle = diagonalAngle + Degrees.D90

    // Calculate starting point - we need to start from outside the canvas to cover it fully
    val maxDim = sqrt((d.w * d.w + d.h * d.h).toFloat())
    val startOffset = -maxDim

    repeat(n) {
        // Calculate the starting point for this parallel line
        // Move perpendicular to the diagonal direction
        val offset = startOffset + it * spacing
        val startX = -d.w + offset * cosf(perpAngle)
        val startY = d.h - offset * sinf(perpAngle)

        // Create a line at the diagonal angle
        val lineLength = maxDim * 1.5f
        val endX = startX + lineLength * cosf(diagonalAngle)
        val endY = startY - lineLength * sinf(diagonalAngle)

        // Line
        val line = Line(Point(startX, startY), Point(endX, endY))

        // Draw rotated rectangles at each point
        val pointDistance = sqWidth
        val points = line.pointsAtDistance(pointDistance - abs(it) * 3f)
        val lineAngle = line.angle() - Degrees.of((it) * 10.5f)
        points.forEachIndexed { ndx, point ->
//            if ((it + ndx + (1 - it / 5)) % 2 == 0)
            if ((it + ndx) % 2 == 0) {
                c.drawRotatedRect(point.offset(it.f(), it.f()), sqWidth, sqWidth, lineAngle + Degrees.of(ndx * 5), rectPaint1)
            }
            //c.drawRotatedRect(point.offset(it.f(), it.f()), sqWidth - 40, sqWidth - 30, lineAngle, fillOf(RetroColors.black01))
        }
        //c.drawLine(line, linePaint)
        //c.drawString("$it", line.centerPoint().x, line.centerPoint().y, font(FontFamily.OdibeeSans, 80f), fillOfRed())
    }
}

/**
 * Splits a line into points with a given distance between them.
 *
 * @param distance The distance between consecutive points
 * @return List of points spaced at the given distance along the line
 */
fun Line.pointsAtDistance(distance: Float): List<Point> {
    val lineLength = this.length()
    if (lineLength <= 0f) return listOf(this.a)

    val numberOfPoints = (lineLength / distance).toInt() + 1
    return (0 until numberOfPoints).map { i ->
        val currentDistance = i * distance
        if (currentDistance > lineLength) {
            this.b
        } else {
            this.pointFromStartLen(currentDistance)
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
private fun Canvas.drawRotatedRect(center: Point, width: Float, height: Float, angle: Angle, paint: Paint) {
    this.save()
    this.rotate(angle.degrees, center.x, center.y)
    val rect = Rect.ofCenter(center, width, height)
    this.drawRect(rect, paint)
    this.clipRect(rect)

    val radius = width.coerceAtMost(height) / 2.4f
    val point = rect.bottomLeftPoint()
    this.drawCircle(center, radius, fillOf(RetroColors.red01))

    this.restore()
    this.restore()
}
