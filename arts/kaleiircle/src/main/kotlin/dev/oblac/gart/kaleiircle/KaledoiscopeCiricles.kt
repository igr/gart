package dev.oblac.gart.kaleiircle

import dev.oblac.gart.Frames
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.Media
import dev.oblac.gart.gfx.fillOfBlack
import dev.oblac.gart.math.sinDeg
import dev.oblac.gart.skia.Color4f
import dev.oblac.gart.skia.Rect
import kotlin.time.Duration.Companion.seconds

val colors = arrayOf(
    Pair(Color4f(0xffff0503.toInt()), Color4f(0xff0534d3.toInt())),
    Pair(Color4f(0xffe00907.toInt()), Color4f(0xff0876d0.toInt())),
    Pair(Color4f(0xfffc8c08.toInt()), Color4f(0xff08b7ca.toInt())),
    Pair(Color4f(0xfffc8d00.toInt()), Color4f(0xff3bc0c5.toInt())),
    Pair(Color4f(0xfffddd00.toInt()), Color4f(0xff70e0d6.toInt())),
    Pair(Color4f(0xfff9d682.toInt()), Color4f(0xff5fd6b6.toInt())),
    Pair(Color4f(0xffffbab1.toInt()), Color4f(0xff10c090.toInt())),
    Pair(Color4f(0xfff28ea7.toInt()), Color4f(0xff13bd55.toInt())),
)

val triangleColors = arrayOf(
    Pair(Color4f(0xffa8022c.toInt()), Color4f(0xff06517a.toInt())),
    Pair(Color4f(0xffd71c68.toInt()), Color4f(0xff06797b.toInt())),
    Pair(Color4f(0xffe22b8b.toInt()), Color4f(0xff0ba57e.toInt())),
    Pair(Color4f(0xffd32ba7.toInt()), Color4f(0xff26c36b.toInt())),
    Pair(Color4f(0xffb71193.toInt()), Color4f(0xff14bc01.toInt())),
    Pair(Color4f(0xffde1e6d.toInt()), Color4f(0xff0f9307.toInt())),
    Pair(Color4f(0xffa20329.toInt()), Color4f(0xff077d04.toInt())),
    Pair(Color4f(0xffde1e6d.toInt()), Color4f(0xff0f9307.toInt())),
    Pair(Color4f(0xffb71193.toInt()), Color4f(0xff14bc01.toInt())),
    Pair(Color4f(0xffd32ba7.toInt()), Color4f(0xff26c36b.toInt())),
    Pair(Color4f(0xffe22b8b.toInt()), Color4f(0xff0ba57e.toInt())),
    Pair(Color4f(0xffd71c68.toInt()), Color4f(0xff06797b.toInt())),

    )

val gart = Gart.of(
    "kaleiircle",
    710, 710,
    30
)

const val r0 = 100f
const val rw = 50f

fun circles(angle: Float, tick: Long): List<DHCircle> {
    val a = 15f
    val x = sinDeg(tick) * sinDeg(tick)
    return List(colors.size) {
        DHCircle(
            r0 + rw * it + 20 * it * x, rw + 20 * x,
            colors[it],
            a,
            angle + it * a + 20 * it * x,
            when (it < 5) {
                true -> DHType.FULL
                false -> DHType.CIRCLE
            }
        )
    }
}

fun triangles(angle: Float) = List(triangleColors.size) {
    DHCircle(
        r0 + rw, 1f,
        triangleColors[it],
        15f,
        angle - (it + 1) * 15,
        DHType.TRIANGLE
    )
}

val makeShapeOfCircle = MakeShapeOfCircle(gart.d)

fun paint(g: Gartvas, frames: Frames) {
    println(frames)

    val shapes1 = triangles(-10f + frames.count.value).map { makeShapeOfCircle(it) }
    val shapes2 = circles(-10f + frames.count.value, frames.count.value).map { makeShapeOfCircle(it) }

    g.canvas.drawRect(Rect(0f, 0f, g.d.wf, g.d.hf), fillOfBlack())

//    MakeWaves(box).invoke().draw(g.canvas)

    for (t in shapes1) {
        t(g.canvas)
    }
    for (s in shapes2) {
        s(g.canvas)
    }
}


fun main() {
    with(gart) {
        println(name)

        w.show()
        val endMarker = f.marker().atTime(18.seconds)

        a.record()
        a.draw {
            paint(g, f)
            when {
                f isNow endMarker -> a.stop()
            }
        }

        Media.saveImage(this)
        Media.saveVideo(this)
    }
}
