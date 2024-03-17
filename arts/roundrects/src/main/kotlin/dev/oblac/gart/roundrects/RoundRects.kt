package dev.oblac.gart.roundrects

import dev.oblac.gart.Gart
import dev.oblac.gart.gfx.Colors
import dev.oblac.gart.math.rnd
import dev.oblac.gart.toFrames
import kotlin.time.Duration.Companion.seconds

val gart = Gart.of(
    "roundrects",
    640, 640, 25
)

fun main() {
    println(gart)

    val sceneChange = 2.seconds.toFrames(gart.fps)

    var bigBox = BigBox(gart.d, 4, 4)

    var totalChanges = 10

    val w = gart.window()
    val m = gart.movie()

    m.record(w).show { c, d, f ->
        f.onEveryFrame(sceneChange) {
            val count = rnd(3, 9)
            bigBox = BigBox(d, count, count)
            if (totalChanges-- == 0) {
                m.stopRecording()
            }
        }
        c.clear(Colors.blackColor.toColor())
        bigBox.allCells.forEach { it.draw(c, f.time) }
    }
}

