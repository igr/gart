package lines.pecten

import dev.oblac.gart.Gart
import dev.oblac.gart.brush.Brushes
import dev.oblac.gart.brush.drawBrush
import dev.oblac.gart.color.BwColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.lumOf
import dev.oblac.gart.color.space.ColorOKLAB
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.gfx.offset
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.WIDE_SCREEN_RATIO
import dev.oblac.gart.math.between
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.length
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.shortSide
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.noiseOffset
import dev.oblac.gart.util.Stopwatch
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tanh
import kotlin.random.Random

/**
 * pecten - a stack of layers, bent by a few big folds. each layer has one ink. the layer is cut
 * into rows of short brush dashes, like the teeth of a comb.
 *
 * each layer has its own thickness along x. it swells into a lens, it thins, and sometimes it
 * pinches out. a lens pushes the layer edges near it apart, the edges over it up and the edges
 * under it down. the push fades out over a few layers. after that, a fold lifts or sinks the
 * stack around one point. the fold fades out above and below its own depth. layers never cross.
 *
 * a dash fills its row from edge to edge, less a thin black gap. where a row pinches, the dash
 * shrinks to a dot. the angle of the dash is between the normal of the band and the vertical.
 * the vertical is the axial plane of the folds.
 */
private const val H = 2000
private val W = shortSide(H, WIDE_SCREEN_RATIO)
private val SEED = pi("seed", 4)
private val OUT = ps("out", "pecten")
private val SS = pi("ss", 3, 1..4)
private val PAL = pi("pal", 35, 1..Palettes.COOL_COUNT)

// the stack
private val LAYERS = pi("layers", 20, 6..90) // across the page
private val SWELL = pf("swell", 1.5f, 0f..2f) // 0 = every layer even
private val FOLDS = pi("folds", 3, 0..6)
private val AMP = pf("amp", 0.12f, 0f..0.3f) // fold height
private val PITCH = pf("pitch", 13f, 5f..40f) // dash to dash along a row, px
private val CLEAVE = pf("cleave", 0.4f, 0f..1f) // 0 square to the band, 1 upright

// the marks
private val BRUSH = ps("brush", "chalk")
private val PASSES = pi("passes", 2, 1..20)

private const val WIDE = 0.8f // dash width, share of pitch
private const val GAP = 0.12f // black between two rows, share of pitch
private const val LEAN = 0.15f
private const val WOB = 0.012f // small waves
private const val DRIFT = 0.3f
private const val DRAPE = 1.5f //
private const val FAT = 1.2f

private val NZ = noiseOffset(SEED.toLong())
private const val GROUND = BwColors.warmBlack0

private val PAD = 2f * PITCH
private val GRID = (W + 2f * PAD).toInt() + 1

private val inks = Palettes.coolPalette(PAL).let { p ->
    val lit = p.filter { lumOf(it) >= 30f }
    if (lit.size >= 2) lit else p
}

private class Fold(val x: Float, val half: Float, val lift: Float, val depth: Float, val reach: Float) {
    fun at(px: Float, v: Float): Float {
        val u = (px - x) / half
        val s = (v - depth) / reach
        return lift * exp(-u * u - s * s)
    }
}

private fun folds(rnd: Random) = List(FOLDS) {
    val lift = AMP * H * rnd.between(0.5f, 1f) * if (rnd.nextBoolean()) 1f else -1f
    val x = W * rnd.between(0.05f, 0.95f)
    val half = W * rnd.between(0.22f, 0.5f)
    val depth = H * rnd.between(0.1f, 0.9f)
    Fold(x, half, lift, depth, max(H * rnd.between(0.35f, 0.8f), 3f * abs(lift)))
}

private class Bend(val folds: List<Fold>) {
    val bleed = folds.sumOf { abs(it.lift).toDouble() }.toFloat() + WOB * H

    fun y(x: Float, v: Float): Float {
        var d = WOB * H * SimplexNoise.noise(x / (0.5f * W) + NZ, v / (0.4f * H) + NZ)
        for (f in folds) d += f.at(x, v)
        return v + d
    }
}

// layers --------------------------------------------------------------

private class Layer(val id: Int, val th: Float, val rows: Int, val ink: Int) {
    private val len = W * lerp(0.45f, 1.3f, hash01(id, 1, SEED))
    private val amp = SWELL * lerp(0.3f, 1.4f, hash01(id, 2, SEED))

