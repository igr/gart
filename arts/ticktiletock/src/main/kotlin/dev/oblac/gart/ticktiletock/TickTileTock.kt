package dev.oblac.gart.ticktiletock

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Media
import kotlin.time.Duration.Companion.seconds

val gart = Gart.of(
    "ticktiletock",
    1024, 1024,
    1
)

fun main() {
    with(gart) {
        println(name)

        // prepare scenario
        val tick = f.marker().onEvery(1.seconds)

        makeMovie(d, b)

        w.show()
        a.record()
        a.draw {
            Scenes.draw(g.canvas)
            if (f isNow tick) {
                Scenes.tick()
            }
            if (Scenes.isEnd()) {
                a.stop()
            }
        }

        Media.saveImage(this)
        Media.saveVideo(this)
    }
}

fun makeMovie(d: Dimension, m: Gartmap) {
    Scenes
        .add(4) { SceneX(d, 32, paintTile2) }
        .add(4) { SceneX(d, 64, paintTile2) }
        .add(4) { SceneAWithFill(d, 64, m) }
        .add(2) { SceneX(d, 128, paintTile2) }
        .add(2) { SceneX(d, 128, paintTile4) }
        .add(2) { SceneX(d, 64, paintTile4) }
        .add(4) { SceneAWithFill2(d, 64, m) }
        .add(2) { SceneX(d, 32, paintCircle) }
        .add(2) { SceneX(d, 32, paintCircleBW) }
        .add(4) { SceneX(d, 16, paintSquares) }
        .add(2) { SceneX(d, 16, paintSquaresFill1) }
        .add(2) { SceneX(d, 16, paintSquaresFill2) }
        .add(1) { SceneX(d, 32, paintTile2) }
}

