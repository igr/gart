package dev.oblac.gart.ticktiletock

import dev.oblac.gart.*

fun main() {
    val name = "ticktiletock"

    println(name)

    val d = Dimension(1024, 1024)
    val g = Gartvas(d)
    val m = Gartmap(g)
    val w = Window(g).show()
    val v = GartvasVideo(g, "$name.mp4", 1)

    // prepare scenario
    val tick = w.frames.marker().onEverySecond(1)

    movie(d, m)

    w.paint {
        Scenes.draw(g.canvas)
        if (tick.now()) {
            v.addFrame()
            Scenes.tick()
        }
        if (Scenes.isEnd()) {
            v.stopAndSaveVideo()
        }
    }

    g.writeSnapshotAsImage("$name.png")
}

fun movie(d: Dimension, m: Gartmap) {
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

