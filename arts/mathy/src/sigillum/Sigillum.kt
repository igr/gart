package sigillum

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.BwColors
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.lerpColorInLight as mix
import dev.oblac.gart.fx.PaperSurface
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.GaussianInt
import dev.oblac.gart.math.PrimeSieve
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.between
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.seededRandom
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.noiseOffset
import dev.oblac.gart.util.Stopwatch
import dev.oblac.gart.util.parallelForRows
import dev.oblac.gart.vector.Vec2
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * sigillum - one seal on a square sheet. the primes decide the shape of the seal.
 *
 * the numbers. a gaussian integer is a + bi, where a and b are whole numbers. these numbers make a
 * square grid in the complex plane. a gaussian prime is not a unit (1, i, -1 or -i), and it is not
 * a product of two gaussian integers unless one of them is a unit. off the axes, a + bi is a prime
 * when its norm a² + b² is an ordinary prime. on an axis, the part that is not 0 must be a prime of
 * the form 4k + 3. for example, 3 and 2 + i are gaussian primes. 5 is not, because 5 = (2 + i)(2 - i).
 *
 * the walk. a walker has a spot z and a heading d. the heading is 1, i, -1 or -i: east, north, west
 * or south. each step adds the heading to the spot. when the walker lands on a gaussian prime, it
 * multiplies the heading by i. this is a quarter turn to the left, because (a + bi) i = -b + ai.
 * the walk starts with the heading east. it stops when it is at its start again with the heading east.
 *
 * each step can be undone. if the spot is a prime, the old heading was d times -i. the old spot was
 * the spot minus the old heading. so two different states never go to the same next state, and a
 * walk cannot enter a loop that does not contain its start. it returns, or it never repeats a state.
 * as far as I know, nobody has proved that every walk returns. some walks do not return in millions
 * of steps, so I drop a walk that does not return in [STEPS] steps. each closed walk is one loop,
 * and its corners are the primes where it turned.
 *
 * the glyph. I keep only the loops that have a mirror line, because they look like characters. the
 * others look like scribbles. I turn each loop so that its mirror line is upright. the corners give
 * a set of x values and a set of y values. each x value becomes a column and each y value becomes a
 * row, with equal spacing. a carver uses the same method to fit folded seal script into a square stone.
 *
 * the seed picks the loop. seed 1 is the first loop, seed 2 is the next one, and so on. after the
 * last loop, the count starts again. on a white seal, the carver cuts the lines into the stone. the
 * lines print white in a red block. on a red seal, the carver removes the stone around the lines and
 * keeps a frame. the lines print red. the seed also picks the cut and the paste.
 *
 * I print with cinnabar paste on laid paper. the paste gets to the paper where the fibers are high
 * and where the hand pushes hard. as a result, a light press gives a speckled print. old stones have
 * chipped edges. the oil in the paste makes a faint stain around the print.
 */

private const val W = 1400
private const val H = W
private val SEED = pi("seed", 13)
private val OUT = ps("out", "sigillum")

private val PRESS = pf("press", 0.62f, 0f..1f)
private val CHIPS = pf("chips", 0.5f, 0f..1f) // how broken the stone is

private const val NORM = 4_000_000 // sieve the primes up to this norm
private const val REACH = 400
private const val STEPS = 200_000 // drop a longer walk too. some walks never seem to return?

private val PASTES = Palette(intArrayOf(NipponColors.col029_GINSYU, NipponColors.col025_SHINSYU, NipponColors.col042_AKE))
private val PAPER = BwColors.parchment
private val OIL = NipponColors.col105_TORINOKO

// the primes

private val PRIMES = PrimeSieve(NORM)
private val EAST = GaussianInt.ONE

// a start spot in the box, as an index into done
private fun cell(z: GaussianInt) = (z.im + REACH) * (2 * REACH + 1) + z.re + REACH

// the walk goes east one step at a time and turns left on every prime. it gives the corners, or null
// if it goes too far. each spot in the box that it passes while it faces east is a start on this same
// walk, so I mark that spot as done
private fun walk(start: GaussianInt, done: BooleanArray): List<GaussianInt>? {
    var z = start
    var d = EAST
    val corners = ArrayList<GaussianInt>()
    repeat(STEPS) {
        z += d
        if (z.norm() > NORM) return null
        if (z.isPrime(PRIMES)) {
            d *= GaussianInt.I // times i is a left turn
            corners += z
        }
        if (d == EAST && abs(z.re) <= REACH && abs(z.im) <= REACH) done[cell(z)] = true
        if (z == start && d == EAST) return corners
    }
    return null
}

