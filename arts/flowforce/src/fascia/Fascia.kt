package fascia

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.lumOf
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.flow2.Streamline
import dev.oblac.gart.flow2.VectorField
import dev.oblac.gart.flow2.plus
import dev.oblac.gart.flow2.streamlines
import dev.oblac.gart.flow2.times
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.between
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.length
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.util.Stopwatch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * fascia - fat bands of flat ink along a flow. most bands become grain along one part.
 *
 * the bands are streamlines of a flow: a few big whirls over a slow straight drift. the lines
 * are far apart, but the bands are fat, so neighbors almost touch. where the flow squeezes the
 * lines, the bands overlap.
 *
 * each band has one ink. a fade takes the band into the paper or into a second ink. the fade
 * does not blend colors. each pixel gets one ink or the other, and the fade only changes how
 * many pixels get each. in a fade, a dither threshold picks the ink: droplets on a jittered grid,
 * mixed with single pixels of noise. as a result, the dots clump like spray paint, not a stipple.
 */

private const val W = 1414
private const val H = 2000
private val SEED = pi("seed", 1)
private val OUT = ps("out", "fascia")

// the flow
private val WHIRLS = pi("whirls", 4, 1..8)
private val DRIFT = pf("drift", 0.35f, 0f..2f)
private val DSEP = pf("dsep", 0.11f, 0.03f..0.25f) // band spacing
private val FAT = pf("fat", 0.88f, 0.3f..1.5f)

private val FIVES = listOf(
    2, 79, 83, 85, 94, 105, 110, 115, 125, 127, 141, 155, 171, 173,
    201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214,
    218, 221, 230, 235, 236, 252, // off the colour sweep
)
private val PAL = pi("pal", 1, 1..FIVES.size) // 1 is cool2

private val BLACK = 0xFF171516.toInt()
private val CREAM = 0xFFF7EBDA.toInt()
private val pal = Palettes.coolPalette(FIVES[PAL - 1])
// the palettes lightest entry is the paper when it is pale enough to be one, else cream
private val PAPER = pal.lightest().let { if (lumOf(it) >= 200f) it else CREAM }
// black always goes in. a palette near-black would only be a second black, so it stays out
private val inks = Palette.of(listOf(BLACK) + pal.toIntArray().filter { it != PAPER && lumOf(it) >= 30f })
private const val NONE = 0


// the flow

private fun flow(rnd: Random): VectorField {
    val tilt = PIf / 2f + rnd.between(-0.35f, 0.35f)
    var f = VectorField.angles { _, _ -> tilt } * DRIFT
    repeat(WHIRLS) {
        val cx = W * rnd.between(-0.3f, 1.3f)
        val cy = H * rnd.between(-0.2f, 1.2f)
        val spin = (if (rnd.nextBoolean()) 1f else -1f) * rnd.between(0.7f, 1.3f)
        f += VectorField.vortex(cx, cy, spin = spin, pull = rnd.between(-0.1f, 0.25f), reach = W * rnd.between(0.25f, 0.6f))
    }
    return f
}

// ---- bands ----

// a piece of a streamline: its spine, how far along it each point is, and the inks
private class Band(
    val xs: FloatArray,
    val ys: FloatArray,
    val arc: FloatArray,
    val ink: Int,
    val to: Int,
    val fades: Boolean,
    val f0: Float,
    val f1: Float,
) {
    // the ink at s along the spine
    fun inkAt(s: Float, x: Int, y: Int): Int {
        val f = if (fades) smoothstep(f0, f1, s) else 0f
        return when {
            f <= 0f -> ink
            f >= 1f || f > thr(x, y) -> to
            else -> ink
        }
    }
}

private const val CUTS = 3

// breaks a streamline into a few bands with gaps between
private fun cut(line: Streamline, w: Float, rnd: Random): List<Band> {
    val pts = line.points
    val xs = FloatArray(pts.size) { pts[it].x }
    val ys = FloatArray(pts.size) { pts[it].y }
    val arc = FloatArray(pts.size)
    for (i in 1 until pts.size) {
        val dx = pts[i].x - pts[i - 1].x
        val dy = pts[i].y - pts[i - 1].y
        arc[i] = arc[i - 1] + length(dx, dy)
    }
    val total = arc.last()
    val marks = List(rnd.nextInt(CUTS)) { rnd.nextFloat() * total }.sorted()
    val gaps = List(marks.size) { rnd.between(0f, 1.2f * w) }

    val out = mutableListOf<Band>()
    var a = 0f
    for (k in 0..marks.size) {
        val b = if (k < marks.size) marks[k] - gaps[k] / 2f else total
        if (b - a > 0.6f * w) out += band(xs, ys, arc, a, b, w, rnd)
        if (k < marks.size) a = marks[k] + gaps[k] / 2f
    }
    return out
}

private const val FADE = 0.75f
private const val GHOST = 0.55f

private fun band(px: FloatArray, py: FloatArray, arc: FloatArray, a: Float, b: Float, w: Float, rnd: Random): Band {
    // every 5th point is for a spine
    val keep = arc.indices.filter { arc[it] in a..b }.filterIndexed { i, _ -> i % 5 == 0 }.toMutableList()
    val lastIn = arc.indices.last { arc[it] <= b }
    if (keep.last() != lastIn) keep += lastIn
    val xs = FloatArray(keep.size) { px[keep[it]] }
    val ys = FloatArray(keep.size) { py[keep[it]] }
    val s = FloatArray(keep.size) { arc[keep[it]] - a }
    val len = s.last()

    val ink = inks.random(rnd)
    val fades = rnd.nextFloat() < FADE
    val others = inks.filter { it != ink }
    val to = if (rnd.nextFloat() < GHOST || others.size == 0) NONE else others.random(rnd)

    // the fade sits anywhere along the band and runs either way
    val mid = len * rnd.between(0.2f, 0.8f)
    val half = min(w * rnd.between(0.5f, 3f), 0.45f * len)
    val flip = rnd.nextBoolean()
    return Band(xs, ys, s, ink, to, fades,
        if (flip) mid + half else mid - half,
        if (flip) mid - half else mid + half
    )
}

