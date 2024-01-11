package dev.oblac.gart.roundrects

import dev.oblac.gart.Gart
import dev.oblac.gart.GartvasVideo
import dev.oblac.gart.gfx.Colors
import dev.oblac.gart.math.rnd
import kotlin.time.Duration.Companion.seconds

val gart = Gart.of(
    "roundrects",
    640, 640
)

fun main() {
    with(gart) {
        val name = "roundrects"
        println(name)

        w.show()
        val onSceneChange = a.frames.marker().onEvery(2.seconds)
        val v = GartvasVideo(g, "$name.mp4")

        var bigBox = BigBox(d, 4, 4)

        var totalChanges = 10;
        a.draw {

            if (onSceneChange.now()) {
                val count = rnd(3, 9)
                bigBox = BigBox(d, count, count)
                if (totalChanges-- == 0) {
                    v.stopAndSaveVideo()
                    a.stop()
                    return@draw
                }
            }

            g.canvas.clear(Colors.blackColor.toColor())
            bigBox.allCells.forEach { it.draw(g.canvas, f.time) }
            v.addFrame()
        }

        g.writeSnapshotAsImage("roundrects.png")
    }
}