// the corners, moved so that the lowest x and the lowest y are 0
private fun atOrigin(c: List<GaussianInt>): Set<GaussianInt> {
    val corner = GaussianInt(c.minOf { it.re }, c.minOf { it.im })
    return c.mapTo(HashSet()) { it - corner }
}

// a loop turned or flipped is the same shape, so there are 8 ways to lay it down.
// times i is a quarter turn, the conjugate is a flip
private fun layouts(c: List<GaussianInt>): List<Set<GaussianInt>> {
    val turns = listOf(GaussianInt.ONE, GaussianInt.I, -GaussianInt.ONE, -GaussianInt.I)
    return turns.flatMap { t -> listOf(atOrigin(c.map { it * t }), atOrigin(c.map { it.conjugate() * t })) }
}

// the glyph
// a loop on an even grid: the distinct x and y of its corners become columns and rows, one apart
private class Glyph(c: List<GaussianInt>) {
    val cols: Int
    val rows: Int
    val across: Array<List<IntRange>> // per row: the line runs, as column ranges
    val down: Array<List<IntRange>> // per column: the line runs, as row ranges
    val lines get() = max(cols, rows)

    init {
        val xs = c.map { it.re }.distinct().sorted()
        val ys = c.map { it.im }.distinct().sorted()
        cols = xs.size
        rows = ys.size
        val col = xs.withIndex().associate { it.value to it.index }

        // the imaginary axis points up, but the image has y down
        val row = ys.withIndex().associate { it.value to rows - 1 - it.index }
        val h = Array(rows) { ArrayList<IntRange>() }
        val v = Array(cols) { ArrayList<IntRange>() }
        // each corner to the next one, and the last one back to the first
        for ((a, b) in (c + c.first()).zipWithNext()) {
            val i0 = col.getValue(a.re)
            val j0 = row.getValue(a.im)
            val i1 = col.getValue(b.re)
            val j1 = row.getValue(b.im)
            if (j0 == j1) h[j0] += min(i0, i1)..max(i0, i1)
            else v[i0] += min(j0, j1)..max(j0, j1)
        }
        across = Array(rows) { merge(h[it]) }
        down = Array(cols) { merge(v[it]) }
    }

    // runs that overlap or touch become one run
    private fun merge(runs: List<IntRange>): List<IntRange> {
        val out = ArrayList<IntRange>()
        for (r in runs.sortedBy { it.first }) {
            val last = out.lastOrNull()
            if (last != null && r.first <= last.last) out[out.lastIndex] = last.first..max(last.last, r.last)
            else out += r
        }
        return out
    }

    // the distance in px from (u, v) to the nearest line, on grid steps of sx by sy. it is never more than reach
    fun dist(u: Float, v: Float, sx: Float, sy: Float, reach: Float): Float {
        var best = reach
        val jFrom = max(0, floor((v - reach) / sy).toInt())
        val jTo = min(rows - 1, ceil((v + reach) / sy).toInt())
        for (j in jFrom..jTo) {
            val dy = abs(v - j * sy)
            if (dy >= best) continue
            for (run in across[j]) best = min(best, cap(outside(u, run.first * sx, run.last * sx), dy))
        }
        val iFrom = max(0, floor((u - reach) / sx).toInt())
        val iTo = min(cols - 1, ceil((u + reach) / sx).toInt())
        for (i in iFrom..iTo) {
            val dx = abs(u - i * sx)
            if (dx >= best) continue
            for (run in down[i]) best = min(best, cap(dx, outside(v, run.first * sy, run.last * sy)))
        }
        return best
    }
}

// how far p is outside from..to. it is 0 inside
private fun outside(p: Float, from: Float, to: Float) = max(max(from - p, p - to), 0f)

// a soft square around a line end. round caps looked stamped by a machine, square ones by a printer
private fun cap(dx: Float, dy: Float): Float {
    if (dx == 0f) return dy
    if (dy == 0f) return dx
    val a = dx * dx
    val b = dy * dy
    return sqrt(sqrt(a * a + b * b))
}

// the loop with its mirror line upright, or null if it has none. the line sits halfway between
// the lowest and the highest x, so a mirror takes x to flipX - x
private fun upright(c: List<GaussianInt>): List<GaussianInt>? {
    val pts = c.toHashSet()
    val flipX = c.minOf { it.re } + c.maxOf { it.re }
    val flipY = c.minOf { it.im } + c.maxOf { it.im }
    if (c.all { GaussianInt(flipX - it.re, it.im) in pts }) return c
    if (c.all { GaussianInt(it.re, flipY - it.im) in pts }) return c.map { GaussianInt(it.im, it.re) }
    return null
}