// ---- laying ink -----------

private const val GRAIN = 6f // droplet spacing
private const val CLUMP = 0.75f // 0 is clean white-noise stipple, 1 is all droplets

// the dither threshold of a pixel. droplets sit on a grid GRAIN px apart,
// jittered, each with its own size and its own moment to show up as a fade comes on.
private fun thr(x: Int, y: Int): Float {
    val gx = (x + 0.5f) / GRAIN
    val gy = (y + 0.5f) / GRAIN
    val cx = floor(gx).toInt()
    val cy = floor(gy).toInt()
    var near = Float.MAX_VALUE
    var late = 0f
    for (j in -1..1) {
        for (i in -1..1) {
            val kx = cx + i
            val ky = cy + j
            val dx = gx - kx - hash01(kx, ky, 1, SEED)
            val dy = gy - ky - hash01(kx, ky, 2, SEED)
            // a bigger droplet reaches further from its centre
            val d = length(dx, dy) / (0.45f + 0.55f * hash01(kx, ky, 3, SEED))
            if (d < near) {
                near = d
                late = hash01(kx, ky, 4, SEED)
            }
        }
    }
    val drop = 0.55f * near + 0.45f * late
    val mist = hash01(x, y, 7, SEED)
    // under 1, or the far corners between droplets never fill even at the end of a fade
    return (CLUMP * drop + (1f - CLUMP) * mist).coerceAtMost(0.999f)
}

// nearest spine distance (squared)
private val best = FloatArray(W * H)
private val along = FloatArray(W * H)

private fun span(lo: Float, hi: Float, reach: Float, first: Int, last: Int) =
    max(first, floor(lo - reach).toInt())..min(last, ceil(hi + reach).toInt())

// a fat line with round caps is every pixel within r of the spine, so it is a distance field
// over the segments. keeps the nearest segment per pixel in best, and where on the spine in along
private fun nearest(b: Band, cols: IntRange, rows: IntRange, reach: Float) {
    for (y in rows) best.fill(Float.MAX_VALUE, y * W + cols.first, y * W + cols.last + 1)
    for (k in 0 until b.xs.size - 1) {
        val ax = b.xs[k]
        val ay = b.ys[k]
        val ex = b.xs[k + 1] - ax
        val ey = b.ys[k + 1] - ay
        val len2 = ex * ex + ey * ey
        val sx = span(min(ax, ax + ex), max(ax, ax + ex), reach, cols.first, cols.last)
        val sy = span(min(ay, ay + ey), max(ay, ay + ey), reach, rows.first, rows.last)
        for (y in sy) {
            val qy = y + 0.5f - ay
            for (x in sx) {
                val qx = x + 0.5f - ax
                val u = if (len2 > 0f) ((qx * ex + qy * ey) / len2).coerceIn(0f, 1f) else 0f
                val dx = qx - u * ex
                val dy = qy - u * ey
                val d2 = dx * dx + dy * dy
                val i = y * W + x
                if (d2 < best[i]) {
                    best[i] = d2
                    along[i] = b.arc[k] + u * (b.arc[k + 1] - b.arc[k])
                }
            }
        }
    }
}

private fun lay(px: IntArray, b: Band, w: Float) {
    val r = w / 2f
    val reach = r + 1.5f
    val cols = span(b.xs.min(), b.xs.max(), reach, 0, W - 1)
    val rows = span(b.ys.min(), b.ys.max(), reach, 0, H - 1)
    if (cols.isEmpty() || rows.isEmpty()) return
    nearest(b, cols, rows, reach)

    val edge = (r + 0.5f) * (r + 0.5f)
    for (y in rows) {
        for (x in cols) {
            val i = y * W + x
            if (best[i] >= edge) continue
            val ink = b.inkAt(along[i], x, y)
            if (ink == NONE) continue

            val cover = (r - sqrt(best[i]) + 0.5f).coerceIn(0f, 1f)
            px[i] = if (cover >= 1f) ink else lerpColor(px[i], ink, cover)
        }
    }
}

// plumbing

fun main(args: Array<String>) {
    val gart = Gart.of("fascia", W, H)
    val sw = Stopwatch()
    val rnd = Random(SEED * 7919L + 11)
    val dSep = DSEP * W
    val w = FAT * dSep

    val lines = flow(rnd).streamlines(Dimension(W, H), rnd, dSep = dSep, margin = dSep)
    val bands = lines.flatMap { cut(it, w, rnd) }.shuffled(rnd)

    val g = gart.gartvas()
    Gartmap(g).use { m ->
        m.pixels.fill(PAPER)
        for (b in bands) lay(m.pixels, b, w)
        m.drawToCanvas()
    }
    gart.saveImage(g, OUT.ensureExtension())
    println("fascia: seed=$SEED, pal=$PAL (cool${FIVES[PAL - 1]}), ${inks.size} inks, ${lines.size} lines, ${bands.size} bands, ${sw.ms}ms")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
