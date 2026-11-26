package numen

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.brush.Brushes
import dev.oblac.gart.brush.Watercolor
import dev.oblac.gart.brush.drawBrush
import dev.oblac.gart.brush.drawWatercolor
import dev.oblac.gart.color.BwColors
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.color.rgb
import dev.oblac.gart.fx.Fibres
import dev.oblac.gart.fx.Paper
import dev.oblac.gart.fx.PaperSurface
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.closedPathOf
import dev.oblac.gart.gfx.lerp
import dev.oblac.gart.gfx.plus
import dev.oblac.gart.gfx.pointBetween
import dev.oblac.gart.gfx.signedArea
import dev.oblac.gart.gfx.squaredDistanceTo
import dev.oblac.gart.gfx.times
import dev.oblac.gart.gfx.vecTo
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PrimeSieve
import dev.oblac.gart.math.TAU
import dev.oblac.gart.math.seededRandom
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Point
import org.jetbrains.skia.Surface
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * numen - a figure that the primes draw, in watercolor.
 *
 * the wheels. each prime p has one wheel. the radius of the wheel is 1/p, and the wheel makes p
 * turns in one round. the center of a wheel is on the rim of the wheel before it, and the pen is
 * on the rim of the last wheel. at the start all the wheels point up, so the line starts at the
 * top of the figure. after one round each wheel is at its start again, and the line closes.
 *
 * the loops. the line crosses itself. at each crossing I go from the line to its other branch.
 * this cuts the line into loops that do not cross each other. a spot is in as many loops as the
 * number of times the pen went around it.
 *
 * the paint. the sheet is black, so a wash adds light. each loop gets one wash, the largest loop
 * first. a wash is thin, so the washes below it stay visible, and a spot is brighter when the pen
 * went around it more times. the outer loops get the blue pigment. a loop that is in two other
 * loops or more gets the red pigment. then the pigment settles in the low spots of the paper. the
 * pencil line is the line of the pen.
 */

private const val W = 1400
private const val H = W
private val SEED = pi("seed", 7)
private val OUT = ps("out", "numen")

private val WHEELS = pi("wheels", 25, 1..400) // how many primes get a wheel
private val POWER = pf("power", 1f, 0.3f..2f) // wheel p has radius p^-power. below 1 the small wheels win
private val WARM = pi("warm", 3, 1..99)       // a loop this deep takes the red. 1 is all red, 99 all blue

private const val STEPS = 64   // pen stops in one turn of the fastest wheel
private const val FIT = 0.88f  // the figure
private const val COAT = 0.6f  // pigment in one wash. at 0.5 the low half of the hood went into the black
private const val LEAD = 1.6f
private const val GRAIN = 0.3f // 0.5 looked like felt meh
private const val CELL = 16    // the grid that finds the crossings

private val PIGMENTS = Palette(intArrayOf(NipponColors.col198_RURIKON, NipponColors.col037_SYOJYOHI))
private val PAPER = BwColors.warmBlack1 // black paper. warmBlack0 has more glow, but it looks like pure black
private val PENCIL = BwColors.bg04 // paper white was louder than the paint


private val WASH = Watercolor(opacity = COAT, bleed = 0.03f, texture = 0.7f, border = 0.6f, angle = Degrees(90f), scatter = false)


private val COLD_PRESS = Paper(
    laid = Fibres(0.5f, 0.6f, 0.64f, 611f),
    chain = Fibres(0.5f, 0.25f, 0.27f, 622f),
    felt = Fibres(0.2f, 0.013f, 0.014f, 901f),
    fibre = 0.9f,
    tooth = 1.2f,
)

private class Wheel(val turns: Int, val radius: Double)

// the 400th prime is 2741, so a sieve to 3000 has every wheel
private val TRAIN = PrimeSieve(3000)
    .let { sieve -> (2..sieve.limit).filter { sieve.isPrime(it) } }
    .take(WHEELS)
    .map { Wheel(it, it.toDouble().pow(-POWER.toDouble())) }

private fun trace(): List<Point> {
    val n = TRAIN.last().turns * STEPS
    val raw = List(n) { k ->
        val u = TAU * k / n
        Point(TRAIN.sumOf { it.radius * sin(it.turns * u) }, -TRAIN.sumOf { it.radius * cos(it.turns * u) })
    }
    val across = raw.maxOf { it.x } - raw.minOf { it.x }
    val top = raw.minOf { it.y }
    val tall = raw.maxOf { it.y } - top
    val scale = min(FIT * H / tall, 0.9f * W / across)

    val shift = Point(W / 2f, (H - tall * scale) / 2f - top * scale)
    return raw.map { it * scale + shift }
}

// the loops

private class Crossing(val at: Point, val first: Double, val again: Double)

// where the steps i and j cross
private fun cut(line: List<Point>, i: Int, j: Int): Crossing? {
    val a = line[i]
    val b = line[(i + 1) % line.size]
    val c = line[j]
    val d = line[(j + 1) % line.size]
    val ab = a.vecTo(b)
    val cd = c.vecTo(d)
    val den = ab.cross(cd)
    if (den == 0f) return null
    val ac = a.vecTo(c)
    val t = ac.cross(cd) / den // how far along the step i
    val s = ac.cross(ab) / den // and along the step j
    return if (t >= 0f && t < 1f && s >= 0f && s < 1f) Crossing(lerp(a, b, t), i + t.toDouble(), j + s.toDouble()) else null
}

