package filum

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.bluef
import dev.oblac.gart.color.chromaOf
import dev.oblac.gart.color.green
import dev.oblac.gart.color.greenf
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.color.lighten
import dev.oblac.gart.color.red
import dev.oblac.gart.color.redf
import dev.oblac.gart.color.rgb
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.gfx.distSquaredToSegment
import dev.oblac.gart.gfx.length
import dev.oblac.gart.gfx.sdRoundBox
import dev.oblac.gart.gfx.toPoints
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.degToRad
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.rndb
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndi
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.noiseOffset
import dev.oblac.gart.pixels.boxDownsample
import dev.oblac.gart.smooth.catmullRomSpline
import dev.oblac.gart.util.Stopwatch
import dev.oblac.gart.util.parallelBands
import dev.oblac.gart.util.timed
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Color
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * filum - a hot wire over a table of cards.
 *
 * The picture is a grid of small stacks of cards. Every card is the same square, and it is half
 * transparent. Each card has one or two rounded corners. Where many cards overlap, a stack is
 * almost white in its middle. Where a stack is thin, the table shows through.
 *
 * One wire hangs over the table in a slow curve. The piece never draws the wire itself. The wire
 * is only the light in the room. Where the wire passes a stack, the cards catch its light. A card
 * that sits higher in the stack is brighter. Each card throws a short thin shadow onto the card
 * below it, away from the wire.
 *
 * The table does not reflect the light. Between the stacks there is no sign of the wire. You read
 * the path of the wire from the cards.
 */
fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("filum", W, H)
    println(gart)

    val inks = Inks()
    println("seed=$SEED pal=${inks.name}")

    val sw = Stopwatch()
    val wire = route()
    val table = grid()
    val ramp = inks.ramp()
    println("${table.stackCount} cells, ${table.cardCount} cards, top at ${"%.0f".format(table.top / SS)}px, in ${sw.ms}ms")

    val lit = timed("lit") { light(table, wire, ramp, inks.tableCol) }

    val g = gart.gartvas()
    val map = Gartmap(g.d)
    boxDownsample(lit, SS, map)
    map.drawToCanvas(g)
    if (GRAIN > 0f) addGrain(g, GRAIN, SEED)

    gart.saveImage(g, "$OUT.png")
    if (!headless) gart.window().showImage(g)
}

private const val W = 1024
private const val H = 1024
private const val COOL = 181

private val SEED = pi("seed", 1)
private val OUT = ps("out", "filum")
private val SS = pi("ss", 2, 1..4)
private val PAL = pi("pal", 0, -1..COOL)
private val HUES = pi("hues", 5, 2..16)
private val COLS = pi("cols", 12, 3..40)
private val STACK = pi("stack", 5, 1..12)
private val ALPHA = pf("alpha", 0.6f, 0.05f..1f)
private val ORDER = pi("order", 1, 0..2)
private val BRICK = pi("brick", 0, 0..1)
private val WAVE = pf("wave", 1f, 0f..3f)
private val WANDER = pf("wander", 0f, 0f..3f)
private val LIFT = pf("lift", 90f, 10f..400f)
private val GLOW = pf("glow", 2.5f, 0f..12f)
private val GRAIN = pf("grain", 0.04f, 0f..1f)

private const val TINT = 1f
private const val VIVID = 1.4f
private const val GAP = 0.22f
private const val LEAN = 0.12f
private const val RELIEF = 0.7f
private const val ROUND = 0.6f
private const val TILT = 1.5f
private const val THICK = 5f
private const val CORE = 1.5f
private const val HALO = 6f
private const val AMB = 0.95f
private const val PULSE = 0.3f
private const val TABLE = 0.12f

private val GW = W * SS
private val GH = H * SS
private val rng = Random(SEED)
private val NZOFF = noiseOffset(SEED.toLong())

private class Card(val cx: Float, val cy: Float, private val hw: Float, ang: Float, private val rad: FloatArray) {
    private val ca = cos(ang)
    private val sa = sin(ang)
    val ext = abs(ca) * hw + abs(sa) * hw

    fun sd(px: Float, py: Float): Float {
        val dx = px - cx
        val dy = py - cy
        return sdRoundBox(dx * ca + dy * sa, dy * ca - dx * sa, hw, hw, rad[0], rad[1], rad[2], rad[3])
    }
}

private class Table(stacks: List<List<Card>>) {
    val stackCount = stacks.size
    val cardCount = stacks.sumOf { it.size }
    val heights = FloatArray(GW * GH)
    val counts = IntArray(GW * GH)
    var top = 0f
        private set

