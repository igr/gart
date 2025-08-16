package dev.oblac.gart.circledots

import dev.oblac.gart.Gart
import dev.oblac.gart.gfx.fillOfWhite
import dev.oblac.gart.toFrames
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import kotlin.time.Duration.Companion.milliseconds

private val gart = Gart.of(
    "CircleDots",
    640, 640, 30
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
    val everySecond = 500.milliseconds.toFrames(w.fps)
    w.show { c, _, f ->
        f.onEveryFrame(everySecond) {
            tick++
            for (circle in circles) {
                circle.tick()
            }
        }
        drawAll(c, tick.mod(2) == 0)
    }
}

