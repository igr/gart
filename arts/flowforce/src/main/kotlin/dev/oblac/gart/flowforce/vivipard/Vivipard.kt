package dev.oblac.gart.flowforce.vivipard

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.flow.Flow2
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.flow.PointTracer
import dev.oblac.gart.gfx.*
import dev.oblac.gart.noise.OpenSimplexNoise
import dev.oblac.gart.noise.poissonDiskSamplingNoise
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun main() {
    val gart = Gart.of("vivipard", 1024, 1024)
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

//    val p = Palettes.cool165
    val p = Palettes.cool85
//    val p = Palettes.cool127
//    val p = Palettes.cool134

    c.clear(RetroColors.black01)

    val flowField1 = OpenSimplexNoise(4).let {
        FlowField.of(d) { x, y ->
            val n = it.random2D(-x * 0.001, y * 0.001) * 90f
            Flow2(Degrees.of(n - 45f), 2f)
        }
    }
    val flowField2 = OpenSimplexNoise(1).let {
        FlowField.of(d) { x, y ->
            val n = it.random2D(x * 0.002, y * 0.001) * 100f
            Flow2(Degrees.of(n + 50f), 1f)
        }
    }

    val traces1 = Array(8) {
        Point(it * 130f, d.hf - it * 130f)
    }.map {
        PointTracer(d, flowField1).trace(it, 250)
    }
//    val traces2 = Array(18000) {
//        randomPoint(d)
//    }.map {
//        PointTracer(d, flowField2).trace(it, 40)
//    }
    val traces2 = poissonDiskSamplingNoise(d, 5f)
        .map {
        PointTracer(d, flowField2).trace(it, 40)
    }

    traces1.forEachIndexed { i, trace ->
        val colorFrom = p.safe(i)
        val colorTo = p.safe(i + 3)
        trace.windowed(2).forEachIndexed { j, (a, b) ->
            val t = j.toFloat() / (trace.size - 1)
            val color = lerpColor(colorFrom, colorTo, t)
            c.drawLine(a.x, a.y, b.x, b.y, strokeOf(60f, color))
        }
    }
    traces2.forEach { trace ->
        val l = trace.last()
        if (isPointInsideOneOfPaths(l, traces1.map { it.toPath() })) {
            c.drawCircle(l, 1f, fillOf(RetroColors.black01))
            return@forEach
        }
        val cx = l.x - d.wf / 2
        val cy = l.y - d.hf / 2
        val r = sqrt(cx * cx + cy * cy)
        val theta = atan2(cy, cx)
        val loff = sin(r * 0.03f + theta * 3f) * 200f +
            cos(theta * 2f - r * 0.01f) * 150f +
            sin(l.x * 0.007f * l.y * 0.005f) * 100f
        c.drawPath(trace.toPath(), strokeOf(1f, p.safe(loff * 0.01f)))
    }

    gart.saveImage(g)
    val w = gart.window()
    w.showImage(g)
}

private fun isPointInsideOneOfPaths(point: Point, path: List<Path>): Boolean {
    return path.any { it.toOutline(80f).outline.contains(point) }
}
