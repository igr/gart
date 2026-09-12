package work.vinculum

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.brush.BrushDabber
import dev.oblac.gart.brush.Brushes
import dev.oblac.gart.brush.Wobble
import dev.oblac.gart.brush.drawBrush
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.colorScale
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.color.unpremultiply
import dev.oblac.gart.gfx.squaredDistanceTo
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.GOLDEN_TURNf
import dev.oblac.gart.math.TAU
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.noiseOffset
import dev.oblac.gart.pixels.boxDownsample
import dev.oblac.gart.util.Stopwatch
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * VINCULUM
 *
 * corona's cousin from the conservative side of the family. same game - iterate a 2d map and
 * stare at where it lands - but this one keeps area, so nothing collapses onto an attractor.
 */
private const val W = 1200
private const val H = 1200

// knobs
private val OUT = ps("out", "vinculum")
private val SEED = pi("seed", 13)                 // the hero cast
private val SS = pi("ss", 3, 1..4)
private val K = pf("k", 0.95f, 0.3f..2.5f)        // kick. 0.9716 kills the golden curve. 0.97 left it
                                                  // one lucky thread, 0.95 keeps both bands alive
private val SCAN = pi("scan", 150, 20..600)       // starts up the middle column, evenly in p
private val RAND = pi("rand", 140, 0..800)
private val ITERS = pi("iters", 2600, 200..20000)
private val MARGIN = pf("margin", 104f, 40f..240f)

private val WIDTH = pf("width", 6f, 1f..14f)
private val INK = pf("ink", 1.6f, 0.2f..6f)       // how solid a line gets
private val DUST = pf("dust", 0.45f, 0f..1.5f)
private val TOOTH = pf("tooth", 0.55f, 0f..1f)
private val LYAP = pf("lyap", 0.02f, 0.001f..0.3f) // lyapunov gate, above this an orbit is sea
private val ACCENT = pf("accent", 1f, 0f..1f)
private val GOLD = pf("gold", 1f, 0f..1f)
private val SWIPES = pi("swipes", 5, 0..12)
private val RESIDUE = pf("residue", 0.55f, 0f..1.5f)
private val FRAME = pi("frame", 1, 0..1)

// chalks + board
private const val SLATE = 0xFF272B27.toInt()
private const val SLATE_LITE = 0xFF3A403A.toInt()

// Declaration order is the drawing order: white first, accents on top.
private enum class Chalk(val color: Int) {
    WHITE(0xFFF2EEE2.toInt()),
    GOLD(0xFFE7C27A.toInt()),
    RUST(0xFFD98B72.toInt())
}

private val RW = W * SS
private val RH = H * SS
private val NZ = noiseOffset(SEED.toLong())

private class Orbit(val xs: FloatArray, val ps: FloatArray, val lyap: Float, val omega: Float, val cover: Float, val extent: Float) {
    val chalk = chalkOf(this)
}

// p' = p + K/2pi sin(2pi x), x' = x + p'
private fun follow(x0: Double, p0: Double): Orbit {
    var x = x0
    var p = p0
    var du = 1.0
    var dv = 0.0
    var lsum = 0.0
    var osum = 0.0
    var bins = 0L              // 64 x-bins as a bitmask
    val xs = FloatArray(ITERS)
    val ps = FloatArray(ITERS)
    val kk = K / TAU
    var xlo = 1f
    var xhi = 0f
    var plo = 1f
    var phi = -1f
    for (i in 0 until ITERS) {
        val cc = K * cos(TAU * x)
        p += kk * sin(TAU * x)
        x += p
        dv += cc * du
        du += dv
        val n = sqrt(du * du + dv * dv)
        lsum += ln(n)
        du /= n
        dv /= n
        osum += p
        val xw = (x - floor(x)).toFloat()
        var pw = (p - floor(p)).toFloat()          // wrap p to [-0.5, 0.5)
        if (pw >= 0.5f) pw -= 1f
        xs[i] = xw
        ps[i] = pw
        if (xw < xlo) xlo = xw
        if (xw > xhi) xhi = xw
        if (pw < plo) plo = pw
        if (pw > phi) phi = pw
        bins = bins or (1L shl (xw * 64).toInt().coerceIn(0, 63))
    }
    return Orbit(
        xs, ps, (lsum / ITERS).toFloat(), (osum / ITERS).toFloat(),
        bins.countOneBits() / 64f, (xhi - xlo) + (phi - plo)
    )
}

