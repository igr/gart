package dev.oblac.gart.circledots.v2

import dev.oblac.gart.Gart
import dev.oblac.gart.circledots.Context
import dev.oblac.gart.gfx.fillOfBlack
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val gart = Gart.of(
    "CircleDots2",
    640, 640, 50
)
private val g = gart.gartvas()
private val ctx = Context(g)

fun main() {
    println(gart.name)

    val w = gart.window()
    val m = gart.movie()

    var tick = 0

    val ccs = createAnimation(320, 320, 220)

    m.record(w, recording = false).show { c, d, f ->
        f.tick {
            tick++
            if (tick == 440) {
                m.startRecording()
            }
        }

        c.drawPaint(fillOfBlack())
        for (cc in ccs) {
            cc.draw(c)
        }

        if (tick == 2000) {
            m.stopRecording()
        }

        f.onFrame(1) {
            gart.saveImage(c, d, "circledots2.png")
        }
    }
}

private fun createAnimation(x: Int, y: Int, count: Int): Array<Circle2Anim> {
    val delta = 2 * PI / count
    var offset = 0.0
    return Array(count) {
        val angle = it * delta + offset
        offset += delta
        Circle2Anim(
            ctx = ctx,
            x = (x + 200 * cos(angle)).toFloat(),
            y = (y + 200 * sin(angle)).toFloat(),
            r = 80f,
            deg = it * PI.toFloat() * 2,
            speed = 0.4f,
            it * 2
        )
    }
}
