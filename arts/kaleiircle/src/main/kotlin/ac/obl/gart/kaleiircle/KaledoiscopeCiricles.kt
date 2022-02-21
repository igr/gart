package ac.obl.gart.kaleiircle

import ac.obl.gart.Gartvas
import ac.obl.gart.ImageWriter
import ac.obl.gart.gfx.fillOfBlack
import ac.obl.gart.skia.Color4f
import ac.obl.gart.skia.Rect

const val name = "kaleiircle"

val box = Box(710, 710)

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
)

const val r0 = 100f
const val rw = 50f

val circles = List(colors.size) {
    DHCircle(
        r0 + rw * it, rw,
        colors[it],
        -10f + it*15,
        when (it < 5) {
            true -> DHType.FULL
            false -> DHType.CIRCLE
        }
    )
}

val triangles = List(triangleColors.size) {
    DHCircle(
        r0 + rw, 1f,
        triangleColors[it],
        -10f - (it+1) *15,
        DHType.TRIANGLE
    )
}

val makeShapeOfCircle = MakeShapeOfCircle(box)

fun main() {
    println(name)

    val g = Gartvas(box.w, box.h)       // todo

    val shapes1 = triangles.map { makeShapeOfCircle(it) }
    val shapes = circles.map { makeShapeOfCircle(it) }
    // background
    g.canvas.drawRect(Rect(0f, 0f, g.wf, g.hf), fillOfBlack())

//    MakeWaves(box).invoke().draw(g.canvas)

    for (t in shapes1) {
        t.draw(g.canvas)
    }
    for (s in shapes) {
        s.draw(g.canvas)
    }

    ImageWriter(g).save("$name.png")
}
