package retro.coil

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.gfx.distanceTo
import dev.oblac.gart.gfx.drawBorder
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.ps
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import kotlin.math.ceil

/**
 * coil - bricks of one length, laid in rings round the sun, ink paper ink paper. a ring is longer
 * than the one inside it, so it takes one more pair. along one line the joints still meet, and
 * there the board is plain straight stripes. everywhere else they slip a bit further with every
 * ring, and the stripes wind in round the sun.
 */
private val sunx = pf("sunx", 0.62f, -1f..2f)
private val suny = pf("suny", 0.58f, -1f..2f)
private val sunr = pf("sunr", 0.28f, 0f..2f)   // the bricks in it are red
private val seam = pf("seam", 218f, 0f..360f)  // where every ring starts a brick, degrees clockwise from 3 oclock. the stripes run along it
private val course = pf("course", 0.028f, 0.004f..0.3f) // how thick a ring is, page widths. a brick is pi times that long
private val out = ps("out", "coil")

private const val SS = 4
private val paper = RetroColors.white01

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("coil", 1024, 1024)
    println(gart)

    val big = gart.supersampled(SS)
    draw(big.canvas, big.d)
    val g = big.downsample(SS)

    gart.saveImage(g, out.ensureExtension("png"))
    if (!headless) gart.window().showImage(g)
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(paper)

    val sun = Point(sunx * d.wf, suny * d.hf)
    val h = course * d.wf
    val wide = h + 1.5f
    val ink = strokeOf(RetroColors.black01, wide).apply { isAntiAlias = false }
    val red = strokeOf(RetroColors.red01, wide).apply { isAntiAlias = false }
    val far = listOf(Point(0f, 0f), Point(d.wf, 0f), Point(0f, d.hf), Point(d.wf, d.hf)).maxOf { it.distanceTo(sun) }

    for (k in 1..ceil(far / h).toInt()) {
        val r = (k - 0.5f) * h
        val pen = if (r < sunr * d.wf) red else ink
        for (j in 0 until k) c.drawArc(sun.x - r, sun.y - r, sun.x + r, sun.y + r, seam + j * 360f / k, 180f / k, false, pen)
    }

    c.drawBorder(d, 40f * d.wf / 1024f, paper)
}
