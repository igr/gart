package dev.oblac.gart.pixelmania.tower

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.GaussianFunction
import dev.oblac.gart.math.rndf
import dev.oblac.gart.shader.createNoiseGrain2Filter
import org.jetbrains.skia.*
import org.jetbrains.skia.Shader.Companion.makeLinearGradient
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of("tower", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw(g)

    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDraw(val g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(Colors.white)

    val g = GaussianFunction(300, d.cx, 400)
    val n = 8
    val gap = d.hf / n
    splitVertical(d, n)
        .forEachIndexed { i, it ->
            val height = g(i * gap) / d.hf + sin(i * 0.9f + 0.4f) * 0.2f
            println(height)
            val r = it.grow(2f)
            c.save()
            c.clipRect(r)
            c.saveLayer(createNoiseGrain2Filter(0.4f, d))
            c.drawRect(r, paint().apply {
                this.isDither = true
                this.mode = PaintMode.FILL
                this.shader = rectTow(r, rndf(30f, 120f), 0.4f - height, 0.8f - height)
            })
            c.restore()
            c.restore()
        }

    c.drawCircle(d.w3, d.h3, 100f, fillOfWhite())

}

/**
 * Creates a linear gradient shader at a specified angle.
 * @param rect The rectangle to fill with the gradient
 * @param angleDegrees The angle in degrees (0° = left to right, 90° = top to bottom)
 * @return Shader with linear gradient
 */
private fun rectTow(rect: Rect, angleDegrees: Float, f1: Float, f2: Float): Shader {
    // Convert angle to radians
    val angleRad = Math.toRadians(angleDegrees.toDouble())

    val rc = rect.center()
    val centerX = rc.x
    val centerY = rc.y

    // Calculate the maximum distance from center (diagonal)
    val maxDistance = kotlin.math.sqrt((rect.width * rect.width + rect.height * rect.height).toDouble()).toFloat() / 2

    // Calculate start and end points along the angle
    val startX = centerX - (cos(angleRad) * maxDistance).toFloat()
    val startY = centerY - (sin(angleRad) * maxDistance).toFloat()
    val endX = centerX + (cos(angleRad) * maxDistance).toFloat()
    val endY = centerY + (sin(angleRad) * maxDistance).toFloat()

    return makeLinearGradient(
        Point(startX, startY),
        Point(endX, endY),
        intArrayOf(Colors.white, Colors.black),
        floatArrayOf(f1, f2),
    )
}

/**
 * Splits dimension d into N vertical slices (rectangles).
 * @param d The dimension to split
 * @param n The number of vertical slices
 * @return List of rectangles representing each vertical slice
 */
private fun splitVertical(d: Dimension, n: Int): List<Rect> {
    require(n > 0) { "Number of slices must be positive" }

    val sliceWidth = d.wf / n
    return (0 until n).map { i ->
        val left = i * sliceWidth
        val right = (i + 1) * sliceWidth
        Rect(left, 0f, right, d.hf)
    }
}
