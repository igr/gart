package dev.oblac.gart.joydiv

import dev.oblac.gart.Gart
import dev.oblac.gart.gfx.fillOfBlack
import dev.oblac.gart.gfx.strokeOfBlack
import dev.oblac.gart.toFrames
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
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

val lines = Array(80) {
    Line(200 + (it * 8).toFloat())
}

fun paint(canvas: Canvas) {
    canvas.drawRect(Rect(0f, 0f, w.toFloat(), h.toFloat()), fillOfBlack())
    for (line in lines) {
        line.draw(canvas)
    }
    canvas.drawRect(Rect(0f, 0f, w.toFloat(), h.toFloat()), strokeOfBlack(w * 0.10f))
}

fun main() {
    println(gart.name)
    val end = 5.seconds.toFrames(gart.fps)

    val w = gart.window()
    val m = gart.movie()
    m.record(w).show { c, _, f ->
        paint(c)
        f.onFrame(end) {
            m.stopRecording()
        }
        f.onFrame(1) {
            gart.saveImage(c)
        }
    }
}
