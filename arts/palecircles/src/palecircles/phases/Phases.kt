package palecircles.phases

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.Circle
import dev.oblac.gart.gfx.drawBorder
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.lerp
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * phases - all-circles, but every dot is a moon and the red disc is the sun.
 *
 * the side of a dot that faces the disc is lit (red), the far side is shadow (ink), and the
 * lit part shrinks with distance: full inside the sun, half on the rim, a sliver, then dark.
 * past dark the cycle keeps going and a few young moons come back on the far side, the way a
 * phase strip flips after the new moon. the disc itself is never drawn, you only see it
 * through the dots, same trick as all-circles.
 */
private val n = pi("n", 12)
private val sunx = pf("sunx", 0.66f)
private val suny = pf("suny", 0.30f)
internal val sunr = pf("sunr", 0.17f) // dvd sibling reads this one
private val fall = pf("fall", 0.7f)    // how far past the rim the light reaches, in canvas widths
private val gamma = pf("gamma", 0.7f)  // <1 thins the crescents early, >1 keeps them fat for longer
private val dark = pf("dark", 0.5f)    // where along the reach the moon goes fully dark
private val dead = pf("dead", 0.08f)   // how wide the fully dark band is, in the same units. thats the truly black dots
private val edge = pf("edge", 0.08f)   // the last sliver before the dark band and the first one after it. 0 fades to nothing and the band just looks wider
private val young = pf("young", 0.35f) // past dark a young moon comes back on the far side, this big on the farthest dot
private val youngpow = pf("youngpow", 1.3f) // >1 keeps the young moons to the last few dots
private val rnear = pf("rnear", 0.6f)  // dot radius over half a cell, at the small end of the ramp
private val rfar = pf("rfar", 1.05f)   // ...and at the big end. over 1 and they overlap
private val screen = pf("screen", 0f)  // halftone screen angle in degrees. 0 is upright like all-circles, 10 is a nice print
private val ramp = ps("ramp", "diag")  // what the dot size follows: diag = all-circles' diagonal, sun = distance from the sun
private val out = ps("out", "phases")

private val paper = RetroColors.white01
private val inkFill = fillOf(RetroColors.black01)
private val redFill = fillOf(RetroColors.red01)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("phases", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    draw(g.canvas, g.d, Circle(g.d.wf * sunx, g.d.hf * suny, g.d.wf * sunr), redFill)

    gart.saveImage(g, out.ensureExtension("png"))
    if (!headless) gart.window().showImage(g)
}

// the sun comes in from outside so PhasesDvd can bounce it around and swap its colour (lit).
// everything else stays put, the grid never moves
internal fun draw(c: Canvas, d: Dimension, sun: Circle, lit: Paint) {
    c.clear(paper)

    val reach = fall * d.wf
    val cell = d.wf / n

    // the screen. same parity as n so the dots land where all-circles puts them, but enough of
    // them to still cover the corners once its turned
    val m = n + 2 * ceil(n * 0.21f).toInt()
    val cs = cos(screen)
    val sn = sin(screen)
    val dots = (0 until m * m).map {
        val ux = (it % m - (m - 1) / 2f) * cell
        val uy = (it / m - (m - 1) / 2f) * cell
        Point(d.cx + ux * cs - uy * sn, d.cy + ux * sn + uy * cs)
    }.filter { it.x > -cell && it.x < d.wf + cell && it.y > -cell && it.y < d.hf + cell }

    class Moon(val ctr: Point, val r: Float, val a: Float, val p: Float, val dist: Float)

    // the young ramp runs from dark to the farthest dot on the page, so young is what the last one gets
    val tfar = ((dots.maxOf { hypot(sun.x - it.x, sun.y - it.y) } - sun.radius) / reach).coerceIn(0f, 1f).pow(gamma)

    val moons = dots.map { ctr ->
        val dx = sun.x - ctr.x
        val dy = sun.y - ctr.y
        val dist = hypot(dx, dy)

        // t is 0 at the rim and 1 where the light gives up
        val a = atan2(dy, dx)
        val t = ((dist - sun.radius) / reach).coerceIn(0f, 1f)
        val tr = if (ramp == "diag") (ctr.x + ctr.y) / (d.wf + d.hf) else t
        val r = lerp(rnear, rfar, tr.coerceIn(0f, 1f)) * cell / 2f

        // the rim is just a phase too: full deep inside the sun, half on the rim, gone one radius
        // past it. (a real clip of the disc looks the same, i checked, and this is one rule instead
        // of two. also tried a dark disc inside the sun for an eclipse - same rule inside out - but
        // two dark masses just muddled it, gone)
        val hard = (0.5f + 0.5f * (sun.radius - dist) / r).coerceIn(0f, 1f)

        // the glow takes over where the rim lets go: half, waning to a last sliver, a band of dark
        // ones, then the moon comes back young on the far side - negative phase, the lit bit flips
        val tg = t.pow(gamma)
        val d0 = dark - dead / 2f
        val d1 = dark + dead / 2f
        val soft = when {
            tg < d0 -> lerp(0.5f, edge, tg / d0)
            tg <= d1 -> 0f
            else -> -lerp(edge, young, (if (tfar > d1) (tg - d1) / (tfar - d1) else 1f).coerceIn(0f, 1f).pow(youngpow))
        }
        Moon(ctr, r, a, if (hard > 0f) maxOf(hard, soft) else soft, dist)
    }

    val farthest = moons.maxOf { it.dist }
    moons.sortedBy { if (it.p >= 0f) it.dist else 2 * farthest - it.dist }.forEach {
        if (it.p >= 0f) drawMoon(c, it.ctr, it.r, it.a, it.p, lit) else drawMoon(c, it.ctr, it.r, it.a + PI.toFloat(), -it.p, lit)
    }

    c.drawBorder(d, 40f, paper)
}

// lit fraction p: 1 full, 0.5 half, 0 new. lit side points at angle a (radians)
private fun drawMoon(c: Canvas, ctr: Point, r: Float, a: Float, p: Float, fill: Paint) {
    val cx = ctr.x
    val cy = ctr.y
    c.save()
    c.rotate(Math.toDegrees(a.toDouble()).toFloat(), cx, cy)
    c.drawCircle(cx, cy, r, inkFill)

    // the lit bit is the sun-side half disc, plus or minus half an ellipse. skia sweeps clockwise
    // with 0 at +x, so -90 is the top pole and we run down the sun side first
    val e = abs(2 * p - 1) * r
    val lit = PathBuilder()
    lit.moveTo(cx, cy - r)
    lit.arcTo(Rect(cx - r, cy - r, cx + r, cy + r), -90f, 180f, false)
    when {
        e < 0.5f -> lit.lineTo(cx, cy - r) // dead on half, a zero width oval makes skia sulk
        p > 0.5f -> lit.arcTo(Rect(cx - e, cy - r, cx + e, cy + r), 90f, 180f, false)  // gibbous, bulge away from the sun
        else -> lit.arcTo(Rect(cx - e, cy - r, cx + e, cy + r), 90f, -180f, false)     // crescent, the ellipse eats the half disc
    }
    lit.closePath()
    c.drawPath(lit.detach(), fill)
    c.restore()
}
