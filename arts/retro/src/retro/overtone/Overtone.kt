package retro.overtone

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.toClosedPath
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PIf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import kotlin.math.sin

/**
 * overtone - a string that rings is a blur, and the blur is a leaf. the next string rings an
 * octave up, two leaves. the third has three, and so on up the series. each string gets as much
 * room as it swings, so every leaf on the page is the first leaf, only smaller. the sun is behind
 * them and shows through the ink, like in all-circles.
 */
private val sunx = pf("sunx", 0.5f, -1f..2f)      // the sun, page widths
private val suny = pf("suny", 0.56f, -1f..2f)
private val sunr = pf("sunr", 0.34f, 0f..2f)
private val n = pi("n", 45, 1..300)            // strings. 30 stops while the leaves are still shapes, 80 ends in grey fuzz
private val out = ps("out", "overtone")

private const val FRAME = 40f / 1024f
private const val FAT = 0.92f  // leaf width over the room its string gets. at 1 the neighbours kiss and its a net
private const val CUTS = 96
private const val SS = 3

private val paper = RetroColors.white01
private val inkFill = fillOf(RetroColors.black01)
private val redFill = fillOf(RetroColors.red01)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("overtone", 1024, 1024)
    println(gart)

    val big = gart.supersampled(SS)
    draw(big.canvas, big.d)
    val g = big.downsample(SS)

    gart.saveImage(g, out.ensureExtension("png"))
    if (!headless) gart.window().showImage(g)
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(paper)
    strings(c, d, inkFill)
    c.save()
    c.clipPath(PathBuilder().addCircle(sunx * d.wf, suny * d.hf, sunr * d.wf).detach(), true)
    strings(c, d, redFill)
    c.restore()
}

// string k is k leaves; each one the first leaf shrunk k times. the widths run 1, 1/2, 1/3.. and
// that sum never settles, so the board has no last string - n is only where we stop
private fun strings(c: Canvas, d: Dimension, fill: Paint) {
    val frame = FRAME * d.wf
    val len = d.hf - 2 * frame
    val room = (d.wf - 2 * frame) / (1..n).sumOf { 1.0 / it }.toFloat() // what the first string gets
    val leaf = leaf(FAT * room, len)

    var x = frame
    for (k in 1..n) {
        for (m in 0 until k) {
            c.save()
            c.translate(x + room / k / 2f, frame + m * len / k)
            c.scale(1f / k, 1f / k)
            c.drawPath(leaf, fill)
            c.restore()
        }
        x += room / k
    }
}

// the blur between sin and -sin, tip up at 0,0
private fun leaf(w: Float, len: Float): Path {
    val right = (0..CUTS).map { it.toFloat() / CUTS }.map { Point(w / 2f * sin(PIf * it), len * it) }
    return (right + right.reversed().map { Point(-it.x, it.y) }).toClosedPath()
}