    init {
        val th = THICK * SS
        for (stack in stacks) for (c in stack) {
            val x0 = max(0, floor(c.cx - c.ext).toInt())
            val x1 = min(GW - 1, ceil(c.cx + c.ext).toInt())
            val y0 = max(0, floor(c.cy - c.ext).toInt())
            val y1 = min(GH - 1, ceil(c.cy + c.ext).toInt())
            if (x1 < x0 || y1 < y0) continue
            val bw = x1 - x0 + 1
            val inside = BooleanArray(bw * (y1 - y0 + 1))
            var base = 0f
            for (y in y0..y1) for (x in x0..x1) if (c.sd(x + 0.5f, y + 0.5f) <= 0f) {
                inside[(y - y0) * bw + x - x0] = true
                base = max(base, heights[y * GW + x])
            }
            val z = base + th
            for (y in y0..y1) for (x in x0..x1) if (inside[(y - y0) * bw + x - x0]) {
                val i = y * GW + x
                heights[i] = z
                counts[i]++
            }
            top = max(top, z)
        }
    }
}

private fun grid(): Table {
    val lo = Layout()
    return Table((0 until lo.rows).flatMap { j -> List(lo.cols(j)) { i -> lo.stack(i, j) } })
}

private class Layout {
    val pitch = 0.88f * GW / COLS
    val rows = (0.88f * GH / pitch).toInt()
    val s = pitch * (1f - GAP)
    val room = 0.9f * GAP * pitch
    val x0 = 0.5f * (GW - COLS * pitch)
    val y0 = 0.5f * (GH - rows * pitch)
    val dir = rng.rndf(TAUf)
    val ox = rng.rndf(100f)
    val oy = rng.rndf(100f)

    fun odd(j: Int) = BRICK == 1 && j % 2 == 1
    fun cols(j: Int) = if (odd(j)) COLS - 1 else COLS

    fun stack(i: Int, j: Int): List<Card> {
        val cx = x0 + (i + (if (odd(j)) 1f else 0.5f)) * pitch
        val cy = y0 + (j + 0.5f) * pitch
        val hill = 0.5f + 0.5f * SimplexNoise.noise(i * 0.21f + ox + NZOFF, j * 0.21f + oy)
        val n = 1 + (lerp(rng.rndf(), hill, RELIEF) * STACK * 0.999f).toInt()
        rng.rndi(5) // Preserve the random sequence used by existing seeds.
        val hw = s * 0.5f
        val rad = corners(hw, i, j)
        val base = degToRad(rng.rndf(-TILT, TILT))
        val d = dir + rng.rndf(-0.25f, 0.25f)
        val step = if (n > 1) min(LEAN * s, room / (n - 1)) else 0f
        return List(n) { k ->
            val q = (k - (n - 1) * 0.5f) * step
            Card(cx + cos(d) * q, cy + sin(d) * q, hw, base + rng.rndf(-TILT, TILT) * PIf / 540f, rad)
        }
    }
}

private fun corners(hw: Float, i: Int, j: Int): FloatArray {
    val r = ROUND * hw * rng.rndf(0.7f, 1f)
    val rad = FloatArray(4)
    when (ORDER) {
        1 -> {
            val c = (i + j) % 4
            rad[c] = r
            rad[(c + 1) % 4] = r
        }
        2 -> rad[if (j % 2 == 0) i % 2 else 3 - i % 2] = r
        else -> {
            val p = rng.rndf()
            when {
                p < 0.45f -> {
                    val c = rng.rndi(4)
                    rad[c] = r
                    rad[(c + 1) % 4] = r
                }
                p < 0.70f -> rad[rng.rndi(4)] = r
                p < 0.85f -> {
                    val c = rng.rndi(2)
                    rad[c] = r
                    rad[c + 2] = r
                }
                else -> {
                    val c = rng.rndi(4)
                    for (q in 0..3) if (q != c) rad[q] = r
                }
            }
        }
    }
    return rad
}

private const val KNOTS = 40
private const val BENDS = 5

