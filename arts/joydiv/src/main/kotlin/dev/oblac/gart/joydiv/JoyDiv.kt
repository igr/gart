package dev.oblac.gart.joydiv

import dev.oblac.gart.Gart
import dev.oblac.gart.Media
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
val frames = gart.f.fps
val g = gart.g

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
    with(gart) {

        println(name)
        val markEnd = f.marker().atTime(5.seconds)
        w.show()
        m.record()
        m.draw {
            paint()
            when {
                f isNow markEnd -> m.stop()
            }
        }

        Media.saveImage(this)
        Media.saveVideo(this)
    }
}
