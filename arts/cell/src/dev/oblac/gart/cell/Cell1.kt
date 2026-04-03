package dev.oblac.gart.cell

import dev.oblac.gart.Gart
import dev.oblac.gart.cellular.CellularAutomata
import dev.oblac.gart.cellular.newBelousovZhabotinskyReaction2
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import org.jetbrains.skia.BlendMode

val pal = (Palettes.cool10.reversed() + Palettes.cool10).expand(256)

fun main() {
    val gart = Gart.of("cell1", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas

    val r = newBelousovZhabotinskyReaction2()

    val ca = CellularAutomata(105, 105, r)
    repeat(60) { ca.step() }

    ca.cells().sortedBy { it.state }.forEach {
        c.drawCircle(it.x.toFloat() * 10, it.y.toFloat() * 10, 12f, fillOf(pal[it.state]))
    }

    c.drawCircle(0f, d.hf, 600f, fillOfWhite().apply {
        this.blendMode = BlendMode.DIFFERENCE
    })
    c.drawTriangle(
        Triangle(
            Point(0f, 0f),
            Point(d.wf, 0f),
            Point(d.wf, d.hf)
        ),
        fillOfWhite().apply {
            this.blendMode = BlendMode.DIFFERENCE
        }
    )

    gart.saveImage(g)
    w.showImage(g)
}
