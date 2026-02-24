package dev.oblac.gart.sun.lines

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.pointToCircleTangents
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathEffect
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of("sunlines", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)

    gart.saveImage(g)
    w.showImage(g)
}

private val colorInk = RetroColors.white01
private val colorBold = RetroColors.red01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(CssColors.black)

    val circle = Circle(d.cx, d.cy, 300f)

    //drawInnerCircle(c, circle, colorBold)

    // Generate external points around the circle to create tangent lines
    val numTangentPoints = 24
    val externalRadius = 800f

    for (i in 0 until numTangentPoints) {
        val angle = Degrees(80f * i / numTangentPoints) + Degrees.of(180f)

        val externalPoint = pointOf(
            d.cx + externalRadius * cos(angle.radians),
            d.cy + externalRadius * sin(angle.radians)
        )

        val tangentLines = pointToCircleTangents(externalPoint, circle)

        tangentLines.forEach() {
            drawCrazyTangent(c, it)
        }

        //c.drawCircle(externalPoint.x, externalPoint.y, 3f, fillOfRed())
    }
    drawInnerCircle(c, circle, colorBold)
}

fun drawCrazyTangent(c: Canvas, line: Line, color: Int = colorInk) {
    val lineShorter = Line(line.a, line.pointFromEndLen(80f))
    val points  = lineShorter.toPath().toPoints(80, EaseFn.CircOut)
    points.forEach {
        drawCrazyLine(c, it, color)
    }
    //c.drawCircle(lineShorter.b, 3f, fillOfYellow())
}

private fun drawInnerCircle(c: Canvas, circle: Circle, color: Int = colorInk) {
    val totalInner = 33
    val gap = circle.radius / totalInner.toFloat()
    val off = 6f
    for (i in 1..totalInner) {
        val innerCircle = Circle(circle.center.offset(-i * off, -i * off), circle.radius - gap * i)
        innerCircle.points(200 - (i * gap * 1.4).toInt()).forEach {
            drawCrazyCircles(c, it, color)
        }
    }
}

fun drawCrazyLine(c: Canvas, point: Point, color: Int) {
    val distanceFromDiagonal = point.x - point.y
    val angle = if (distanceFromDiagonal >= 0f) {
        Degrees(100f * rndf(-2f, 0f)) - Degrees.of(90f)
    } else {
        Degrees(100f * rndf(0f, 2f)) + Degrees.of(180f)
    }
    val length = 32f
    val endX = point.x + length * cos(angle.radians)
    val endY = point.y + length * sin(angle.radians)
    val line = Line(point, Point(endX, endY))
    c.drawLine(line, strokeOf(color, 1f).apply {
        this.pathEffect = PathEffect.makeDash(floatArrayOf(8f, 2f), 0f)
        this.alpha = 255 - (point.distanceTo(Point(512f, 512f)).toInt() * 0.2f).toInt()
    })
}

fun drawCrazyCircles(c: Canvas, point: Point, color: Int) {
    c.drawCircle(point.x, point.y, 1f, strokeOf(color, rndf(1f, 18f)))
}
