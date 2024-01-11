package dev.oblac.gart.circledots

import dev.oblac.gart.*
import dev.oblac.gart.gfx.fillOfWhite
import dev.oblac.gart.skia.Rect
import kotlin.time.Duration.Companion.seconds

const val w: Int = 640
const val h: Int = w

const val rowCount = 25
const val gap = w / (rowCount - 2)

const val fps = 50

val d = Dimension(w, h)
val g = Gartvas(d)
val ctx = Context(g)
val anim = Animation(g, fps)
val frames = anim.frames
val window = Window(anim).show()
val v = GartvasVideo(g, "circledots.mp4", fps)

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
    if (change) drawCircle = !drawCircle
    g.canvas.drawRect(Rect(0f, 0f, w.toFloat(), h.toFloat()), fillOfWhite())
    for (circle in circles) {
        circle.draw(drawCircle)
    }
}

fun main() {
    println("CircleDots")

    var tick = 0

    val markEverySecond = frames.marker().onEvery(1.seconds)
    val markEnd = frames.marker().atFrame(8 * fps)

    window.draw {
        tick = if (markEverySecond.now()) tick + 1 else tick
        drawAll(tick.mod(2) == 0)

        if (before(markEnd)) {
            v.addFrame()
        } else {
            v.stopAndSaveVideo()
        }
    }

    g.writeSnapshotAsImage("circledots.png")
    println("Done")
}
