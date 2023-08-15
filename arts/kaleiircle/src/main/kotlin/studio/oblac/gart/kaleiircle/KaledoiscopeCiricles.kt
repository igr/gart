package studio.oblac.gart.kaleiircle

import studio.oblac.gart.*
import studio.oblac.gart.gfx.fillOfBlack
import studio.oblac.gart.math.sind
import studio.oblac.gart.skia.Color4f
import studio.oblac.gart.skia.Rect

const val name = "kaleiircle"

val d = Dimension(710, 710)

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

const val r0 = 100f
const val rw = 50f

fun circles(angle: Float, tick: Long): List<DHCircle> {
    val a = 15f
    val x = sind(tick) * sind(tick)
    return List(colors.size) {
        DHCircle(
            r0 + rw * it + 20 * it * x, rw + 20 * x,
            colors[it],
            a,
            angle + it*a + 20 * it * x,
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
        angle - (it+1) *15,
        DHType.TRIANGLE
    )
}

val makeShapeOfCircle = MakeShapeOfCircle(d)

fun paint(g: Gartvas, frames: Frames) {
    println(frames)

    val shapes1 = triangles(-10f + frames.count()).map { makeShapeOfCircle(it) }
    val shapes2 = circles(-10f + frames.count(), frames.count()).map { makeShapeOfCircle(it) }

    g.canvas.drawRect(Rect(0f, 0f, g.d.wf, g.d.hf), fillOfBlack())

//    MakeWaves(box).invoke().draw(g.canvas)

    for (t in shapes1) {
        t.draw(g.canvas)
    }
    for (s in shapes2) {
        s.draw(g.canvas)
    }
}

fun main() {
    println(name)

    val g = Gartvas(d)
    val frames = 30
    val window = Window(g, frames).show()
    val v = VideoGartvas(g).start("$name.mp4", frames)
    val endMarker = v.frames.marker().atSecond(18)

    window.paint {
        paint(g, it)
        when {
            endMarker.before() -> v.addFrame()
            endMarker.now() -> v.save()
        }
    }

    writeGartvasAsImage(g, "$name.png")
}
