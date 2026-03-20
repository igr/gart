package dev.oblac.gart.flowforce.fire

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.flow.Flow2
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.flow.PointTracer
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.random
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.f
import dev.oblac.gart.noise.OpenSimplexNoise
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of("fire", 1024, 1024)
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

//    val p = Palettes.cool66
    val p = Palettes.cool95

    c.clear(p[3])
    val rect = Rect(0f, d.hf - 100f, d.wf, d.hf)
    val points = Array(50) {
        Point.random(rect)
    }.sortedBy { it.x }

    val simplex = OpenSimplexNoise(1)
    val flowField = FlowField.of(d) { x, y ->
        val n = simplex.random2D(-x * 0.001, y * 0.001) * 100f
        val wave = sin(x * 0.01f) * cos(y * 0.008f) * 40f
        Flow2(Degrees.of(n + wave), 5f)
    }
    val traces = points.map {
        PointTracer(d, flowField).trace(it, 200)
    }
    val traces2 = Array(1000) {
        Point.random(d)
    }.sortedBy { it.x }.map {
        PointTracer(d, flowField).trace(it, 200)
    }

    traces2.forEach { trace ->
        trace.forEachIndexed { index, point ->
            val r = (d.hf - point.y) * 0.2f
            c.drawCircle(point, r, fillOf(p.safe(index * 0.02)))
        }
    }

    traces.forEachIndexed { i, trace ->
        val colorFrom = p.safe(i * 0.02)
        val colorTo = p.safe(i * 0.02 + 3)
        val maxWidth = 120f
        val minWidth = 8f

        trace.windowed(2).forEachIndexed { j, (a, b) ->
            val t = j.toFloat() / (trace.size - 1)
            val colorT = (sin(t * 3.14f * 2 + i * 0.5f) * 0.5f + 0.5f)
            val widthWave = abs(cos(t * 4f + i * 0.3f))
            val width = minWidth + widthWave * (maxWidth - minWidth)
            val color = lerpColor(colorFrom, colorTo, colorT)
            val alpha = (255 * (0.6f + 0.4f * sin(t * 5f + i.f()))).toInt()

            val paint = strokeOf(color, width).apply {
                this.strokeCap = PaintStrokeCap.ROUND
                this.alpha = alpha
            }
            c.drawLine(a.x, a.y, b.x, b.y, paint)
        }
    }

    gart.saveImage(g)
    val w = gart.window()
    w.showImage(g)
}
