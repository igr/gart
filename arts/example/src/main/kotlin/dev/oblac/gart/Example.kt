package dev.oblac.gart

import dev.oblac.gart.gfx.Colors
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.fillOfRed
import dev.oblac.gart.noise.HaltonSequenceGenerator
import dev.oblac.gart.skia.Rect

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val gart = Gart("example")
    println(gart.name)

    // second canvas

    val d2 = gart.dimension(10, 10)
    val g2 = gart.gartvas(d2)

    g2.draw { c, _ ->
        c.drawCircle(5f, 5f, 5f, fillOf(Colors.coral))
    }
    val snapshot2 = g2.snapshot()

    // main canvas

    val d1 = gart.dimension(162, 100)
    val g1 = gart.gartvas(d1)

    g1.draw { c, d ->
        c.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOf(0xFF174185))
        c.drawCircle(d.w / 2f, d.h / 2f, 30f, fillOfRed())
        c.drawImage(snapshot2, 30f, 30f)
        c.drawImage(snapshot2, 30f, 50f)
    }

    // bitmap

    val b = gart.gartmap(g1)
    println(b[0, 0].toHexString())

    b.forEach { x, y, v ->
        if (v == 0xFFFF0000.toInt()) {  // red detected
            if ((x + y).mod(2) == 0) {
                b[x, y] = 0xFFFFFF00
            }
        }
    }

    // add bitmap noise

    val halton = HaltonSequenceGenerator(2)
    for (i in 1 until 1000) {
        halton.get().toList().zipWithNext().forEach {
            val (x, y) = it
            val x1 = (x * b.d.w).toInt()
            val y1 = (y * b.d.h).toInt()
            b[x1, y1] = Colors.black
        }
    }

    // draw a line and a dot
    for (x in 0 until b.d.w) {
        b[x, 0] = 0xFFFF0044
    }
    b[0, 0] = 0xFF00FF00

    // draw back
    b.drawToCanvas()

    // THE END, save

    gart.saveImage(g1)

    // show image
    gart.showImage(g1)
}
