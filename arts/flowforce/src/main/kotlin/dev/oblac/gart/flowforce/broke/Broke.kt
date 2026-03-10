package dev.oblac.gart.flowforce.broke

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.flow.Flow2
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.flow.PointTracer
import dev.oblac.gart.flow.StreamlineTracer
import dev.oblac.gart.gfx.*
import dev.oblac.gart.noise.OpenSimplexNoise
import dev.oblac.gart.noise.poissonDiskSampling
import org.jetbrains.skia.PathEffect


fun main() {
    val gart = Gart.of("broke", 1024, 1024)
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

    val p = Palettes.cool124
    c.clear(RetroColors.black01)

    val list = poissonDiskSampling(d.rect.shrink(100f), 15f)

    val simplex = OpenSimplexNoise(212)
    val flowField = FlowField.of(d) { x, y ->
        val n = simplex.random2D(x * 0.002, y * 0.002) * 200f
        val abs = (n / 30).toInt() * 30f
        Flow2(Degrees.of(abs), StreamlineTracer.STEP_SIZE)
    }
    val traces = list.map {
        PointTracer(d, flowField).trace(it, 1000)
    }

    val neighborRadius = 10f
    val neighborRadiusSq = neighborRadius * neighborRadius

    val centers = traces.map { it[it.size / 2] }

    traces.forEachIndexed { i, trace ->
        val center = centers[i]
        val neighbors = centers.count { other ->
            val dx = other.x - center.x
            val dy = other.y - center.y
            dx * dx + dy * dy < neighborRadiusSq
        } - 1 // exclude self
        val color = p.safe(neighbors)

        val paint = strokeOf(color, 2f).alpha(100).apply {
            this.pathEffect = PathEffect.makeDiscrete(2f, 2f, 12)
        }

        val percents = (trace.size * 0.1).toInt().coerceAtLeast(1)
        val start = trace.subList(0, percents)
        val middle = trace.subList(percents, trace.size - percents)
        val end = trace.subList(trace.size - percents, trace.size)
        start.forEach {
            c.drawPoint(it, paint)
        }
        c.drawPath(middle.toPath(), paint)
        c.drawPath(end.toPath(), paint)
    }


    gart.saveImage(g)
    val w = gart.window()
    w.showImage(g)
}

