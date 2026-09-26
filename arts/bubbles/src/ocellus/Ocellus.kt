package ocellus

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.space.mix
import dev.oblac.gart.color.space.of
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.divOrZero
import dev.oblac.gart.math.length
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.smin
import dev.oblac.gart.util.Stopwatch
import dev.oblac.gart.util.parallelForRows
import org.jetbrains.skia.Color4f
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.random.Random

/*
 * ocellus - eyespots
 *
 * Eyes. The piece puts 9 eyes at random positions on the page.
 * Each eye is a stack of levels: a core disc of random size on top, then one ring for
 * each level under it. The depth of an eye is the number of levels in its stack.
 * All eyes use the same ring width for each. The outer rings are the widest,
 * but level 1 is only a thin halo.
 *
 * Levels. Each level is one shape: the discs of all eyes that are deep enough for that level.
 * A soft min melts these discs together, and the melt is looser
 * at lower levels. Thus the lower levels melt into one soft body, and only the top rings stay round.
 * The same ring widths and the looser melt keep each level inside the level under it.
 * For each pixel, the code mixes the level colors over the cream ground. The amount of
 * each color is the part of the pixel that the level covers, so the edges are smooth.
 *
 * Specks. Where three or more eyes meet, a level can almost close and leave a small hole of
 * the level under it. After the paint, a flood fill finds each patch of pixels that have the
 * same top level. If a patch has less than 400 pixels and only one higher level is around it,
 * the patch is a speck. The code paints each speck again, together with a 4-pixel border
 * around it. This time, all levels up to that higher level cover each pixel fully. A second pass
 * closes a hole that is inside another hole
 */
private const val W = 1200
private const val H = 1200
private const val LEVELS = 13
private val SEED = pi("seed", 20)
private val OUT = ps("out", "ocellus")

// the eyes
private val COUNT = pi("count", 9, 1..80)
private val LOW = pi("low", 4, 1..LEVELS) // the shallowest an eye may sit
private val DEEP = pf("deep", 1.6f, 0.3f..6f) // > 1 leans the depths toward the full stack
private val CORE = pf("core", 0.09f, 0.01f..0.3f)
private val ROOM = pf("room", 0.03f, 0f..0.3f)
private val MARGIN = pf("margin", 0.12f, -0.2f..0.45f)

// rings
private val BAND = pf("band", 0.011f, 0.003f..0.05f) // ring width up by the eye
private val GROW = pf("grow", 2.5f, 0f..8f) // how much wider the outer rings get
private val FUSE = pf("fuse", 0.5f, 0.02f..2f)
private val POOL = pf("pool", 2f, 1f..10f)

// the inks
private val GROUND = 0xFFF5EBC8.toInt()

private val RAMPS = listOf(
    Palettes.cool183,
    Palettes.mix7.pick(
        18, 6, 19, 17, 16, 8, 21, 9, 15, 20, 30, 7, 41, 5, 31, 4, 29, 32, 40, 28, 3,
        33, 39, 27, 2, 34, 26, 10, 22, 14, 38, 23, 35, 25, 11, 37, 13, 1, 36, 12, 0, 24,
    ),
    Palettes.mix12.pick(14, 12, 9, 5, 4, 10, 7, 13, 11, 8, 3, 2, 0, 1, 6),
    Palettes.cool31.pick(2, 1, 4, 3, 5, 6, 7, 8, 9, 0),
    Palettes.cool94.pick(0, 1, 2, 3, 4),
    Palettes.cool85.pick(0, 4, 3, 2, 1), // no dark eye here
)
private val PAL = pi("pal", 0, RAMPS.indices)

// ground in front
private fun inks(): Palette {
    val ramp = RAMPS[PAL]
    val levels = if (ramp.size == LEVELS) ramp else ramp.stretchOklab(LEVELS)
    return Palette.of(GROUND) + levels
}

// geometry -----------

// ring width per level
private val width = FloatArray(LEVELS + 1) { k ->
    val s = (LEVELS - 1f - k) / (LEVELS - 3f)
    when (k) {
        0, LEVELS -> 0f
        1 -> 0.6f * BAND * W
        else -> BAND * W * (1f + GROW * s * s * s)
    }
}

private val reach = FloatArray(LEVELS + 1).also { r ->
    for (k in LEVELS - 1 downTo 1) r[k] = r[k + 1] + width[k]
}
private val melt = FloatArray(LEVELS + 1) { k -> FUSE * (reach[k] + BAND * W) * (if (k == 1) POOL else 1f) }

private class Eye(val x: Float, val y: Float, val r: Float, val depth: Int) {
    val radius = FloatArray(LEVELS + 1) { k -> if (k in 1..depth) r + reach[k] - reach[depth] else 0f }

    fun has(level: Int) = level <= depth
    fun crowds(o: Eye) = hypot(x - o.x, y - o.y) < r + o.r + ROOM * W
}

private fun eyes(): List<Eye> {
    val rnd = Random(SEED * 7919L + 11)
    val out = mutableListOf<Eye>()
    var tries = 0
    while (out.size < COUNT && tries++ < 20000) {
        val eye = rnd.nextEye()
        if (out.none { it.crowds(eye) }) out += eye
    }
    return out
}

private fun Random.nextEye(): Eye {
    val depth = LEVELS - (nextFloat().pow(DEEP) * (LEVELS - LOW + 1)).toInt()
    val r = W * lerp(0.006f, CORE, nextFloat())
    val x = lerp(MARGIN * W, W - MARGIN * W, nextFloat())
    val y = lerp(MARGIN * W, H - MARGIN * W, nextFloat())
    return Eye(x, y, r, depth)
}

