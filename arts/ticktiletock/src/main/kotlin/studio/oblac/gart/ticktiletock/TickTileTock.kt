package studio.oblac.gart.ticktiletock

import studio.oblac.gart.*

fun main() {
    val name = "ticktiletock"

    println(name)

    val box = Box(1024, 1024)
    val g = Gartvas(box)
    val m = Gartmap(g)
    val w = Window(g).show()
    val v = VideoGartvas(g).start("$name.mp4", 1)

    // prepare scenario
    val tick = w.frames.marker().onEverySecond(1)

    movie(box, m)

    w.paint {
        Scenes.draw(g.canvas)
        if (tick.now()) {
            v.addFrameIfRunning()
            Scenes.tick()
        }
        if (Scenes.isEnd()) {
            v.save()
        }
    }

    writeGartvasAsImage(g, "$name.png")
}

fun movie(box: Box, m: Gartmap) {
    Scenes
        .add(4) { SceneX(box, 32, paintTile2) }
        .add(4) { SceneX(box, 64, paintTile2) }
        .add(4) { SceneAWithFill(box, 64, m) }
        .add(2) { SceneX(box, 128, paintTile2) }
        .add(2) { SceneX(box, 128, paintTile4) }
        .add(2) { SceneX(box, 64, paintTile4) }
        .add(4) { SceneAWithFill2(box, 64, m) }
        .add(2) { SceneX(box, 32, paintCircle) }
        .add(2) { SceneX(box, 32, paintCircleBW) }
        .add(4) { SceneX(box, 16, paintSquares) }
        .add(2) { SceneX(box, 16, paintSquaresFill1) }
        .add(2) { SceneX(box, 16, paintSquaresFill2) }
        .add(1) { SceneX(box, 32, paintTile2) }
}

