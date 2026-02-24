package dev.oblac.gart.lines.stripes1

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.drawBorder
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathEffect
import org.jetbrains.skia.Point
import kotlin.math.sin

fun main() {
    val gart = Gart.of("stripes1", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // Hot reload requires a real class to be created, not a lambda!

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

private data class Stripe(
    val xoff: Float,
    val deltaY: Int,
    val amplitude: Float,
    val frequency: Float,
)

private fun draw(c: Canvas, d: Dimension) {
    c.clear(CssColors.black)

    var prev = Stripe(
        xoff = -100f,
        deltaY = 10,
        amplitude = 30f,
        frequency = 0.02f
    )

//    val pal = Palettes.colormap004 + Palettes.colormap004.reversed()
    val pal = Palettes.colormap006 + Palettes.colormap006.reversed()

    repeat(12) {
        val new = Stripe(
            xoff = prev.xoff + 100f,
            deltaY = if (prev.deltaY > 0) -20 else 20,
            amplitude = 20f + rndf(-5f, 20f),
            frequency = 0.006f + rndf(0.0f, 0.008f)
        )

        drawStripe(
            c, d,
            prev.xoff, prev.deltaY, prev.amplitude, prev.frequency,
            new.amplitude, new.frequency,
            pal.safe(it)
        )

        c.drawCircle(
            Point(it * 100f, d.h3 + rndf(-100f, 100f)), rndf(80f, 120f),
            fillOf(pal.safe(it + 2)).apply {
                this.alpha = 100
                this.pathEffect = PathEffect.makeDiscrete(20.0f, 8.0f, 123)
            })

        prev = new
    }

    c.drawBorder(d, 20f, CssColors.white)
}

private fun drawStripe(
    c: Canvas,
    d: Dimension,
    xoff: Float,
    delta: Int,
    amplitude1: Float,
    frequency1: Float,
    amplitude2: Float,
    frequency2: Float,
    color: Int
) {
    val pointCount = 150

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
        xStart = xoff + 100f,
        height = d.hf,
        amplitude = amplitude2,
        frequency = frequency2,
        pointCount = pointCount
    )

//    c.drawPath(path1.toPath(), strokeOf(Color.BLUE, 3f))
//    c.drawPath(path2.toPath(), strokeOf(Color.RED, 2f))

    val drawColor = strokeOf(3f, color).apply {
        this.alpha = 255
    }
    for (i in path1.indices) {
        val targetIndex = i + delta
        if (targetIndex < path2.size && targetIndex >= 0) {
            val point1 = path1[i]
            val point2 = path2[targetIndex]
            c.drawLine(point1.x, point1.y, point2.x, point2.y, drawColor)

            c.drawCircle(point2.x, point2.y, rndf(10f, 20f), fillOf(color).apply {
                this.alpha = 100
            })
        }
    }

}

/**
 * Creates a vertical sinusoidal path from Points.
 * The path oscillates horizontally while progressing vertically.
 */
private fun createVerticalSinePath(
    xStart: Float,
    height: Float,
    amplitude: Float,
    frequency: Float,
    pointCount: Int
): List<Point> {
    val points = mutableListOf<Point>()

    // Generate points for the vertical sine wave
    for (i in -100..pointCount + 100) {
        val y = (i.toFloat() / pointCount) * height
        val x = xStart + amplitude * sin(y * frequency)
        points.add(Point(x, y))
    }
    return points
}
