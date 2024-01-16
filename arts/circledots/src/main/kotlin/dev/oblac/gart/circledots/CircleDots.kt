package dev.oblac.gart.circledots

import dev.oblac.gart.Gart
import dev.oblac.gart.Media
import dev.oblac.gart.gfx.fillOfWhite
import dev.oblac.gart.skia.Rect
import kotlin.time.Duration.Companion.seconds

val gart = Gart.of(
    "CircleDots",
    640, 640,
    50
)

const val rowCount = 25
val gap = gart.d.w / (rowCount - 2)

val ctx = Context(gart.g)

val circles = Array(rowCount * rowCount) {
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
private fun drawAll(change: Boolean) {
    val g = gart.g
    val w = gart.d.w
    val h = gart.d.h
    if (change) drawCircle = !drawCircle
    g.canvas.drawRect(Rect(0f, 0f, w.toFloat(), h.toFloat()), fillOfWhite())
    for (circle in circles) {
        circle.draw(drawCircle)
    }
}

fun main() {
    with(gart) {

        println(name)

        var tick = 0

        val markEverySecond = f.marker().onEvery(1.seconds)
        val markEnd = f.marker().atFrame(8 * f.fps)

        w.show()
        a.record()
        a.draw {
            tick = if (markEverySecond.now()) tick + 1 else tick
            drawAll(tick.mod(2) == 0)

            if (f after markEnd) {
                a.stop()
            }
        }

        Media.saveImage(this)
        Media.saveVideo(this)
    }
}
