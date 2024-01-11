package dev.oblac.gart.palecircles

import dev.oblac.gart.Gart
import dev.oblac.gart.GartvasVideo
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rnd
import org.jetbrains.skia.Rect
import kotlin.time.Duration.Companion.seconds

val gart = Gart.of(
    "palecircles",
    800, 800,
    30
)

const val numberOfCircles = 12
val size = gart.d.w / numberOfCircles

val matrix: Array<Array<CircleSet>> = Array(numberOfCircles) { row ->
    Array(numberOfCircles) { col ->
        CircleSet(createCircleSet(size, 16), col * size, row * size)
    }
}

fun main() {
    val name = "palecircles"
    println(name)

    gart.w.show()
    val v = GartvasVideo(gart.g, "$name.mp4", 30)
    val endMarker = v.frames.marker().atTime(10.seconds)

    gart.a.draw {
        draw()
        v.addFrame()
        if (endMarker.after()) {
            gart.a.stop()
        }
    }
    v.stopAndSaveVideo()

    gart.g.writeSnapshotAsImage("$name.png")
}

fun draw() {
    val g = gart.g
    val d = gart.d

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
    val g = gart.g
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
