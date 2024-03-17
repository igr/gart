package dev.oblac.gart.circledots

import dev.oblac.gart.Gart
import dev.oblac.gart.gfx.fillOfWhite
import dev.oblac.gart.skia.Canvas
import dev.oblac.gart.skia.Rect
import dev.oblac.gart.toFrames
import kotlin.time.Duration.Companion.seconds

private val gart = Gart.of(
    "CircleDots",
    640, 640, 50
)

private const val rowCount = 25
private val gap = gart.d.w / (rowCount - 2)

private val g = gart.gartvas()
private val ctx = Context(g)

private val circles = Array(rowCount * rowCount) {
    val row = it.div(rowCount)
    val column = it.mod(rowCount)
    Circle(
        ctx = ctx,
        x = (column * gap).toFloat() - gap / 2,
        y = (row * gap).toFloat() - gap / 2,
        r = gap * 0.8f,
        deg = ((column + row) * 10).toFloat(),
        speed = 8f
    )
}

var drawCircle = true
private fun drawAll(canvas: Canvas, change: Boolean) {
    val w = gart.d.w
    val h = gart.d.h
    if (change) drawCircle = !drawCircle
    canvas.drawRect(Rect(0f, 0f, w.toFloat(), h.toFloat()), fillOfWhite())
    for (circle in circles) {
        circle.draw(canvas, drawCircle)
    }
}

fun main() {
    println(gart.name)

    var tick = 0

    val w = gart.window()
    val m = gart.movie()

    val everySecond = 1.seconds.toFrames(w.fps)
    val end = 8L * w.fps

    m.record(w).show { c, _, f ->
        f.onEveryFrame(everySecond) {
            tick++
        }

        drawAll(c, tick.mod(2) == 0)

        f.onFrame(end) {
            m.stopRecording()
        }
        f.onFrame(1) {
            gart.saveImage(c)
        }
    }
}

