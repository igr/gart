package dev.oblac.gart.ticktiletock

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap

val gart = Gart.of(
    "ticktiletock",
    1024, 1024,
)
const val fps = 1

fun main() {
    println(gart)

        // prepare scenario
    val g = gart.gartvas()
    val b = gart.gartmap(g)
    makeMovie(gart.d, b)

    val w = gart.window(fps = fps)
    w.show { c, d, f ->
        Scenes.draw(g.canvas, d)

        c.drawImage(g.snapshot(), 0f, 0f)

        f.tick {
            Scenes.tick()
        }
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

