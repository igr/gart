package dev.oblac.gart.flowforce.flow11

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.NipponColors.col016_KURENAI
import dev.oblac.gart.color.NipponColors.col234_GOFUN
import dev.oblac.gart.flow.Flow2
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.gfx.points
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.noise.OpenSimplexNoise
import dev.oblac.gart.streamlines.StreamlineTracer
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

    val tracer = StreamlineTracer(d, flowField)
    val paths = tracer.trace()

    // assign random widths, sort thickest-first
    c.clear(col016_KURENAI)

    // build outlines
    paths.forEach { path ->
        val point = path.points().middle()
        val width = cos(point.x * 0.01f) * 6f + 8f
        val paint = strokeOf(col234_GOFUN, width).apply {
            this.strokeCap = PaintStrokeCap.ROUND
        }
        c.drawPath(path, paint)
    }

    //gart.saveImage(g)
    val w = gart.window()
    w.showImage(g)
}
