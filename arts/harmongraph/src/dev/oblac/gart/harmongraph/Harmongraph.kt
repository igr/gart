package dev.oblac.gart.harmongraph

import dev.oblac.gart.Frames
import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.alpha
import dev.oblac.gart.gfx.Triangle
import dev.oblac.gart.gfx.drawTriangle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.sinDeg
import dev.oblac.gart.toFrames
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.exp
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

val gart = Gart.of(
    "Harmongraph",
    800, 800,
    30
)

fun main() {
    println(gart)

    var drawing = 3
    val changeMarker = 10.seconds.toFrames(gart.fps)

    val w = gart.window()
    w.show { c, _, f ->
        draw(c, f, drawing)

        f.onEveryFrame(changeMarker) {
            drawing--
            //Media.saveImage(gart, "$name$drawing.png")
        }
    }
}

private var deltaPhase = 0f

private fun draw(canvas: Canvas, tick: Frames, drawing: Int) {
    val d = gart.d

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
    canvas.drawTriangle(backTriangle1, fillOf(BgColors.warmBlack1))
    canvas.drawTriangle(backTriangle2, fillOf(CssColors.oldLace))

    when (drawing) {
        3 ->
            drawHarmongraph(
                canvas, d.cx, d.cy, d.wf - 80, d.hf - 80,
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
                canvas, d.cx, d.cy, d.wf - 80, d.hf - 80,
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
                canvas, d.cx, d.cy, d.wf - 80, d.hf - 80,
                iterations = 100000,
                p1Start = 0.6f,
                p2Start = 1.7f,
                d1 = 0.001f,
                d2 = 0.002f,
                a = 1.99f,
                b = 3.01f,
            )
    }

    canvas.drawRect(Rect(0f, 0f, d.wf, d.hf), strokeOf(CssColors.oldLace, 40f))
    canvas.drawLine(0f, 0f, d.wf, d.hf, strokeOf(CssColors.oldLace, 20f))

    deltaPhase = sinDeg(tick.frame / 2.5f) / 4
}

private fun drawHarmongraph(
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
    repeat(iterations) {
        val x = A * sin(a * t + p1) * exp(-d1 * t)
        val y = B * sin(b * t + p2) * exp(-d2 * t)

        dots += Dot(cx + x, cy + y)
        t += delta
    }

    drawConnectDots(canvas, dots)
}

private data class Dot(val x: Float, val y: Float)

private fun drawConnectDots(canvas: Canvas, dots: List<Dot>) =
    dots.forEachIndexed { index, dot ->
        val nextDot = dots.getOrNull(index + 1)
        if (nextDot != null) {
            val color = if (dot.x > dot.y) {
                CssColors.white.alpha(0x66)
            } else {
                CssColors.black.alpha(0x66)
            }
            canvas.drawLine(dot.x, dot.y, nextDot.x, nextDot.y, strokeOf(color, 1.0f))
        }
    }