// the loops that a carver can cut. small ones look like a box of boxes, huge ones look gray
private fun glyphs(): List<Glyph> {
    val seen = HashSet<Set<GaussianInt>>() // every way to lay down every shape so far
    val done = BooleanArray((2 * REACH + 1) * (2 * REACH + 1))
    val out = ArrayList<Glyph>()
    for (y in -REACH..REACH) for (x in -REACH..REACH) {
        val z = GaussianInt(x, y)
        if (done[cell(z)] || z.isPrime(PRIMES)) continue
        val c = walk(z, done) ?: continue
        if (atOrigin(c) in seen) continue
        seen += layouts(c)

        val g = Glyph(upright(c) ?: continue)
        if (g.lines in 9..72) out += g
    }
    return out
}

// the stone

private fun noise(x: Float, y: Float) = SimplexNoise.noise(x, y)

private val NZ = noiseOffset(SEED.toLong())

// three fiber directions, about 60 deg apart
private val FIBERS = List(3) { Vec2(cos(0.5f + it * 1.05f), sin(0.5f + it * 1.05f)) }

// the pulp under the seal: fibers in all directions, pores and the tooth. the laid lines of the sheet
// made the print look like canvas, so the paste does not use them
private fun pulp(x: Int, y: Int): Float {
    var f = 0f
    FIBERS.forEachIndexed { k, d -> f += noise((x * d.x + y * d.y) * 0.06f + k * 91f + NZ, (y * d.x - x * d.y) * 0.55f) }
    return 0.1f * f + 0.08f * noise(x * 0.3f + NZ, y * 0.3f + 13f) + 0.22f * (hash01(x, y, SEED) - 0.5f)
}

// how much of the paste reaches the paper, for a press q
private fun contact(rise: Float, q: Float) = smoothstep(0f, 1f, (rise + 0.9f * (q - 0.3f)) / 0.06f)

// a chip off the stone edge, in stone coordinates
private class Bite(val x: Float, val y: Float, val r: Float)

private class Seal(
    val glyph: Glyph,
    val white: Boolean,
    val cx: Float, val cy: Float,
    val w: Float,
    val h: Float,
    rnd: Random,
) {
    val paste = PASTES[rnd.nextInt(PASTES.size)]
    private val turn = rnd.between(-0.018f, 0.018f) // about a degree
    private val ct = cos(turn)
    private val st = sin(turn)

    // the margin and the frame, in steps between two lines. a big stone with few lines gets fat lines
    private val rim = if (white) 0f else rnd.between(0.42f, 0.62f)
    private val margin = rim + (if (white) rnd.between(0.95f, 1.25f) else rnd.between(0.7f, 1.0f))
    private val step = if (glyph.cols >= glyph.rows) w / (glyph.cols - 1 + 2f * margin) else h / (glyph.rows - 1 + 2f * margin)
    private val frame = rim * step
    private val pad = margin * step
    private val sx = (w - 2f * pad) / (glyph.cols - 1)
    private val sy = (h - 2f * pad) / (glyph.rows - 1)
    // the carver cuts white lines fat, with thin red walls between them. red lines are thinner
    private val half = min(sx, sy) * (if (white) rnd.between(0.25f, 0.3f) else rnd.between(0.2f, 0.25f))
    private val press = PRESS + rnd.between(-0.08f, 0.08f)
    private val tilt = rnd.between(0.08f, 0.26f)
    private val lean = rnd.between(0f, TAUf) // the way the hand leans
    private val leanX = cos(lean)
    private val leanY = sin(lean)
    // one or two chips near a corner. the center sits on the edge or just out of it: a bite inside the
    // stone printed as a round hole in the block, like a bubble
    private val bites = List(rnd.between(0, 2)) {
        val r = CHIPS * max(w, h) * rnd.between(0.01f, 0.035f)
        val ex = if (rnd.nextBoolean()) -0.5f else 0.5f
        val ey = if (rnd.nextBoolean()) -0.5f else 0.5f
        val along = rnd.between(0f, 0.25f)
        val out = rnd.between(0f, 0.6f) * r
        if (rnd.nextBoolean()) Bite(ex * w + 2f * ex * out, ey * h * (1f - 2f * along), r)
        else Bite(ex * w * (1f - 2f * along), ey * h + 2f * ey * out, r)
    }

    // signed px to the edge of the paste, negative where the stone face meets the paper
    fun sd(x: Float, y: Float): Float {
        val lx = (x - cx) * ct + (y - cy) * st
        val ly = (y - cy) * ct - (x - cx) * st
        var box = max(abs(lx) - w / 2f, abs(ly) - h / 2f)
        if (box > 12f) return box
        box += 0.5f * noise(lx * 0.045f + NZ, ly * 0.045f) // a stone edge is never ruled
        box += CHIPS * 9f * max(0f, noise(lx * 0.028f + NZ, ly * 0.028f + 50f) - 0.5f)
        for (b in bites) {
            // a clean round bite looked like a ticket punch
            val ragged = b.r * (1f + 0.35f * noise(lx * 0.06f + NZ, ly * 0.06f - 40f))
            box = max(box, ragged - sqrt((lx - b.x) * (lx - b.x) + (ly - b.y) * (ly - b.y)))
        }
        val wobble = half * 0.16f * noise(lx * 0.11f + NZ, ly * 0.11f - 20f)
        val stroke = glyph.dist(lx + w / 2f - pad, ly + h / 2f - pad, sx, sy, half + 12f) - half + wobble
        return if (white) max(box, -stroke) else max(box, min(stroke, -box - frame))
    }

    // the paste that reaches the paper, 0..1. rise is the paper height
    fun ink(sd: Float, x: Float, y: Float, rise: Float): Float {
        val cover = (0.5f - sd).coerceIn(0f, 1f)
        if (cover <= 0f) return 0f
        val lx = (x - cx) * ct + (y - cy) * st
        val ly = (y - cy) * ct - (x - cx) * st
        // the hand leans to one side. the paste is never even. the stone face has its own low spots
        val q = press + tilt * (leanX * lx + leanY * ly) / max(w, h) + 0.08f * noise(lx * 0.01f + NZ, ly * 0.01f + 9f)
        val face = 0.06f * noise(lx * 0.05f + NZ, ly * 0.05f + 31f)
        return cover * contact(rise + face, q)
    }
}

