package dev.oblac.gart.lines.stripes2

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Point
import kotlin.math.sin

fun main() {
    val gart = Gart.of("stripes2", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // Hot reload requires a real class to be created, not a lambda!

    val draw = MyDraw(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.red01)

    repeat(22) {
        drawStripe(
            c, d,
            -100f + it * 70f,
            -10 + (it / 2),
            24f + rndf(-5f, 20f), 0.005f + rndf(0.0f, 0.009f),
            26f + rndf(-5f, 20f), 0.005f + rndf(0.0f, 0.01f)
        )
    }

    //c.drawBorder(d, 20f, Colors.white)
}

private fun drawStripe(
    c: Canvas,
    d: Dimension,
    xoff: Float,
    delta: Int,
    amplitude1: Float,
    frequency1: Float,
    amplitude2: Float,
    frequency2: Float
) {
    val pointCount = 70

    // Create first sinusoidal path starting at xoff
    val path1 = createVerticalSinePath(
        xStart = xoff,
        height = d.hf,
        amplitude = amplitude1,
        frequency = frequency1,
        pointCount = pointCount
    )

    // Create second sinusoidal path starting at xoff + 50
    val path2 = createVerticalSinePath(
        xStart = xoff + 110f,
        height = d.hf,
        amplitude = amplitude2,
        frequency = frequency2,
        pointCount = pointCount - 20
    )

    val path = (path1 + path2.reversed()).toPath().closePath()
    c.drawPath(path, fillOf(RetroColors.white01))

    c.drawPath(path1.toPath(), strokeOf(RetroColors.black01, 3f))
    c.drawPath(path2.toPath(), strokeOf(RetroColors.black01, 2f))

    c.save()
    c.clipPath(path)
    for (i in path1.indices) {
        val targetIndex = i + delta
        if (targetIndex < path2.size && targetIndex >= 0) {
            val point1 = path1[i]
            val point2 = path2[targetIndex]
            val drawColor = strokeOf(2f + i / 12f, RetroColors.black01).apply {
                this.alpha = 255
                this.strokeCap = PaintStrokeCap.ROUND
            }
            val line = Line(point1, point2)
            c.drawLine(line, drawColor)
        }
    }
    c.restore()

}

private fun createVerticalSinePath(
    xStart: Float,
    height: Float,
    amplitude: Float,
    frequency: Float,
    pointCount: Int
): List<Point> {
    val points = mutableListOf<Point>()

    (-100..pointCount + 100).forEach { i ->
        val y = (i.toFloat() / pointCount) * height
        val x = xStart + amplitude * sin(y * frequency)
        points.add(Point(x, y))
    }
    return points
}