    fun swell(x: Float): Float {
        val m = 1f + amp * SimplexNoise.noise(x / len + NZ, id * 3.7f + NZ)
        return if (m > 1f) 1f + FAT * tanh((m - 1f) / FAT) else max(0f, m)
    }
}

private class Stack(val layers: List<Layer>, val top: Float)

private fun layer(id: Int, from: Float, walk: List<Layer>, rnd: Random): Layer {
    val t = H.toFloat() / LAYERS
    val th = t * rnd.between(0.45f, 1.6f)
    val rows = when {
        th < 0.75f * t -> 1
        th < 1.5f * t -> 2
        else -> 3
    }
    return Layer(id, th, rows, pick((from / H).coerceIn(0f, 1f), walk.takeLast(4).map { it.ink }, rnd))
}

private fun pick(u: Float, recent: List<Int>, rnd: Random): Int {
    val last = recent.lastOrNull()
    val w = FloatArray(inks.size) { j ->
        val d = (j / (inks.size - 1f) - u) / DRIFT
        var p = exp(-d * d)
        if (last != null) p *= 0.02f + smoothstep(0.08f, 0.25f, apart(inks[j], last))
        if (inks[j] in recent) p *= 0.15f
        if (inks[j] == last) 0f else p
    }
    var r = rnd.nextFloat() * w.sum()
    for (j in w.indices) {
        r -= w[j]
        if (r <= 0f && w[j] > 0f) return inks[j]
    }
    return inks[w.indices.last { w[it] > 0f }]
}

private fun apart(c1: Int, c2: Int): Float {
    val a = ColorOKLAB.of(Color4f(c1))
    val b = ColorOKLAB.of(Color4f(c2))
    return length(a.l - b.l, length(a.a - b.a, a.b - b.b))
}

private fun stack(bleed: Float, rnd: Random, up: Random): Stack {
    val below = ArrayList<Layer>()
    var bottom = 0f
    while (bottom < H + bleed) {
        val l = layer(below.size, bottom, below, rnd)
        below += l
        bottom += l.th
    }

    val above = ArrayList<Layer>()
    var top = 0f
    while (top > -bleed) {
        val l = layer(-1 - above.size, top, below.take(4).reversed() + above, up)
        above += l
        top -= l.th
    }
    return Stack(above.reversed() + below, top)
}

private fun edges(stack: Stack): Array<FloatArray> {
    val ls = stack.layers
    val n = ls.size
    val e = Array(n + 1) { FloatArray(GRID) }
    val rest = FloatArray(n + 1) // where edge k sits with no lenses
    for (k in 0 until n) rest[k + 1] = rest[k] + ls[k].th
    val fade = FloatArray(n + 1) { exp(-it / DRAPE) }
    val shove = FloatArray(n) // how far each layer shoves its two edges out, per column
    for (ix in 0 until GRID) {
        for (i in 0 until n) shove[i] = ls[i].th * (ls[i].swell(ix - PAD) - 1f) / 2f
        var prev = Float.NEGATIVE_INFINITY
        for (k in 0..n) {
            var d = 0f
            for (i in 0 until k) d += shove[i] * fade[k - 1 - i]
            for (i in k until n) d -= shove[i] * fade[i - k]

            prev = max(stack.top + rest[k] + d, prev)
            e[k][ix] = prev
        }
    }
    return e
}

// ---- dashes --------------------------------------------------------------------------

private class Dash(val a: Point, val b: Point, val w: Float, val ink: Int)

private fun dashes(stack: Stack, bend: Bend, rnd: Random): List<Dash> {
    val e = edges(stack)
    val out = ArrayList<Dash>()
    for ((k, l) in stack.layers.withIndex()) {
        for (r in 0 until l.rows) {
            val top = curve(e[k], e[k + 1], r / l.rows.toFloat(), bend)
            val bot = curve(e[k], e[k + 1], (r + 1f) / l.rows, bend)
            if (top.min() > H || bot.max() < 0f) continue // off the page
            val mid = curve(e[k], e[k + 1], (r + 0.5f) / l.rows, bend)
            out += row(top, mid, bot, l.ink, rnd)
        }
    }
    return out
}

private fun curve(e0: FloatArray, e1: FloatArray, f: Float, bend: Bend) =
    FloatArray(GRID) { bend.y(it - PAD, lerp(e0[it], e1[it], f)) }

