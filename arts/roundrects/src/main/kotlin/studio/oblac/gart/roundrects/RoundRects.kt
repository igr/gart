package studio.oblac.gart.roundrects

import studio.oblac.gart.Dimension
import studio.oblac.gart.Gartvas
import studio.oblac.gart.GartvasVideo
import studio.oblac.gart.Window
import studio.oblac.gart.gfx.Colors
import studio.oblac.gart.math.rnd
import kotlin.time.Duration.Companion.seconds

fun main() {
    val name = "roundrects"
    println(name)

    val d = Dimension(640, 640)
    val g = Gartvas(d)
    val w = Window(g).show()
    val onSceneChange = w.frames.marker().onEvery(2.seconds)
    val v = GartvasVideo(g, "$name.mp4")

    var bigBox = BigBox(d, 4, 4)

    var totalChanges = 10;
    w.paintWhile { frames ->

        if (onSceneChange.now()) {
            val count = rnd(3, 9)
            bigBox = BigBox(d, count, count)
            if (totalChanges-- == 0) {
                v.stopAndSaveVideo()
                return@paintWhile false
            }
        }

        g.canvas.clear(Colors.blackColor.toColor())
        bigBox.allCells.forEach { it.draw(g.canvas, frames.time()) }
        v.addFrame()
        return@paintWhile true
    }
    g.writeSnapshotAsImage("roundrects.png")
}