// plumbing

// the field seen from one pixel
private class Field(val eyes: List<Eye>, inks: Palette) {
    private val n = eyes.size
    private val deepest = eyes.maxOf { it.depth }
    private val ink = Array(LEVELS + 1) { Color4f.of(inks[it]) }
    private val d = FloatArray(n) // how far each eye is + which way
    private val ux = FloatArray(n)
    private val uy = FloatArray(n)
    private var v = 0f // the soft min so far, gradient
    private var gx = 0f
    private var gy = 0f
    var top = 0

    fun shade(x: Int, y: Int, fillTo: Int = 0): Int {
        look(x + 0.5f, y + 0.5f)
        var c = ink[0]
        top = 0
        for (k in 1..deepest) {
            val a = if (k <= fillTo) 1f else cover(k)
            if (a <= 0f) break // levels nest
            c = c.mix(ink[k], a)
            if (a >= 0.5f) top = k
        }
        return c.toColor()
    }

    private fun look(px: Float, py: Float) {
        for (i in 0 until n) {
            val dx = px - eyes[i].x
            val dy = py - eyes[i].y
            d[i] = length(dx, dy)
            ux[i] = divOrZero(dx, d[i], 1e-3f)
            uy[i] = divOrZero(dy, d[i], 1e-3f)
        }
    }

    // how much of the pixel level k covers
    private fun cover(k: Int): Float {
        var first = true
        for (i in 0 until n) {
            if (!eyes[i].has(k)) continue
            val a = d[i] - eyes[i].radius[k]
            if (first) {
                v = a
                gx = ux[i]
                gy = uy[i]
                first = false
            } else {
                meltIn(a, ux[i], uy[i], melt[k])
            }
        }
        val slope = length(gx, gy).coerceAtLeast(0.25f)
        return (0.5f - v / slope).coerceIn(0f, 1f)
    }

    private fun meltIn(a: Float, ax: Float, ay: Float, k: Float) {
        val h = (0.5f + 0.5f * (v - a) / k).coerceIn(0f, 1f)
        v = smin(a, v, k)
        gx = lerp(gx, ax, h)
        gy = lerp(gy, ay, h)
    }
}

private fun paint(eyes: List<Eye>, inks: Palette, m: Gartmap): IntArray {
    val tops = IntArray(W * H)
    parallelForRows(H) { y0, y1 ->
        val f = Field(eyes, inks)
        for (y in y0 until y1) for (x in 0 until W) {
            m[x, y] = f.shade(x, y)
            tops[y * W + x] = f.top
        }
    }
    return tops
}

// specks

private const val SPECK = 400
private const val RIM = 4

// one patch of a single level.. around is the level all round it, -1 if two levels or the page edge touch it
private class Patch(val size: Int, val around: Int)

private fun fillSpecks(tops: IntArray, f: Field, m: Gartmap): Int {
    val seen = BooleanArray(W * H)
    val cells = IntArray(W * H)
    var filled = 0
    for (p0 in 0 until W * H) {
        if (seen[p0]) continue
        val found = flood(p0, tops, seen, cells)
        val speck = found.around > tops[p0] && found.size < SPECK
        if (!speck) continue
        filled++
        for (i in 0 until found.size) tops[cells[i]] = found.around
        for (i in 0 until found.size) reshade(cells[i], found.around, tops, f, m)
    }
    return filled
}

private fun flood(start: Int, tops: IntArray, seen: BooleanArray, cells: IntArray): Patch {
    val level = tops[start]
    var size = 0
    var around = -1
    var enclosed = true
    fun visit(q: Int) {
        if (tops[q] == level) {
            if (!seen[q]) {
                seen[q] = true
                cells[size++] = q
            }
        } else if (around == -1) around = tops[q]
        else if (around != tops[q]) enclosed = false
    }
    seen[start] = true
    cells[size++] = start
    var i = 0
    while (i < size) {
        val p = cells[i++]
        val x = p % W
        val y = p / W
        if (x == 0 || y == 0 || x == W - 1 || y == H - 1) enclosed = false
        if (x > 0) visit(p - 1)
        if (x < W - 1) visit(p + 1)
        if (y > 0) visit(p - W)
        if (y < H - 1) visit(p + W)
    }
    return Patch(size, if (enclosed) around else -1)
}

private fun reshade(p: Int, level: Int, tops: IntArray, f: Field, m: Gartmap) {
    val x = p % W
    val y = p / W
    for (yy in y - RIM..y + RIM) for (xx in x - RIM..x + RIM) {
        if (xx !in 0 until W || yy !in 0 until H) continue
        if (tops[yy * W + xx] >= level) m[xx, yy] = f.shade(xx, yy, fillTo = level)
    }
}

fun main(args: Array<String>) {
    val gart = Gart.of("ocellus", W, H)
    val sw = Stopwatch()
    val g = gart.gartvas()
    val eyes = eyes()
    val inks = inks()
    var specks = 0
    Gartmap(gart.d).use { m ->
        val tops = paint(eyes, inks, m)
        // twice!
        repeat(2) { specks += fillSpecks(tops, Field(eyes, inks), m) }
        m.drawToCanvas(g)
    }
    gart.saveImage(g, OUT.ensureExtension())
    println("ocellus: seed=$SEED, pal=$PAL, ${eyes.size} eyes, depths ${eyes.map { it.depth }}, $specks specks, ${sw.ms}ms")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