private fun crossings(line: List<Point>): List<Crossing> {
    val n = line.size
    val cols = W / CELL + 1
    val grid = Array(cols * (H / CELL + 1)) { ArrayList<Int>() }
    fun cellOf(p: Point) = (p.y / CELL).toInt() * cols + (p.x / CELL).toInt()

    // a step is in each cell that its box touches
    for (i in 0 until n) {
        val a = line[i]
        val b = line[(i + 1) % n]
        for (cy in (min(a.y, b.y) / CELL).toInt()..(max(a.y, b.y) / CELL).toInt()) {
            for (cx in (min(a.x, b.x) / CELL).toInt()..(max(a.x, b.x) / CELL).toInt()) grid[cy * cols + cx] += i
        }
    }
    val found = ArrayList<Crossing>()
    for ((here, steps) in grid.withIndex()) {
        for (i in steps) for (j in steps) {
            if (j <= i + 1 || (i == 0 && j == n - 1)) continue // two steps with a common end do not cross
            val crossing = cut(line, i, j) ?: continue
            // two steps can be together in two cells. the crossing counts only in its own cell
            if (cellOf(crossing.at) == here) found += crossing
        }
    }
    return found
}

// one pass of the pen through a crossing
private class Pass(val place: Double, val crossing: Int)

private fun loops(line: List<Point>, crossings: List<Crossing>): List<List<Point>> {
    if (crossings.isEmpty()) return listOf(line)
    val n = line.size
    val passes = crossings.flatMapIndexed { k, c -> listOf(Pass(c.first, k), Pass(c.again, k)) }.sortedBy { it.place }

    // the other pass of the same crossing
    val twin = IntArray(passes.size)
    val earlier = HashMap<Int, Int>()
    passes.forEachIndexed { e, pass ->
        val before = earlier.put(pass.crossing, e) ?: return@forEachIndexed
        twin[e] = before
        twin[before] = e
    }

    val used = BooleanArray(passes.size)
    val out = ArrayList<List<Point>>()
    for (start in passes.indices) {
        if (used[start]) continue
        val loop = ArrayList<Point>()
        var e = start
        do {
            used[e] = true
            val next = (e + 1) % passes.size
            loop += crossings[passes[e].crossing].at
            // the points of the line between this pass and the next pass. the last pass goes on to the first
            val from = passes[e].place.toInt() + 1
            val to = passes[next].place.toInt() + if (next == 0) n else 0
            for (k in from..to) loop += line[k % n]
            e = twin[next]
        } while (e != start)
        out += loop
    }
    return out
}

// a ray from p to the right crosses the rim an odd number of times when p is in the loop
private fun holds(loop: List<Point>, p: Point): Boolean {
    var inside = false
    for ((a, b) in (loop + loop.first()).zipWithNext()) {
        if ((a.y > p.y) != (b.y > p.y) && p.x < a.x + (p.y - a.y) / (b.y - a.y) * (b.x - a.x)) inside = !inside
    }
    return inside
}

// how many loops a loop is in, itself included..
private fun depths(loops: List<List<Point>>): List<Int> = loops.map { loop ->
    val (a, b) = (loop + loop.first()).zipWithNext().maxBy { (a, b) -> a.squaredDistanceTo(b) }
    val probe = pointBetween(a, b)
    1 + loops.count { it !== loop && holds(it, probe) }
}

// the paint

private fun wash(c: Canvas, loop: List<Point>, pigment: Int, rnd: Random) {
    if (abs(signedArea(loop)) < 3f) return // a sliver between two crossings
    closedPathOf(loop).use { c.drawWatercolor(it, pigment, WASH, rnd, BlendMode.SCREEN) }
}

// the pigment settles in the low spots of the sheet and thins on the high spots
private fun settle(g: Gartvas) {
    val sheet = PaperSurface(SEED, COLD_PRESS)
    fun settled(v: Int, bare: Int, e: Float) = (255 - (255 - bare) * ((255 - v).toFloat() / (255 - bare)).coerceAtMost(1f).pow(e)).roundToInt()
    Gartmap(g).use { m ->
        for (y in 0 until H) for (x in 0 until W) {
            val px = m.pixels[y * W + x]
            val e = 1f - GRAIN * sheet.heightAt(x, y)
            m.pixels[y * W + x] = rgb(settled(red(px), red(PAPER), e), settled(green(px), green(PAPER), e), settled(blue(px), blue(PAPER), e))
        }
        m.drawToCanvas()
    }
}

fun main(args: Array<String>) {
    val gart = Gart.of("numen", W, H)
    val line = trace()
    val loops = loops(line, crossings(line)).sortedByDescending { abs(signedArea(it)) }
    val depth = depths(loops)
    println("$gart ${TRAIN.size} wheels, ${line.size} steps, ${loops.size} loops, ${depth.max()} deep")

    // a wash is 20 thin coats. an 8 bit sheet rounds them off: the hood went violet and the face magenta
    val sheet = Surface.makeRaster(ImageInfo(W, H, ColorType.RGBA_F16, ColorAlphaType.PREMUL))
    sheet.canvas.clear(PAPER)
    val rnd = seededRandom(SEED)
    loops.forEachIndexed { k, loop -> wash(sheet.canvas, loop, PIGMENTS[if (depth[k] < WARM) 0 else 1], rnd) }

    val g = gart.gartvas()
    g.canvas.drawImage(sheet.makeImageSnapshot(), 0f, 0f)
    settle(g)
    g.canvas.drawBrush(line + line.first(), Brushes.pencil2B, PENCIL, LEAD, rnd, blend = BlendMode.SCREEN)

    gart.saveImage(g, OUT.ensureExtension())
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
