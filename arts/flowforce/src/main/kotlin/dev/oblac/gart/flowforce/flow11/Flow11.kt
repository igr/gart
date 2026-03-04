package dev.oblac.gart.flowforce.flow11

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.NipponColors.col016_KURENAI
import dev.oblac.gart.color.NipponColors.col234_GOFUN
import dev.oblac.gart.flow.Flow2
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.gfx.drawPoint
import dev.oblac.gart.gfx.randomPoint
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.noise.OpenSimplexNoise

fun main() {
    val gart = Gart.of("flow11", 1024, 1024)
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

    // prepare field
    val simplex = OpenSimplexNoise()
    val flowField = FlowField.of(d) { x, y ->
        val n = simplex.random2D(x * 0.001, y * 0.001) * 360f
        Flow2(Degrees.of(n), 1f)
    }

    // prepare points

    var randomPoints = Array(8000) {
        randomPoint(d)
    }.toList()

    // draw

    c.clear(col016_KURENAI)
    repeat(3000) {
        randomPoints = flowField.apply(randomPoints) { oldPoint, newPoint ->
            c.drawPoint(newPoint, strokeOf(col234_GOFUN, 1f).apply {
                alpha = 100
            })
        }
    }

    //gart.saveImage(g)
    val w = gart.window()
    w.showImage(g)
}
