package dev.oblac.gart.roundrects

import dev.oblac.gart.Gart
import dev.oblac.gart.Media
import dev.oblac.gart.gfx.Colors
import dev.oblac.gart.math.rnd
import kotlin.time.Duration.Companion.seconds

val gart = Gart.of(
    "roundrects",
    640, 640
)

fun main() {
    with(gart) {
        println(name)

        val onSceneChange = f.marker().onEvery(2.seconds)

        var bigBox = BigBox(d, 4, 4)

        var totalChanges = 10;

        w.show()
        a.record()
        a.draw {
            if (f isNow onSceneChange) {
                val count = rnd(3, 9)
                bigBox = BigBox(d, count, count)
                if (totalChanges-- == 0) {
                    a.stop()
                    return@draw
                }
            }

            g.canvas.clear(Colors.blackColor.toColor())
            bigBox.allCells.forEach { it.draw(g.canvas, f.time) }
        }

        Media.saveImage(this)
        Media.saveVideo(this)
    }
}