private fun chalkOf(o: Orbit): Chalk {
    if (o.lyap > LYAP) return Chalk.WHITE
    val w = abs(o.omega)
    if (GOLD > 0f && o.cover > 0.72f && abs(w - GOLDEN_TURNf) < 0.013f) return Chalk.GOLD
    if (ACCENT > 0f && o.cover < 0.72f && abs(w - 1f / 3f) < 0.003f) return Chalk.RUST
    return Chalk.WHITE
}

private val STICK = Brushes.chalk
private val BAND = STICK.weight + 2f * STICK.scatter

private fun inkOf(color: Int, a: Float) = color.alpha((a.coerceIn(0f, 1f) * 255f + 0.5f).toInt())

private fun chalkOrbits(c: Canvas, orbits: List<Orbit>, rnd: Random) {
    val inset = MARGIN * SS
    val span = RW - 2 * inset
    val size = WIDTH * SS / BAND

    for ((o, orb) in orbits.withIndex().sortedBy { it.value.chalk }) {
        val sea = orb.lyap > LYAP
        val ink = orb.chalk.color
        val zc = o * 0.618f                        // per-orbit noise sheet so crossing curves dont share pressure

        val ppp = ITERS / (1.45f * orb.extent * span).coerceAtLeast(1f)
        val thin = min(1f, 2.6f / ppp)

        val stick = BrushDabber(c, STICK, inkOf(ink, thin * (if (sea) DUST else 1f)), size * (if (sea) 0.72f else 1f), rnd)
        val shed = if (sea) null else BrushDabber(c, STICK, inkOf(ink, thin * 0.2f), size, rnd)
        for (i in 0 until ITERS) {
            val bx = orb.xs[i]
            val by = 0.5f - orb.ps[i]

            val press = 0.7f + 0.3f * (0.5f + 0.5f * SimplexNoise.noise(bx * 3.1f + NZ, by * 3.1f + NZ, zc))
            val px = inset + bx * span
            val py = inset + by * span
            stick.dab(px, py, press, TAUf * hash01(o, i, 1, SEED))

            if (shed != null && hash01(o, i, 3, SEED) < 0.035f) {
                shed.dab(px, py + (3f + 13f * hash01(o, i, 4, SEED)) * SS, press * 0.84f)
            }
        }
    }
}

private fun chalkFrame(c: Canvas, rnd: Random) {
    val m = (MARGIN - 26f) * SS
    val corners = arrayOf(
        Point(m, m), Point(RW - m, m),
        Point(RW - m, RH - m), Point(m, RH - m)
    )
    val box = PathBuilder()
    for (side in 0 until 4) {
        val a = corners[side]
        val b = corners[(side + 1) % 4]

        val lead = -(2f + 6f * hash01(side, 5, SEED)) * SS
        val over = (5f + 9f * hash01(side, 6, SEED)) * SS
        val len = sqrt(a.squaredDistanceTo(b))
        val ux = (b.x - a.x) / len
        val uy = (b.y - a.y) / len
        box.moveTo(a.x + ux * lead, a.y + uy * lead)
        box.lineTo(a.x + ux * (len + over), a.y + uy * (len + over))
    }

    val bend = Wobble.curved(rnd, amount = 0.06f, scale = 0.004f / SS)
    c.drawBrush(box.detach(), STICK, Chalk.WHITE.color, WIDTH * SS / BAND * 0.85f, rnd, bend)
}

// the slate

private class Swipe(val x: Float, val y: Float, val ux: Float, val uy: Float, val half: Float, val w: Float, val str: Float) {
    fun at(fx: Float, fy: Float, si: Int): Float {
        val dx = fx - x
        val dy = fy - y
        val along = dx * ux + dy * uy
        val across = -dx * uy + dy * ux
        if (abs(along) > half + 80f || abs(across) > w) return 0f
        val past = (abs(along) - half).coerceAtLeast(0f)
        val fall = (1f - smoothstep(w * 0.45f, w, abs(across))) * (1f - smoothstep(0f, 80f, past))
        if (fall <= 0f) return 0f
        val streak = 0.5f + 0.5f * SimplexNoise.noise(across * 0.14f + NZ, along * 0.008f + NZ, si * 3.1f)
        return fall * (0.3f + 0.7f * streak) * str
    }
}

