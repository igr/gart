package ac.obl.gart.joydiv

import ac.obl.gart.*
import ac.obl.gart.gfx.fillOfBlack
import ac.obl.gart.gfx.strokeOfBlack
import io.github.humbleui.types.Rect

const val w: Int = 640
const val h: Int = 1036 // gold ratio
const val wf = w.toFloat()
const val hf = h.toFloat()
const val frames = 50

val g = Gartvas(w, h)
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

    val v = VideoGartvas(g).start("${name}.mp4", frames)

    window.paint {
        paint()
        v.addFrameUntil(frames * 5) { it.save() }
    }

    ImageWriter(g).save("${name}.png")
    println("Done")
}