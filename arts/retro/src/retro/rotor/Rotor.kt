package retro.rotor

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.angle.cosf
import dev.oblac.gart.angle.sinf
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.distanceTo
import dev.oblac.gart.gfx.drawBorder
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.offset
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.gfx.vecTo
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.wrapPi
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.ln

/**
 * rotor - "tiles" little brother. the plain dot board, but near a hidden sun the dots swell until
 * they overlap, and each one goes under the next one round the sun. the big ones turn into blades,
 * and right on the sun the order runs in a circle, so nothing gets on top and the paper shows.
 */
private val sunx = pf("sunx", 0.62f, -1f..2f)    // the sun
private val suny = pf("suny", 0.58f, -1f..2f)
private val swell = pf("swell", 0.3f, 0.01f..2f)
private val out = ps("out", "rotor")

private const val RIM = 4.5f // paper rim round each disc
private val paper = RetroColors.white01
private val inkFill = fillOf(RetroColors.black01)
private val redFill = fillOf(RetroColors.red01)
private val rimStroke = strokeOf(paper, RIM)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("rotor", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    draw(g.canvas, g.d)

    gart.saveImage(g, out.ensureExtension("png"))
    if (!headless) gart.window().showImage(g)
}

// the board
private const val N = 14        // discs across
private const val FAR = 0.4f      // disc radius over cell, far from the sun. under 0.5 they are just dots
private const val NEAR = 2.2f   // ...and right at it. 2.6 grows a tail on the eye
private const val SUNR = 0.28f      // discs that start inside it are red
private val TWIST = Degrees(40f)
private const val FALL = 0.3f

private class Tile(val at: Point, val r: Float, val red: Boolean)

private fun draw(c: Canvas, d: Dimension) {
    c.clear(paper)

    val cell = d.wf / N
    val sun = Point(sunx * d.wf, suny * d.hf)
    val tiles = (0 until N).flatMap { j ->
        (0 until N).map { i ->
            val v = sun.vecTo(Point((i + 0.5f) * cell, (j + 0.5f) * cell))
            val r = v.length()
            val q = r / (FALL * d.wf)
            val k = r / (swell * d.wf)
            Tile(sun.offset(v.rotate(TWIST.radians * exp(-q * q))), lerp(FAR, NEAR, exp(-k * k)) * cell, r < SUNR * d.wf)
        }
    }

    //every disc minus the ones lying on it, cut along their rims
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

private fun lies(u: Tile, t: Tile, sun: Point): Boolean {
    val a = sun.vecTo(u.at)
    val b = sun.vecTo(t.at)
    val h = cosf(SPIN) * ln(a.length() / b.length()) + sinf(SPIN) * wrapPi(atan2(a.y, a.x) - atan2(b.y, b.x))
    return if (abs(h) > 1e-6f) h < 0f else a.length() < b.length()
}

private fun disc(t: Tile): Path = PathBuilder().addCircle(t.at.x, t.at.y, t.r + RIM / 2f).detach()
