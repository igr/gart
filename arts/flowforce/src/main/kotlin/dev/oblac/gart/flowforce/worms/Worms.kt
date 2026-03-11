package dev.oblac.gart.flowforce.worms

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.flow.Flow2
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.flow.PointTracer
import dev.oblac.gart.flow.StreamlineTracer
import dev.oblac.gart.gfx.Line
import dev.oblac.gart.gfx.alpha
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.gfx.toPath
import dev.oblac.gart.noise.OpenSimplexNoise
import dev.oblac.gart.noise.poissonDiskSampling
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Point


fun main() {
    val gart = Gart.of("worms", 1024, 1024)
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

    val p = Palettes.cool125
//    val p = Palettes.cool127

    c.clear(RetroColors.black01)

    val list = poissonDiskSampling(d.rect, 45f)

    val line = Line(d.rightTop.offset(-20f, 0f), d.leftBottom.offset(20f, 0f))

    val simplex = OpenSimplexNoise(543)
    val flowField = FlowField.of(d) { x, y ->
        val p = Point(x, y)
        val n = simplex.random2D(x * 0.001, y * 0.001) * 2000f
        if (isRightFromLine(line, p)) {
            Flow2(Degrees.of(n), StreamlineTracer.STEP_SIZE)
        } else {
            Flow2(Degrees.of(- 2 * n + x), StreamlineTracer.STEP_SIZE)
        }
    }
    val traces = list.map {
        PointTracer(d, flowField).trace(it, 1000)
    }

    traces.sortedByDescending { it.size }.forEachIndexed { i, trace ->
        val color = p.safe(i)

        val paint = strokeOf(color, 30f).alpha(100).apply {
            this.strokeCap = PaintStrokeCap.ROUND
        }

        c.drawPath(trace.toPath(), paint)
    }

    gart.saveImage(g)
    val w = gart.window()
    w.showImage(g)
}

fun isLeftFromLine(line: Line, p: Point): Boolean {
    return (line.b.x - line.a.x) * (p.y - line.a.y) - (line.b.y - line.a.y) * (p.x - line.a.x) > 0
}

fun isRightFromLine(line: Line, p: Point): Boolean {
    return (line.b.x - line.a.x) * (p.y - line.a.y) - (line.b.y - line.a.y) * (p.x - line.a.x) < 0
}

