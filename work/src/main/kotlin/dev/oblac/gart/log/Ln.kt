package dev.oblac.gart.log

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.SampleMode
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.knot.Knot
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndi
import dev.oblac.gart.pixels.conformalWarp
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder

private fun generateWaveProfiles(count: Int): List<List<Knot>> = List(count) {
    val knotCount = rndi(3, 10)
    val knots = mutableListOf(Knot(0f, 0f))
    for (i in 1 until knotCount) {
        knots.add(Knot(i.toFloat() / knotCount, rndf(-90f, 90f)))
    }
    knots.add(Knot(1f, 0f))
    knots
}

private fun buildWavePath(knots: List<Knot>, baseY: Float, w: Float): Path {
    val path = PathBuilder()
    val pts = knots.map { Pair(it.x * w, baseY - it.dy) }
    path.moveTo(pts[0].first, pts[0].second)
    for (i in 1 until pts.size) {
        val (x0, y0) = pts[i - 1]
        val (x1, y1) = pts[i]
        val cx = (x0 + x1) / 2
        path.cubicTo(cx, y0, cx, y1, x1, y1)
    }
    return path.detach()
}

fun main() {
    val pal = Palettes.cool32

    val gart = Gart.of("conformal", 1024, 1024)

    val srcGartvas = gart.gartvas()
    val c = srcGartvas.canvas
    val d = gart.d
    c.clear(CssColors.black)

    val waveProfiles = generateWaveProfiles(6)
    val lineCount = waveProfiles.size
    val spacing = d.hf / (lineCount + 1)

    for (i in 0 until lineCount) {
        val baseY = spacing * (i + 1)
        val path = buildWavePath(waveProfiles[i], baseY, d.wf)
        val stroke = strokeOf(pal.safe(i), 4f)
        c.drawPath(path, stroke)
    }

    val src = Gartmap(srcGartvas)
    val result = conformalWarp(
        src = src,
        outDimension = d,
        sampleMode = SampleMode.TILE,
        rInner = 0.1,
        rOuter = 6.0,
        background = CssColors.black
    )
    gart.window().showImage(result.image())
}
