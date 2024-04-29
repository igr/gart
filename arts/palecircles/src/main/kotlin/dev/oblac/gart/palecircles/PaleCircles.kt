package dev.oblac.gart.palecircles

import dev.oblac.gart.Gart
import dev.oblac.gart.color.alpha
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.fillOfWhite
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.gfx.strokeOfBlack
import dev.oblac.gart.math.rnd
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

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
    println(gart)

    val w = gart.window()

    w.show { c, _, _ ->
        draw(c)
    }
}

fun draw(canvas: Canvas) {
    val d = gart.d

    canvas.drawRect(Rect(0f, 0f, d.w.toFloat(), d.h.toFloat()), fillOfWhite())

    // draw every tick
    var i = 0
    matrix.forEach { row ->
        row.forEach { circleSet ->
            if (i++ % 2 == 0) drawCircleSet(canvas, circleSet)
        }
    }
    i = 1
    matrix.forEach { row ->
        row.forEach { circleSet ->
            if (i++ % 2 == 0) drawCircleSet(canvas, circleSet)
        }
    }
    canvas.drawRect(Rect(0f, 0f, d.w.toFloat(), d.h.toFloat()), strokeOf(0xFF1A1A1A, 50f))

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

fun drawCircleSet(canvas: Canvas, circleSet: CircleSet) {
    circleSet.circles.reversed().forEach { circle ->
        canvas.drawCircle(
            circleSet.x + circle.x.toFloat(),
            circleSet.y + circle.y.toFloat(),
            circle.rOff(), fillOf(alpha(circle.color, 0x55))
        )

        canvas.drawCircle(
            circleSet.x + circle.x.toFloat(),
            circleSet.y + circle.y.toFloat(),
            circle.rOff(), strokeOfBlack(0.5f)
        )
    }
}
