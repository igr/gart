package studio.oblac.gart.harmongraph

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import studio.oblac.gart.Dimension
import studio.oblac.gart.Gartvas
import studio.oblac.gart.GartvasVideo
import studio.oblac.gart.Window
import studio.oblac.gart.gfx.*
import studio.oblac.gart.math.sinDeg
import kotlin.math.exp
import kotlin.math.sin

val d = Dimension(800, 800)
val g = Gartvas(d)

fun main() {
    val name = "harmongraph"
    println(name)

    val w = Window(g).show()
    val v = GartvasVideo(g, "$name.mp4", 30, dryRun = false)

    var drawing = 4

    val changeMarker = w.frames.marker().onEverySecond(10)

    w.paintWhile { frames ->
        draw(frames.count(), drawing)
        v.addFrame()

        if (changeMarker.now()) {
            drawing--
            g.writeSnapshotAsImage("$name$drawing.png")
        }

        if (drawing == 0) {
            return@paintWhile false
        }
        return@paintWhile true
    }
    v.stopAndSaveVideo()
}

var deltaPhase = 0f

fun draw(tick: Long, drawing: Int) {
    val backTriangle1 = Triangle(
        Point(0f, 0f),
        Point(d.wf, 0f),
        Point(d.wf, d.hf)
    )
    val backTriangle2 = Triangle(
        Point(0f, 0f),
        Point(0f, d.hf),
        Point(d.wf, d.hf)
    )
    g.canvas.drawTriangle(backTriangle1, fillOf(Colors.warmBlack1))
    g.canvas.drawTriangle(backTriangle2, fillOf(Colors.oldLace))

    when (drawing) {
        3 ->
            drawHarmongraph(
                g.canvas, d.cx, d.cy, d.wf - 80, d.hf - 80,
                iterations = 100000,
                p1Start = 0.3f,
                p2Start = 1.7f,
                d1 = 0.001f,
                d2 = 0.002f,
                a = 2.0f,
                b = 2.01f,
            )

        2 ->
            drawHarmongraph(
                g.canvas, d.cx, d.cy, d.wf - 80, d.hf - 80,
                iterations = 100000,
                p1Start = 12.6f,
                p2Start = 1.0f,
                d1 = 0.008f,
                d2 = 0.003f,
                a = 2.03f,
                b = 1.00f,
            )

        1 ->
            drawHarmongraph(
                g.canvas, d.cx, d.cy, d.wf - 80, d.hf - 80,
                iterations = 100000,
                p1Start = 0.6f,
                p2Start = 1.7f,
                d1 = 0.001f,
                d2 = 0.002f,
                a = 1.99f,
                b = 3.01f,
            )
    }

    g.canvas.drawRect(Rect(0f, 0f, d.wf, d.hf), strokeOf(Colors.oldLace, 40f))
    g.canvas.drawLine(0f, 0f, d.wf, d.hf, strokeOf(Colors.oldLace, 20f))

    deltaPhase = sinDeg(tick / 2.5f) / 4
}

fun drawHarmongraph(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    width: Float,
    height: Float,
    iterations: Int,
    p1Start: Float,
    p2Start: Float,
    d1: Float,
    d2: Float,
    a: Float,
    b: Float,
) {
    val p1 = p1Start + deltaPhase
    val p2 = p2Start - deltaPhase
    val delta = 0.01f      // number of steps/dots
    val A = width / 2       // dimensions of square
    val B = height / 2      // dimensions of square

    val dots = mutableListOf<Dot>()

    var t = 0.0f
    for (i in 0..iterations) {
        val x = A * sin(a * t + p1) * exp(-d1 * t)
        val y = B * sin(b * t + p2) * exp(-d2 * t)

        dots += Dot(cx + x, cy + y)
        t += delta
    }

    drawConnectDots(canvas, dots)
}

data class Dot(val x: Float, val y: Float)

fun drawConnectDots(canvas: Canvas, dots: List<Dot>) =
    dots.forEachIndexed() { index, dot ->
        val nextDot = dots.getOrNull(index + 1)
        if (nextDot != null) {
            val color = if (dot.x > dot.y) {
                Colors.white.alpha(0x66)
            } else {
                Colors.black.alpha(0x66)
            }
            canvas.drawLine(dot.x, dot.y, nextDot.x, nextDot.y, strokeOf(color, 1.0f))
        }
    }