private fun FloatArray.at(x: Float): Float {
    val f = (x + PAD).coerceIn(0f, size - 1.001f)
    val i = f.toInt()
    return lerp(this[i], this[i + 1], f - i)
}

private fun row(top: FloatArray, mid: FloatArray, bot: FloatArray, ink: Int, rnd: Random): List<Dash> = buildList {
    val arc = FloatArray(GRID)
    for (i in 1 until GRID) arc[i] = arc[i - 1] + length(1f, mid[i] - mid[i - 1])
    var s = rnd.nextFloat() * PITCH
    var i = 0
    while (s < arc[GRID - 1]) {
        while (arc[i + 1] < s) i++
        val f = (s - arc[i]) / (arc[i + 1] - arc[i])
        val p = Point(i + f - PAD, lerp(mid[i], mid[i + 1], f))
        val tan = Vec2(1f, mid[i + 1] - mid[i]).normalize()
        dash(p, tan, top, bot, ink, rnd)?.let { add(it) }
        s += PITCH
    }
}

private fun dash(p: Point, tan: Vec2, top: FloatArray, bot: FloatArray, ink: Int, rnd: Random): Dash? {
    val slide = PITCH * rnd.between(-0.06f, 0.06f)
    val tilt = rnd.between(-0.04f, 0.04f)
    val stretch = rnd.between(0.94f, 1.04f)
    val w = WIDE * PITCH * rnd.between(0.9f, 1.1f)

    val c = p.offset(tan * slide)
    val normal = Vec2(-tan.y, tan.x)
    val lean = LEAN * SimplexNoise.noise(c.x / (0.4f * W) + NZ + 31f, c.y / (0.4f * W) + NZ)
    val dir = (normal * (1f - CLEAVE) + Vec2(0f, CLEAVE)).normalize().rotate(lean + tilt)
    val t0 = meet(c, dir, top)
    val t1 = meet(c, dir, bot)
    val long = (t1 - t0) * stretch - GAP * PITCH
    if (long < 1.5f) return null
    val mid = (t0 + t1) / 2f
    val a = c.offset(dir * (mid - long / 2f))
    val b = c.offset(dir * (mid + long / 2f))
    if (max(a.y, b.y) < -w || min(a.y, b.y) > H + w) return null
    if (max(a.x, b.x) < -w || min(a.x, b.x) > W + w) return null
    return Dash(a, b, min(w, long), ink)
}

private fun meet(c: Point, dir: Vec2, curve: FloatArray): Float {
    var l = (curve.at(c.x) - c.y) / dir.y
    repeat(4) { // newton, 4 steps is plenty
        val x = c.x + l * dir.x
        val slope = curve.at(x + 0.5f) - curve.at(x - 0.5f)
        l -= (c.y + l * dir.y - curve.at(x)) / (dir.y - slope * dir.x)
    }
    return l
}

private fun draw(c: Canvas, dashes: List<Dash>, rnd: Random) {
    val brush = Brushes.of(BRUSH)
    val band = brush.weight + 2f * brush.scatter // how wide a size 1 stroke lands
    for (d in dashes) {
        val size = d.w / band
        val tip = brush.weight * size / 2f
        val run = Vec2(d.b.x - d.a.x, d.b.y - d.a.y)
        val cut = run.normalize() * min(tip, run.length() / 2f - 0.25f)
        val a = d.a.offset(cut)
        val b = d.b.offset(cut * -1f)
        repeat(PASSES) { c.drawBrush(a, b, brush, d.ink, size, rnd) }
    }
}

fun main(args: Array<String>) {
    val gart = Gart.of("pecten", W, H)
    val sw = Stopwatch()
    val bend = Bend(folds(Random(SEED * 7919 + 1)))
    val stack = stack(bend.bleed, Random(SEED), Random(SEED * 7919 + 2))
    val dashes = dashes(stack, bend, Random(SEED * 7919 + 3))

    val big = gart.supersampled(SS)
    val c = big.canvas
    c.clear(GROUND)
    c.scale(SS.toFloat(), SS.toFloat())
    draw(c, dashes, Random(SEED * 7919 + 4))
    val g = big.downsample(SS)
    println("pecten: seed=$SEED, ${stack.layers.size} layers, ${dashes.size} dashes, ${sw.ms}ms")

    gart.saveImage(g, OUT.ensureExtension())
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
