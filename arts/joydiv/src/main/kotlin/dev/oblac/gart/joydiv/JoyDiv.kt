package dev.oblac.gart.joydiv

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartvas
import dev.oblac.gart.GartvasVideo
import dev.oblac.gart.Window
import dev.oblac.gart.gfx.fillOfBlack
import dev.oblac.gart.gfx.strokeOfBlack
import dev.oblac.gart.skia.Rect

const val w: Int = 640
const val h: Int = 1036 // gold ratio
const val wf = w.toFloat()
const val hf = h.toFloat()
const val frames = 50

val d = Dimension(w, h)
val g = Gartvas(d)
val window = Window(g, frames).show()

val lines = Array(80) {
    Line(g, 200 + (it * 8).toFloat())
}

fun paint() {
    g.canvas.drawRect(Rect(0f, 0f, w.toFloat(), h.toFloat()), fillOfBlack())
    for (line in lines) {
        line.draw()
    }
    g.canvas.drawRect(Rect(0f, 0f, w.toFloat(), h.toFloat()), strokeOfBlack(w * 0.10f))
}

fun main() {
    val name = "joydiv"
    println(name)

    val v = GartvasVideo(g, "${name}.mp4", frames)
    val endMarker = v.frames.marker().atSecond(5)

    window.paint {
        paint()
        when {
            endMarker.before() -> v.addFrame()
            endMarker.now() -> v.stopAndSaveVideo()
        }
    }

    g.writeSnapshotAsImage("${name}.png")
    println("Done")
}
