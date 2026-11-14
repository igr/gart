package palecircles.tiles

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.distanceTo
import dev.oblac.gart.gfx.drawBorder
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.offset
import dev.oblac.gart.gfx.pointBetween
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.gfx.vecTo
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.lerp
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.exp

/**
 * tiles - big discs laid like roof tiles round a hidden sun, the ones inside it red.
 * a disc lies under its neighbour a bit round the sun from it, never straight in. going round you
 * always step under the next one and still come back where you started, an endless stair. the
 * board turns and nothing moves.
 */
private val sunx = pf("sunx", 0.62f, -1f..2f)
private val suny = pf("suny", 0.58f, -1f..2f)
private val sunr = pf("sunr", 0.28f, 0f..2f)
private val out = ps("out", "tiles")

private const val RIM = 4.5f // paper rim round each disc, px
private val paper = RetroColors.white01
private val inkFill = fillOf(RetroColors.black01)
private val redFill = fillOf(RetroColors.red01)
private val rimStroke = strokeOf(paper, RIM)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("tiles", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    draw(g.canvas, g.d)

    gart.saveImage(g, out.ensureExtension("png"))
    if (!headless) gart.window().showImage(g)
}

// the board
private const val N = 12         // discs across
private const val SMALL = 0.55f
private const val BIG = 1.1f
private val TWIST = Degrees(40f)
private const val FALL = 0.3f

private class Tile(val at: Point, val r: Float, val red: Boolean)

private fun draw(c: Canvas, d: Dimension) {
    c.clear(paper)

    val cell = d.wf / N
    val sun = Point(sunx * d.wf, suny * d.hf)
    val tiles = (0 until N).flatMap { j ->
        (0 until N).map { i ->
            val at = Point((i + 0.5f) * cell, (j + 0.5f) * cell)
            val v = sun.vecTo(at)
            val q = v.length() / (FALL * d.wf)
            Tile(sun.offset(v.rotate(TWIST.radians * exp(-q * q))), lerp(SMALL, BIG, (at.x + at.y) / (d.wf + d.hf)) * cell, at.distanceTo(sun) < sunr * d.wf)
        }
    }

    // theres no order to paint this in, so every disc is cut by the ones lying on it. the cuts
    // run along their rims, paper on paper, so no seam shows
    tiles.forEach { t ->
        c.save()
        c.clipPath(disc(t), ClipMode.INTERSECT, true)
        tiles.filter { it !== t && it.at.distanceTo(t.at) < it.r + t.r + RIM && lies(it, t, sun) }
            .forEach { c.clipPath(disc(it), ClipMode.DIFFERENCE, true) }
        c.drawCircle(t.at, t.r, if (t.red) redFill else inkFill)
        c.drawCircle(t.at, t.r, rimStroke)
        c.restore()
    }

    c.drawBorder(d, 40f, paper)
}

private val SPIN = Degrees(70f)

// u lies on t when its nearer the sun along a line turned by SPIN. right in the middle three or so
// discs each wait on the next and none of them gets on top - a paper eye. I keep it
private fun lies(u: Tile, t: Tile, sun: Point): Boolean {
    val m = sun.vecTo(pointBetween(u.at, t.at)).rotate(SPIN.radians)
    val s = t.at.vecTo(u.at).dot(m)
    return if (abs(s) > 1e-3f) s < 0f else u.at.distanceTo(sun) < t.at.distanceTo(sun)
}

private fun disc(t: Tile): Path = PathBuilder().addCircle(t.at.x, t.at.y, t.r + RIM / 2f).detach()
