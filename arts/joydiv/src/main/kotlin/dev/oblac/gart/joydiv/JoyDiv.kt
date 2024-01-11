package dev.oblac.gart.joydiv

import dev.oblac.gart.Gart
import dev.oblac.gart.GartvasVideo
import dev.oblac.gart.gfx.fillOfBlack
import dev.oblac.gart.gfx.strokeOfBlack
import dev.oblac.gart.skia.Rect
import kotlin.time.Duration.Companion.seconds

val gart = Gart.of(
    "joydiv",
    640, 1036,  // gold ratio
    50
)
val w: Int = gart.d.w
val h: Int = gart.d.h
val wf = gart.d.wf
val hf = gart.d.hf
val frames = gart.frames.fps
val g = gart.g
val window = gart.window.show()

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
    val markEnd = gart.frames.marker().atTime(5.seconds)

    window.draw {
        paint()
        when {
            markEnd.before() -> v.addFrame()
            markEnd.now() -> v.stopAndSaveVideo()
        }
    }

    g.writeSnapshotAsImage("${name}.png")
    println("Done")
}
