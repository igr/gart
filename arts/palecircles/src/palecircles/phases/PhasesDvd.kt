package palecircles.phases

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.Circle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.lerp
import kotlin.math.roundToInt

/**
 * phases-dvd - phases, but the sun bounces round the page like the dvd logo.
 */
private val frames = pi("frames", 300) // frames per loop, at fps. 300 at 25 is 12s
private val fps = pi("fps", 25)        // 25 is an exact 4cs gif delay, 30 rounds to 3cs and runs fast
private val kx = pi("kx", 3)           // round trips across, per loop...
private val ky = pi("ky", 2)           // ...and down. coprime, or the path folds onto itself
private val phx = pf("phx", 0.6f)      // where it starts, in half trips: 0 is the left wall heading right, 1 the right wall heading back
private val phy = pf("phy", 0.4f)      // same, down. 0.6/0.4 lands both corners on whole frames (120 and 270)
private val wall = pf("wall", 40f)     // the rim bounces this far in from the edge. 40 is the paper border
private val ncol = pi("ncol", 4)       // colours in the cycle. 1 is the plain red bounce
private val out = ps("out", "phases-dvd")

// the dvd colours, retro edition. red first so it starts as phases
private val suns = Palette.of(RetroColors.red01, RetroColors.blue01, RetroColors.amber01, RetroColors.green01)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("phases-dvd", 1024, 1024, fps)
    println(gart)
    val d = gart.d

    val path = plot(d)
    val hits = path.count { it.hit }
    println("$hits wall hits per loop" + if (hits % ncol == 0) ", loop closes" else " - loop wont close, ${hits % ncol} colours off each time round")

    val fills = List(ncol) { fillOf(suns[it % suns.size]) }
    val g = gart.gartvas()
    val m = gart.movieGif(name = out.ensureExtension("gif"))
    path.forEachIndexed { i, b ->
        draw(g.canvas, d, b.sun, fills[b.col % ncol])
        m.addFrame(g)
        print("frame $i/$frames\r")
    }
    gart.saveMovie(m, fps)

    if (!headless) gart.window().show { c, _, f -> c.drawImage(m[(f.frame % frames).toInt()], 0f, 0f) }
}

private class Bounce(val sun: Circle, val col: Int, val hit: Boolean)

private fun plot(d: Dimension): List<Bounce> {
    val r = d.wf * sunr
    val x0 = wall + r
    val x1 = d.wf - wall - r
    val y0 = wall + r
    val y1 = d.hf - wall - r
    val px = (phx * frames).roundToInt()
    val py = (phy * frames).roundToInt()

    fun ticks(f: Int, k: Int, ph: Int) = 2 * k * f + ph
    fun tri(t: Int): Float { val v = Math.floorMod(t, 2 * frames); return (if (v < frames) v else 2 * frames - v) / frames.toFloat() }
    fun hit(f: Int, k: Int, ph: Int) = Math.floorDiv(ticks(f, k, ph), frames) > Math.floorDiv(ticks(f - 1, k, ph), frames)

    var col = 0 // hits so far. a hit on frame 0 counts as the wrap, so frame 0 is always red
    return List(frames) { f ->
        val hit = hit(f, kx, px) || hit(f, ky, py)
        if (hit && f > 0) col++
        Bounce(Circle(lerp(x0, x1, tri(ticks(f, kx, px))), lerp(y0, y1, tri(ticks(f, ky, py))), r), col, hit)
    }
}
