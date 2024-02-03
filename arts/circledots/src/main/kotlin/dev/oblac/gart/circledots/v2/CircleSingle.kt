package dev.oblac.gart.circledots.v2

import dev.oblac.gart.Gart
import dev.oblac.gart.Media
import dev.oblac.gart.circledots.Context
import dev.oblac.gart.gfx.Colors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val gart = Gart.of(
    "CircleDots2",
    640, 640,
    50
)
private val ctx = Context(gart.g)

fun main() {
    with(gart) {

        println(name)

        var tick = 0

        val ccs = createAnimation(320, 320, 220)

        w.show()
        a.draw {
            tick++
            if (tick > 440) {
                a.record()
            }
            g.fill(Colors.black)
            for (cc in ccs) {
                cc.draw()
            }
            if (tick == 2000) {
                a.stop()
                return@draw
            }
        }
        Media.saveImage(this, "circledots2.png")
        Media.saveVideo(this, "circledots2.mp4")
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
            speed = 4f,
            it * 2
        )
    }
}
