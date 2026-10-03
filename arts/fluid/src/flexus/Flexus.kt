package flexus

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.brush.Brushes
import dev.oblac.gart.brush.Wobble
import dev.oblac.gart.brush.drawBrush
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.closedPathOf
import dev.oblac.gart.gfx.drawBorder
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.marbling.Custom
import dev.oblac.gart.marbling.Marbling
import dev.oblac.gart.math.between
import dev.oblac.gart.math.lerp
import dev.oblac.gart.util.Stopwatch
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import kotlin.random.Random

/*
 * flexus - a stone bed of marbled paper, with no comb.
 *
 * the bath gets only drops. the big drops come first, and the size decreases as the bed fills.
 * thus the late small drops sit as spots inside the rings of the big ones. the marbling engine
 * makes each drop as a closed-form deformation, so the image is exact at all sizes.
 */

private const val W = 1200
private const val H = 1200
private val SEED = pi("seed", 3)
private val OUT = ps("out", "flexus")
private const val AA = 3

private const val DROPS = 200
private const val MARGIN = 0.05f
private const val RULE = 10f

private const val PAPER = BgColors.plumCharcoal
private val PAL = pi("pal", 202, 1..256)
private val inks = Palettes.coolPalette(PAL)

// the tray is bigger than the sheet, so the bed runs past its edges
private fun stone(rnd: Random): Marbling {
    val m = Marbling(PAPER)
    for (i in 0 until DROPS) {
        val r = W * lerp(0.13f, 0.02f, i / (DROPS - 1f)) * rnd.between(0.7f, 1.3f)
        m.drop(rnd.between(-0.2f, 1.2f) * W, rnd.between(-0.2f, 1.2f) * H, r, inks[i % inks.size])
    }
    return m
}

private fun inset(): Custom {
    val mx = MARGIN * W
    val my = MARGIN * W
    val kx = W / (W - 2f * mx)
    val ky = H / (H - 2f * my)
    return Custom(
        fwd = { p -> p.set(mx + p.x / kx, my + p.y / ky) },
        inv = { p -> p.set((p.x - mx) * kx, (p.y - my) * ky) },
    )
}

private fun mount(c: Canvas, d: Dimension, paper: Int, gilt: Int, rnd: Random) {
    val m = MARGIN * W
    c.drawBorder(d, m, paper)
    val r = m - RULE
    closedPathOf(listOf(Point(r, r), Point(W - r, r), Point(W - r, H - r), Point(r, H - r))).use {
        c.drawBrush(it, Brushes.chalk, gilt, 2f, rnd, Wobble.curved(rnd, amount = 0.03f, scale = 0.004f))
    }
}

fun main(args: Array<String>) {
    val gart = Gart.of("flexus", W, H)
    val sw = Stopwatch()
    val m = stone(Random(SEED))
    m.add(inset())
    val g = gart.gartvas()
    m.render(g, aa = AA)
    mount(g.canvas, gart.d, PAPER, inks.last(), Random(SEED + 1))
    println("flexus: seed=$SEED, pal=$PAL, ${m.ops.size} ops, aa=$AA, ${sw.ms}ms")
    gart.saveImage(g, OUT.ensureExtension())
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
