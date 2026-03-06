package dev.oblac.gart.flowforce.cyanowaves

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.CyanotypeColors
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.flow.Flow2
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.length
import dev.oblac.gart.gfx.toPoints
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.OpenSimplexNoise
import dev.oblac.gart.streamlines.StreamlineTracer

fun main() {
    val gart = Gart.of("cyanowaves", 1024, 1024)
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

    // prepare flow field
    val paths = run {
        val simplex = OpenSimplexNoise(123L)
        val flowField = FlowField.of(d) { x, y ->
//            val n = simplex.random2D(x * 0.002, y * 0.002) * 200f
            val n = simplex.random2D(x * 0.002, y * 0.002) * 200f
            Flow2(Degrees.of(n), StreamlineTracer.STEP_SIZE)
        }
        StreamlineTracer(d, flowField, 10f, 550).trace()
    }
//    val p = Palettes.cool37.reversed()
    val p = CyanotypeColors.palette1

    c.clear(RetroColors.black01)

    paths.forEach {
        val len = it.length()
        val pointsCount = (len / 10f).toInt().coerceAtLeast(2)
        val points = it.toPoints(pointsCount)
        val gradient = if (pointsCount <= p.size) p else p.expand(points.size)
        points.forEachIndexed { i, pp ->
            c.drawCircle(pp.x, pp.y, rndf(3, 6f), fillOf(gradient[i]))
        }
    }

    gart.saveImage(g)
    val w = gart.window()
    w.showImage(g)
}

