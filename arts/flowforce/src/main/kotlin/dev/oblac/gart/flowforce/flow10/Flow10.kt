package dev.oblac.gart.flowforce.flow10

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.ColorMatrices
import dev.oblac.gart.color.NipponColors.col016_KURENAI
import dev.oblac.gart.color.NipponColors.col234_GOFUN
import dev.oblac.gart.flow.Flow2
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.gfx.drawPoint
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.rndGaussian
import dev.oblac.gart.noise.SimplexNoise
import org.jetbrains.skia.*

fun main() {
    val gart = Gart.of("flow10", 1024, 1024)
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

    // prepare field
    val simplex = SimplexNoise
    val flowField = FlowField.of(d) { x, y ->
        val n = simplex.noise(x * 0.01, y * 0.01) * 360f
        Flow2(Degrees.of(n), 1f)
    }

    // prepare points

    var randomPoints = Array(8000) {
        Point(
            rndGaussian(200f, 100f),
            rndGaussian(d.cy, 300f)
        )
    }.toList()

    // draw

    c.clear(col016_KURENAI)
    repeat(1600) {
        randomPoints = flowField.apply(randomPoints) { oldPoint, newPoint ->
            c.drawPoint(newPoint, strokeOf(col234_GOFUN, 1f).apply {
                alpha = 100
            })
        }
    }

    circle(g, c, Point(d.w3, d.h3x2), 200f)
    circle(g, c, Point(d.wf - 200f, 20f), 100f)
//    circle2(g, c, Point(d.w3, d.h3x2), 150f)

    gart.saveImage(g)
    val w = gart.window()
    w.showImage(g)
}

private fun circle(
    g: Gartvas,
    c: Canvas,
    p: Point,
    r: Float
) {
    val snapshot = g.snapshot()
    val circlePath = PathBuilder().addCircle(p.x, p.y, r).detach()
    c.save()
    c.clipPath(circlePath)
    c.drawImage(snapshot, 0f, 0f, Paint().apply {
        colorFilter = ColorFilter.makeMatrix(ColorMatrices.swap(col016_KURENAI, col234_GOFUN))
    })
    c.restore()
}

private fun circle2(
    g: Gartvas,
    c: Canvas,
    p: Point,
    r: Float
) {
    val snapshot = g.snapshot()
    val circlePath = PathBuilder().addCircle(p.x, p.y, r).detach()
    c.save()
    c.clipPath(circlePath)
    c.drawImage(snapshot, 0f, 0f, Paint().apply {
        colorFilter = ColorFilter.makeMatrix(ColorMatrices.swap(col234_GOFUN, col016_KURENAI))
    })
    c.restore()
}