private fun route(): Path {
    val rot = degToRad(rng.rndf(-25f, 25f))
    val across = rng.rndb()
    val mid = rng.rndf(-0.12f, 0.12f)
    val amp = rng.rndf(0.08f, 0.14f)
    val frq = rng.rndf(0.5f, 0.8f)
    val phs = rng.rndf(TAUf)
    val phs2 = rng.rndf(TAUf)
    println("wire ${if (across) "across" else "down"}, turned ${"%.0f".format(rot * 180f / PIf)}")
    val jit = Random(SEED * 31 + 7)
    val off = FloatArray(BENDS + 1) { jit.rndf(-1f, 1f) }
    val pts = ArrayList<Point>()
    for (i in 0..KNOTS) {
        val u = -0.35f + 1.7f * i / KNOTS
        var v = mid + WAVE * amp * (sin(TAUf * frq * u + phs) + 0.25f * sin(TAUf * frq * 2.3f * u + phs2))
        if (WANDER > 0f) v += WANDER * amp * bend(off, i.toFloat() / KNOTS)
        val p = Vec2((u - 0.5f) * W, v * H).rotate(rot)
        val x = p.x + 0.5f * W
        val y = p.y + 0.5f * H
        pts += if (across) Point(x, y) else Point(y, x)
    }
    return catmullRomSpline(pts, 12)
}

private fun bend(off: FloatArray, t: Float): Float {
    val s = t * (off.size - 1)
    val i = min(s.toInt(), off.size - 2)
    val f = s - i
    val p0 = off[max(i - 1, 0)]
    val p1 = off[i]
    val p2 = off[i + 1]
    val p3 = off[min(i + 2, off.size - 1)]
    return 0.5f * (2f * p1 + (p2 - p0) * f + (2f * p0 - 5f * p1 + 4f * p2 - p3) * f * f + (3f * p1 - p0 - 3f * p2 + p3) * f * f * f)
}

private const val BLOCK = 16
private const val MARCH = 2f
private val REACH = 64f * SS

private fun light(t: Table, wire: Path, ramp: Palette, tableCol: Int): IntArray {
    val lamp = Lamp(t, wire, ramp, tableCol)
    val img = IntArray(GW * GH)
    parallelBands(GH) { y0, y1 ->
        for (y in y0 until y1) for (x in 0 until GW) {
            val i = y * GW + x
            img[i] = if (t.counts[i] == 0) tableCol else lamp.pixel(x, y)
        }
    }
    return img
}

private class Wire(path: Path, ramp: Palette, L: Float, top: Float) {
    val len = path.length()
    val n = max(2, ceil(len / min(6f, (L - top) * 0.4f / SS)).toInt())
    val dsOut = len / n
    val ds = dsOut * SS
    val x = FloatArray(n + 1)
    val y = FloatArray(n + 1)
    val pulse = FloatArray(n + 1)
    val r = FloatArray(n + 1)
    val g = FloatArray(n + 1)
    val b = FloatArray(n + 1)

    init {
        val pts = path.toPoints(n + 1)
        for (k in 0..n) {
            x[k] = pts[k].x * SS
            y[k] = pts[k].y * SS
            pulse[k] = 1f - PULSE * 0.5f * (1f + SimplexNoise.noise(k * dsOut * 0.004f + NZOFF, 0.7f))
            val col = ramp.sampleOklch((k * dsOut / len - 0.2f) / 0.6f)
            val m = maxOf(red(col), green(col), blue(col)).toFloat()
            r[k] = tone(red(col), m)
            g[k] = tone(green(col), m)
            b[k] = tone(blue(col), m)
        }
    }
}

private class NearbyHeights(heights: FloatArray) {
    val bw = (GW + BLOCK - 1) / BLOCK
    val bh = (GH + BLOCK - 1) / BLOCK
    val top = FloatArray(bw * bh)

    init {
        val bmax = FloatArray(bw * bh)
        for (y in 0 until GH) for (x in 0 until GW) {
            val bi = (y / BLOCK) * bw + x / BLOCK
            bmax[bi] = max(bmax[bi], heights[y * GW + x])
        }
        val rb = ceil(REACH / BLOCK).toInt() + 1
        for (by in 0 until bh) for (bx in 0 until bw) {
            var m = 0f
            for (yy in max(0, by - rb)..min(bh - 1, by + rb)) for (xx in max(0, bx - rb)..min(bw - 1, bx + rb)) m = max(m, bmax[yy * bw + xx])
            top[by * bw + bx] = m
        }
    }

    fun at(x: Int, y: Int) = top[(y / BLOCK) * bw + x / BLOCK]
}