private fun planSwipes(rnd: Random) = List(SWIPES) {
    val ang = (rnd.nextFloat() - 0.5f) * 0.9f      // erasing arm goes mostly side to side
    Swipe(
        rnd.nextFloat() * W, rnd.nextFloat() * H,
        cos(ang), sin(ang),
        250f + 250f * rnd.nextFloat(), 55f + 65f * rnd.nextFloat(),
        0.5f + 0.5f * rnd.nextFloat()
    )
}

private fun composite(sheet: Gartvas, swipes: List<Swipe>): IntArray {
    val pixels = Gartmap(sheet).pixels
    for (y in 0 until RH) {
        val fy = (y + 0.5f) / SS
        for (x in 0 until RW) {
            val fx = (x + 0.5f) / SS
            val i = y * RW + x
            var col = slate(fx, fy)
            col = rubResidue(col, fx, fy, swipes)
            col = layChalk(col, pixels[i], fx, fy)
            pixels[i] = dimCorners(col, fx, fy)
        }
    }
    return pixels
}

private fun slate(fx: Float, fy: Float): Int {
    val wash = 0.5f + 0.5f * SimplexNoise.noise(fx * 0.0035f + NZ, fy * 0.0035f + NZ)
    val mot = 0.5f + 0.5f * SimplexNoise.noise(fx * 0.02f + NZ, fy * 0.02f + NZ, 7.7f)
    return lerpColor(SLATE, SLATE_LITE, (0.2f + 0.5f * wash * (0.6f + 0.4f * mot)).coerceIn(0f, 1f))
}

private fun rubResidue(col: Int, fx: Float, fy: Float, swipes: List<Swipe>): Int {
    var res = 0f
    for ((si, sw) in swipes.withIndex()) res += sw.at(fx, fy, si)
    return if (res > 0f) lerpColor(col, Chalk.WHITE.color, min(res, 1.4f) * 0.09f * RESIDUE) else col
}

private fun layChalk(col: Int, px: Int, fx: Float, fy: Float): Int {
    val ca = px ushr 24
    if (ca == 0) return col
    val tooth = 0.5f + 0.5f * SimplexNoise.noise(fx * 0.55f + NZ, fy * 0.55f + NZ, 3.3f)
    val mix = 1f - TOOTH * (1f - tooth)
    return lerpColor(col, unpremultiply(px), 1f - (1f - ca / 255f).pow(INK * mix))
}

// corner falloff, the room light not reaching
private fun dimCorners(col: Int, fx: Float, fy: Float): Int {
    val ex = (fx - W * 0.5f) / (W * 0.72f)
    val ey = (fy - H * 0.5f) / (W * 0.72f)
    return colorScale(col, 1f - 0.24f * smoothstep(0.55f, 1.25f, sqrt(ex * ex + ey * ey)))
}

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("vinculum", W, H)
    println(gart)

    val rnd = Random(SEED)
    val sw = Stopwatch()
    val orbits = ArrayList<Orbit>(SCAN + RAND)
    for (i in 0 until SCAN) {
        orbits += follow(0.497 + 0.006 * rnd.nextDouble(), -0.5 + (i + 0.5) / SCAN)
    }
    repeat(40) {
        val sgn = if (it % 2 == 0) 1.0 else -1.0
        val o = follow(0.48 + 0.04 * rnd.nextDouble(), sgn * (GOLDEN_TURNf + 0.016 * (rnd.nextDouble() - 0.5)))
        if (o.lyap <= LYAP) orbits += o
    }
    repeat(RAND) { orbits += follow(rnd.nextDouble(), rnd.nextDouble() - 0.5) }
    val sea = orbits.count { it.lyap > LYAP }
    val gold = orbits.count { it.chalk == Chalk.GOLD }
    val rust = orbits.count { it.chalk == Chalk.RUST }
    println("k=$K orbits=${orbits.size} sea=$sea gold=$gold rust=$rust in ${sw.lap()}ms")

    val sheet = Gartvas.of(RW, RH).also { it.canvas.clear(0) }
    val swipes = planSwipes(rnd)
    if (FRAME == 1) chalkFrame(sheet.canvas, rnd)
    chalkOrbits(sheet.canvas, orbits, rnd)
    val img = composite(sheet, swipes)
    println("chalked in ${sw.lap()}ms")

    val g = gart.gartvas()
    val map = Gartmap(g.d)
    boxDownsample(img, SS, map)
    map.drawToCanvas(g)

    gart.saveImage(g, "$OUT.png")
    if (!headless) gart.window().showImage(g)
}
