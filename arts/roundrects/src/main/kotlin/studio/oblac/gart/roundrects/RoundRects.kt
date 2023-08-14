package studio.oblac.gart.roundrects

import studio.oblac.gart.*
import studio.oblac.gart.gfx.Colors
import studio.oblac.gart.math.rnd
import kotlin.time.Duration.Companion.seconds

fun main() {
    val name = "roundrects"
    println(name)

    val box = Box(640, 640)
    val g = Gartvas(box)
    val w = Window(g).show()
    val onSceneChange = w.frames.marker().onEvery(2.seconds)
    val v = VideoGartvas(g).start("$name.mp4")

    var bigBox =  BigBox(box, 4, 4)

    var totalChanges = 10;
    w.paint2 { frames ->

        if (onSceneChange.now()) {
            val count = rnd(3, 9)
            bigBox = BigBox(box, count, count)
            if (totalChanges-- == 0) {
                v.save()
                return@paint2 false
            }
        }

        g.canvas.clear(Colors.black.toColor())
        bigBox.allCells.forEach { it.draw(g.canvas, frames.time()) }
        v.addFrameIfRunning()
        return@paint2 true
    }
    writeGartvasAsImage(g, "roundrects.png")
}

