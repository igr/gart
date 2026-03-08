package dev.oblac.gart.flowforce.perl

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.NipponColors.col234_GOFUN
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.alpha
import dev.oblac.gart.flow.Flow2
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.flow.StreamlineTracer
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.map
import dev.oblac.gart.math.rndb
import dev.oblac.gart.noise.OpenSimplexNoise
import dev.oblac.gart.util.middle
import org.jetbrains.skia.PaintStrokeCap
import kotlin.math.cos

fun main() {
    val gart = Gart.of("flow11", 1024, 1024)
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

    // prepare flow field
    val simplex = OpenSimplexNoise()
    val flowField = FlowField.of(d) { x, y ->
        val n = simplex.random2D(x * 0.001, y * 0.001) * 360f
        Flow2(Degrees.of(n), StreamlineTracer.STEP_SIZE)
    }

    val paths = StreamlineTracer(d, flowField).trace()

    //val p = Palettes.cool9
    //val p = Palettes.cool19
//    val p = Palettes.cool22
    val p = Palettes.cool25

    c.clear(RetroColors.black01)

    var circle: Circle? = Circle(
        Point(d.w3x2, d.h3x2), 120f
    )

    paths.map { Pair(it, it.points().middle()) }
        .sortedBy { it.second.y }
        .forEach { (path, point) ->
            val width = cos(point.x * 0.01f) * 6f + 8f
            val color = p.safe(map(width, 0, 14f, 0, p.size - 1).toInt())
            val paint = strokeOf(color, width).apply {
                strokeCap = PaintStrokeCap.ROUND
            }
            if (rndb(1, 6)) {
                generateSequence(2) { it + 2 }
                    .map { path.toPoints(it) }
                    .first { it[0].distanceTo(it[1]) < 20f }
                    .forEach {
                        c.drawCircle(it, width * 0.5f, fillOf(color.alpha(100)))
                    }
            } else {
                c.drawPath(path, paint)
            }
            if (circle != null) {
                if (point.y > circle.center.y) {
                    c.drawCircle(circle, fillOf(col234_GOFUN))
                    c.drawCircle(circle, strokeOf(RetroColors.black01, 20f))
                    circle = null
                }

            }
        }

    gart.saveImage(g)
    val w = gart.window()
    w.showImage(g)
}
