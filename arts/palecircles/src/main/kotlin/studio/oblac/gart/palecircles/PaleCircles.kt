package studio.oblac.gart.palecircles

import org.jetbrains.skia.Rect
import studio.oblac.gart.*
import studio.oblac.gart.gfx.*
import studio.oblac.gart.math.rnd

val d = Dimension(800, 800)
val g = Gartvas(d)
const val numberOfCircles = 12
val size = d.w / numberOfCircles

val matrix: Array<Array<CircleSet>> = Array(numberOfCircles) { row ->
    Array(numberOfCircles) { col ->
        CircleSet(createCircleSet(size, 16), col * size, row * size)
    }
}

fun main() {
    val name = "palecircles"
    println(name)

    val w = Window(g).show()
    val v = GartvasVideo(g, "$name.mp4", 30)

    w.paint2 { frames ->
        draw()
        v.addFrame()
        if (frames.time() > 10) {
            return@paint2 false
        }
        return@paint2 true
    }
    v.stopAndSaveVideo()

    g.writeSnapshotAsImage("$name.png")
}

fun draw() {
    g.canvas.drawRect(Rect(0f, 0f, d.w.toFloat(), d.h.toFloat()), fillOfWhite())

    // draw every tick
    var i = 0
    matrix.forEach { row ->
        row.forEach { circleSet ->
            if (i++ % 2 == 0) drawCircleSet(circleSet)
        }
    }
    i = 1
    matrix.forEach { row ->
        row.forEach { circleSet ->
            if (i++ % 2 == 0) drawCircleSet(circleSet)
        }
    }
    g.canvas.drawRect(Rect(0f, 0f, d.w.toFloat(), d.h.toFloat()), strokeOf(0xFF1A1A1A, 50f))

    // increase off
    matrix.forEach { row ->
        row.forEach { circleSet ->
            circleSet.circles.forEach { circle ->
                circle.off += rnd(-1f, 1f)
                if (circle.off > 4) {
                    circle.off = 4f
                }
                if (circle.off < 0) {
                    circle.off = 0f
                }
            }
        }
    }

}

fun drawCircleSet(circleSet: CircleSet) {
    circleSet.circles.reversed().forEach { circle ->
        g.canvas.drawCircle(
            circleSet.x + circle.x.toFloat(),
            circleSet.y + circle.y.toFloat(),
            circle.rOff(), fillOf(alpha(circle.color, 0x55)))

        g.canvas.drawCircle(
            circleSet.x + circle.x.toFloat(),
            circleSet.y + circle.y.toFloat(),
            circle.rOff(), strokeOfBlack(0.5f))
    }
}