// the sheet

// seed n is loop n. past the last loop it starts again, and the cut and the paste come out new
private fun stamp(pool: List<Glyph>): Seal {
    val rnd = seededRandom(SEED)
    val g = pool[(SEED - 1).mod(pool.size)]
    val size = W * rnd.between(0.56f, 0.64f)

    // most loops with a mirror line are twice as wide as tall. on a square sheet a square stone looks best
    val aspect = ((g.cols + 1.6f) / (g.rows + 1.6f)).coerceIn(0.92f, 1.08f)
    val w = if (aspect >= 1f) size else size * aspect
    val h = if (aspect >= 1f) size / aspect else size
    // a hair above the middle. dead center looks like it slides down
    val cx = W / 2f + rnd.between(-0.01f, 0.01f) * W
    val cy = H * 0.48f + rnd.between(-0.01f, 0.01f) * H
    return Seal(g, rnd.nextBoolean(), cx, cy, w, h, rnd)
}

// colors mix in light, the way specks of paste mix when you step back
private fun shade(x: Int, y: Int, paper: PaperSurface, s: Seal): Int {
    val rise = paper.heightAt(x, y)
    val fx = x + 0.5f
    val fy = y + 0.5f
    // the sheet itself: the fibers catch a little light
    val col = mix(PAPER, if (rise > 0f) BwColors.paperWhite else BwColors.wildSand, (abs(rise) * 0.5f).coerceAtMost(1f))
    val sd = s.sd(fx, fy)
    if (sd > 10f) return col
    // the oil in the paste spreads a little into the sheet
    val oiled = mix(col, OIL, 0.3f * exp(-max(sd, 0f) / 3f))
    val k = s.ink(sd, fx, fy, pulp(x, y))
    if (k <= 0f) return oiled
    // the press pushes the paste to the edge of a line, so it is deeper there
    return mix(oiled, mix(s.paste, BwColors.warmBlack2, 0.22f * exp(min(sd, 0f) / 1.6f)), k)
}

fun main(args: Array<String>) {
    val gart = Gart.of("sigillum", W, H)
    val sw = Stopwatch()
    val pool = glyphs()
    println("sigillum: ${pool.size} loops in ${sw.lap()}ms")
    val seal = stamp(pool)
    val paper = PaperSurface(SEED)
    val g = gart.gartvas()
    Gartmap(g).use { m ->
        parallelForRows(H) { y0, y1 ->
            for (y in y0 until y1) for (x in 0 until W) m.pixels[y * W + x] = shade(x, y, paper, seal)
        }
        m.drawToCanvas()
    }
    gart.saveImage(g, OUT.ensureExtension())
    val n = (SEED - 1).mod(pool.size) + 1
    println("sigillum: seed=$SEED, loop $n of ${pool.size} (${seal.glyph.cols}x${seal.glyph.rows}), ${if (seal.white) "white" else "red"}, ${sw.ms}ms")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
