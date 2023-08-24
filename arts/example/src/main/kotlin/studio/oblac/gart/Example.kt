package studio.oblac.gart

import studio.oblac.gart.gfx.Colors
import studio.oblac.gart.gfx.fillOf
import studio.oblac.gart.gfx.fillOfRed
import studio.oblac.gart.math.Constants.goldenRatio
import studio.oblac.gart.noise.HaltonSequenceGenerator
import studio.oblac.gart.skia.Rect

fun main() {
    println("Example")

    val h = 100
    val w = (h * goldenRatio).toInt()
    val d = Dimension(w, h)
    val g = Gartvas(d)

    // use canvas

    g.canvas.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOf(0xFF174185))
    g.canvas.drawCircle(w / 2f, h / 2f, 30f, fillOfRed())

    // second canvas

    val d2 = Dimension(10, 10)
    val g2 = Gartvas(d2)
    g2.canvas.drawCircle(5f, 5f, 5f, fillOf(Colors.coral))

    g.draw(g2, 30f, 30f)
    g.draw(g2, 50f, 95f)

    // get bitmap

    val b = Gartmap(g)

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
            val x1 = (x * w).toInt()
            val y1 = (y * h).toInt()
            b[x1, y1] = Colors.black
        }
    }

    // draw a line and a dot
    for (x in 0 until w) {
        b[x, 0] = 0xFFFF0044
    }
    b[0, 0] = 0xFF00FF00

    // draw back
    b.draw()

    g.writeSnapshotAsImage("example.png")
}