private class Lamp(t: Table, path: Path, ramp: Palette, tableCol: Int) {
    val L = max(LIFT * SS, t.top + 20f * SS)
    val w = Wire(path, ramp, L, t.top)
    private val heights = t.heights
    private val counts = t.counts
    private val near = NearbyHeights(heights)
    val cut = 5f * L
    val cut2 = cut * cut
    val halo = HALO * SS
    val halo2 = halo * halo
    val reach2 = halo2 * 144f
    val th = THICK * SS
    val thru = FloatArray(65) { (1f - ALPHA).pow(it) }
    val wash = GLOW * w.ds * L * 0.5f
    val tr = redf(tableCol)
    val tg = greenf(tableCol)
    val tb = bluef(tableCol)

    fun pixel(x: Int, y: Int): Int {
        val i = y * GW + x
        val px = x + 0.5f
        val py = y + 0.5f
        val h = heights[i]
        val hm = near.at(x, y)
        val dz = L - h
        var er = 0f
        var eg = 0f
        var eb = 0f
        var dmin2 = Float.MAX_VALUE
        var kmin = -1
        for (k in 0..w.n) {
            val dx = w.x[k] - px
            val dy = w.y[k] - py
            val r2 = dx * dx + dy * dy
            if (r2 > cut2) continue
            if (r2 < reach2 && k < w.n) {
                val f2 = distSquaredToSegment(px, py, w.x[k], w.y[k], w.x[k + 1], w.y[k + 1])
                if (f2 < dmin2) {
                    dmin2 = f2
                    kmin = k
                }
            }
            var pass = 1f
            if (hm > h + 0.5f) {
                pass = march(px, py, dx, dy, r2, h, dz, hm)
                if (pass < 0.01f) continue
            }
            val d2 = r2 + dz * dz
            val wnd = 1f - r2 / cut2
            val term = w.pulse[k] * dz / (d2 * sqrt(d2)) * wnd * wnd * pass
            er += term * w.r[k]
            eg += term * w.g[k]
            eb += term * w.b[k]
        }
        val bk = max(kmin, 0)
        val band = if (kmin < 0) 0f else CORE * w.pulse[bk] * halo2 / (dmin2 + halo2)
        return paper(
            er * wash + band * w.r[bk],
            eg * wash + band * w.g[bk],
            eb * wash + band * w.b[bk],
            counts[i],
        )
    }

    private fun march(px: Float, py: Float, dx: Float, dy: Float, r2: Float, h: Float, dz: Float, hm: Float): Float {
        val r = sqrt(r2)
        val xmax = min(REACH, r * (hm - h) / dz)
        var xx = MARCH
        while (xx <= xmax) {
            val sx = (px + dx / r * xx).toInt()
            val sy = (py + dy / r * xx).toInt()
            if (sx < 0 || sy < 0 || sx >= GW || sy >= GH) break
            val z = h + dz * xx / r
            val hh = heights[sy * GW + sx]
            if (hh > z + 0.5f) return thru[min(64, ceil((hh - z) / th).toInt())]
            xx += MARCH
        }
        return 1f
    }

    private fun paper(r: Float, g: Float, b: Float, cards: Int): Int {
        var cr = AMB + r
        var cg = AMB + g
        var cb = AMB + b
        val m = maxOf(cr, cg, cb)
        if (m > 1f) {
            cr /= m
            cg /= m
            cb /= m
        }
        val cov = 1f - thru[min(64, cards)]
        return pack(lerp(tr, cr, cov), lerp(tg, cg, cov), lerp(tb, cb, cov))
    }
}

private class Inks {
    val draw = rng.rndi(COOL) + 1 // Draw even for a fixed palette to preserve existing seeds.
    val palNo = if (PAL >= 0) PAL else draw
    val name = if (palNo > 0) "cool $palNo" else "vivid"
    val set = (if (palNo > 0) Palettes.coolPalette(palNo) else Palettes.colormap032)
        .filter { chromaOf(it) / 255f > 0.12f }.takeIf { it.size > 0 } ?: Palette.of(Color.WHITE)
    val tableCol = lerpColor(0xFF08070B.toInt(), lighten(set.darkest(), 0.3f), TABLE)

    fun ramp() = set.shuffle(rng).take(HUES)
}

private fun tone(ch: Int, m: Float) = lerp(1f, sat(ch / m), TINT)

private fun sat(c: Float): Float = (1f - VIVID + c * VIVID).coerceAtLeast(0f)

private fun pack(r: Float, g: Float, b: Float) = rgb(q(r), q(g), q(b))
private fun q(v: Float) = (v * 255f + 0.5f).toInt().coerceIn(0, 255)
