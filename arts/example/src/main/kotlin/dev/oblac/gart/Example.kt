package dev.oblac.gart

import dev.oblac.gart.gfx.Colors
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.fillOfRed
import dev.oblac.gart.noise.HaltonSequenceGenerator
import dev.oblac.gart.skia.Rect

fun main() {
    val gart = Gart.of(
        "Example",
        162, 100
    )
    println("Example")

    // use canvas
    val g = gart.g
    val d = gart.d

    g.canvas.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOf(0xFF174185))
    g.canvas.drawCircle(d.w / 2f, d.h / 2f, 30f, fillOfRed())

    // second canvas

    val d2 = Dimension(10, 10)
    val g2 = Gartvas(d2)
    g2.canvas.drawCircle(5f, 5f, 5f, fillOf(Colors.coral))

    g.draw(g2, 30f, 30f)
    g.draw(g2, 50f, 95f)

    // get bitmap

    val b = gart.b

    b.forEach { x, y, v ->
        if (v == 0xFFFF0000.toInt()) {  // red detected
            if ((x + y).mod(2) == 0) {
                b[x, y] = 0xFFFFFF00
            }
        }
    }

    // add noise
    val halton = HaltonSequenceGenerator(2)
    for (i in 1 until 1000) {
        halton.get().toList().zipWithNext().forEach {
            val (x, y) = it
            val x1 = (x * d.w).toInt()
            val y1 = (y * d.h).toInt()
            b[x1, y1] = Colors.black
        }
    }

    // draw a line and a dot
    for (x in 0 until d.w) {
        b[x, 0] = 0xFFFF0044
    }
    b[0, 0] = 0xFF00FF00

    // draw back
    b.draw()

    Media.saveImage(gart)

    // show image
    with(gart) {
        w.show()
    }
}
