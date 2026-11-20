package retro.albers

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.drawBorder
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.ps
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

private val sunx = pf("sunx", 0.6f, -1f..2f)  // the sun, page widths. never drawn, only the bands flip in it
private val suny = pf("suny", 0.58f, -1f..2f)
private val sunr = pf("sunr", 0.33f, 0f..3f)
private val size = pf("size", 0.3f, 0f..0.7f) // dot radius over cell
private val out = ps("out", "albers")

private const val N = 12 // dots across, the band widths below are counted in these cells

// bands left to right, light to dark, uneven on purpose. the teal one ends on a half cell, so its
// edge runs right through a column of dots and halves them; the other edges run between columns
private val bands = Palette.of(RetroColors.white01, RetroColors.yellow01, RetroColors.teal01, RetroColors.blue01, RetroColors.black01)
private val widths = floatArrayOf(2f, 3f, 1.5f, 3.5f, 2f)

private val paper = RetroColors.white01
private val red = fillOf(RetroColors.red01)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("albers", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    draw(g.canvas, g.d)

    gart.saveImage(g, out.ensureExtension("png"))
    if (!headless) gart.window().showImage(g)
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(paper)
    val cell = d.wf / N

    drawBands(c, d, cell, false)
    c.save()
    c.clipPath(PathBuilder().addCircle(sunx * d.wf, suny * d.hf, sunr * d.wf).detach(), true)
    drawBands(c, d, cell, true)
    c.restore()

    for (j in 0 until N) for (i in 0 until N) {
        c.drawCircle(Point((i + 0.5f) * cell, (j + 0.5f) * cell), size * cell, red)
    }

    c.drawBorder(d, 40f, paper)
}

private fun drawBands(c: Canvas, d: Dimension, cell: Float, turned: Boolean) {
    var x = 0f
    for (i in 0 until bands.size) {
        val w = widths[i] * cell
        c.drawRect(Rect(x, 0f, x + w, d.hf), fillOf(bands[if (turned) bands.size - 1 - i else i]))
        x += w
    }
}
