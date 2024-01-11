package dev.oblac.gart.roundrects

import dev.oblac.gart.*
import dev.oblac.gart.gfx.Colors
import dev.oblac.gart.math.rnd
import kotlin.time.Duration.Companion.seconds

fun main() {
    val name = "roundrects"
    println(name)

    val d = Dimension(640, 640)
    val g = Gartvas(d)
    val a = Animation(g)
    val w = Window(a).show()
    val onSceneChange = a.frames.marker().onEvery(2.seconds)
    val v = GartvasVideo(g, "$name.mp4")

    var bigBox = BigBox(d, 4, 4)

    var totalChanges = 10;
    w.drawWhile { frames ->

        if (onSceneChange.now()) {
            val count = rnd(3, 9)
            bigBox = BigBox(d, count, count)
            if (totalChanges-- == 0) {
                v.stopAndSaveVideo()
                return@drawWhile false
            }
        }

        g.canvas.clear(Colors.blackColor.toColor())
        bigBox.allCells.forEach { it.draw(g.canvas, frames.time) }
        v.addFrame()
        return@drawWhile true
    }
    g.writeSnapshotAsImage("roundrects.png")
}

