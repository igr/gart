package craticula

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.darken
import dev.oblac.gart.color.lighten
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.lerp
import dev.oblac.gart.gfx.offset
import dev.oblac.gart.gfx.signedArea
import dev.oblac.gart.gfx.squaredDistanceTo
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.degToRad
import dev.oblac.gart.math.hash01
import dev.oblac.gart.util.Stopwatch
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * craticula. two grids laid over each other, the second one turned a few degrees, and every
 * scrap of page they cut between them drawn as a rounded tile.
 *
 * The turned grid walks out of step with the straight one, so the
 * scraps beat slowly across the page: where the two nearly agree they come out big and almost
 * square, where they fight they shrink to lozenges and seeds.
 *
 * A scrap is always convex which is what makes the rounding one rule with no special cases.
 */

private const val W = 1024
private const val H = 1024
private val SEED = pi("seed", 4)
private val OUT = ps("out", "craticula")

// the two grids
private val COLS = pi("cols", 6, 2..24)
private val ROWS = pi("rows", 6, 2..24)
private val ANGLE = pf("angle", 26f, 0f..90f)
private val RATIO = pf("ratio", 1.2f, 0.3f..3f) // its cell size over the straight ones
private val PHX = pf("phx", 0.17f, -1f..1f)     // and where it sits, in cells. this moves the beat
private val PHY = pf("phy", 0.31f, -1f..1f)

// the tiles
private val GAP = pf("gap", 7f, 0f..30f)
private val ROUND = pf("round", 0.95f, 0f..1f) // 0 leaves the raw polygon, 1 rounds every corner as hard as it goes
private val MINAREA = pf("minarea", 0.012f, 0f..0.2f) // scraps under this fraction of a cell are swept off the page
// minarea is relative to the cell, so it cant say "no mark under n px" - a compact little scrap
// passes it and still lands as a dot.
private val SPECK = pf("speck", 42f, 0f..200f) // a tile narrower than this across is dirt, not a tile
private val MARGIN = pf("margin", 26f, 0f..160f)

private val WAYS = listOf(
    "brick" to Palette(
        0xFFD8202A, 0xFF7A2E39, 0xFF2C3E56, 0xFFE8763A, 0xFFF2E2C4,
        0xFFB98B86, 0xFF5B7290, 0xFF8A6A56, 0xFF8C3A47, 0xFF33465E,
    ),
    "pool" to Palette(
        0xFF16505E, 0xFF2C7A8C, 0xFF7FB6B3, 0xFFE9E3D2, 0xFFD9A441,
        0xFF9C4F32, 0xFF1F3540, 0xFF5E8C7E, 0xFFC96F45, 0xFF3E6B78,
    ),
    "soil" to Palette(
        0xFF6B4A32, 0xFF9C6234, 0xFFC08552, 0xFFE8D2A8, 0xFF7C8A63,
        0xFFAE4630, 0xFF8C6A4E, 0xFFB5793F, 0xFF8C9A78, 0xFFD2A87C,
    ),
    "ink" to Palette(
        0xFF4A525C, 0xFF5E6772, 0xFF737C87, 0xFF8A939D, 0xFF9AA2AC,
        0xFFC6CBD2, 0xFFE9EBEE, 0xFFB0B7BE, 0xFF808991, 0xFFDCDFE3,
    ),
)
private val WAY = pi("way", 0, 0..3)
private val pal = WAYS[WAY].second
private const val PAGE = 0xFF0E1015.toInt()

// the frames
private val rad = degToRad(ANGLE)
private val sx = (W - 2f * MARGIN) / COLS
private val sy = (H - 2f * MARGIN) / ROWS
private val bw = sx * RATIO
private val bh = sy * RATIO
private val phx = PHX * bw
private val phy = PHY * bh

// page <-> the turned grids own frame, spun about the middle of the page
private fun toGrid(x: Float, y: Float): Point {
    val r = Vec2(x - W * 0.5f, y - H * 0.5f).rotate(-rad)
    return Point(r.x + phx, r.y + phy)
}

private fun toPage(p: Point): Point {
    val r = Vec2(p.x - phx, p.y - phy).rotate(rad)
    return Point(r.x + W * 0.5f, r.y + H * 0.5f)
}

// one area between the grid lines, with both the cells it came out of
private class Scrap(val poly: List<Point>, val ai: Int, val aj: Int, val bi: Int, val bj: Int)

// keep whatever side of the line reads >= 0. convex in, convex out
private inline fun half(poly: List<Point>, side: (Point) -> Float): List<Point> {
    if (poly.isEmpty()) return poly
    val out = ArrayList<Point>(poly.size + 2)
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[(i + 1) % poly.size]
        val da = side(a)
        val db = side(b)
        if (da >= 0f) out += a
        if ((da >= 0f) != (db >= 0f)) {
            val t = da / (da - db)
            out += lerp(a, b, t)
        }
    }
    return out
}

private fun apart(a: Point, b: Point) = abs(a.x - b.x) > 1e-3f || abs(a.y - b.y) > 1e-3f

private fun tidy(poly: List<Point>): List<Point> {
    val out = ArrayList<Point>(poly.size)
    for (p in poly) if (out.isEmpty() || apart(out.last(), p)) out += p
    while (out.size > 1 && !apart(out.first(), out.last())) out.removeAt(out.size - 1)
    return out
}

// Every scrap is one straight cell cut by one turned cell, so there is no arrangement to walk:
// take each straight cell into the turned grids frame, see which of its cells it can possibly
// touch, and clip. Each scrap turns up exactly once and knows both parents.
private fun scraps(): List<Scrap> {
    val out = mutableListOf<Scrap>()
    val least = MINAREA * sx * sy
    for (aj in 0 until ROWS) for (ai in 0 until COLS) {
        val x0 = MARGIN + ai * sx
        val y0 = MARGIN + aj * sy
        val cell = listOf(
            toGrid(x0, y0), toGrid(x0 + sx, y0), toGrid(x0 + sx, y0 + sy), toGrid(x0, y0 + sy),
        )
        val bi0 = floor(cell.minOf { it.x } / bw).toInt()
        val bi1 = floor(cell.maxOf { it.x } / bw).toInt()
        val bj0 = floor(cell.minOf { it.y } / bh).toInt()
        val bj1 = floor(cell.maxOf { it.y } / bh).toInt()
        for (bj in bj0..bj1) for (bi in bi0..bi1) {
            val l = bi * bw
            val t = bj * bh
            var p = half(cell) { it.x - l }
            p = half(p) { l + bw - it.x }
            p = half(p) { it.y - t }
            p = half(p) { t + bh - it.y }
            val q = tidy(p)
            if (q.size >= 3 && signedArea(q) >= least) out += Scrap(q.map(::toPage), ai, aj, bi, bj)
        }
    }
    return out
}

// the tile

private fun inset(raw: List<Point>, d: Float): List<Point>? {
    val poly = if (signedArea(raw) < 0f) raw.reversed() else raw
    val n = poly.size
    val dir = ArrayList<Vec2>(n)
    val at = ArrayList<Point>(n)
    for (i in 0 until n) {
        val a = poly[i]
        val b = poly[(i + 1) % n]
        val e = Vec2(b.x - a.x, b.y - a.y)
        if (e.magnitude < 1e-4f) return null
        val u = e.normalize()
        dir += u
        at += a.offset(-u.y * d, u.x * d) // the edge, moved in along its normal
    }
    val out = ArrayList<Point>(n)
    for (i in 0 until n) {
        val j = (i + n - 1) % n
        val den = dir[j].cross(dir[i])
        if (abs(den) < 1e-5f) return null   // the two edges are in line, no corner to keep
        val e = Vec2(at[i].x - at[j].x, at[i].y - at[j].y)
        val t = e.cross(dir[i]) / den
        out += at[j].offset(dir[j] * t)
    }
    return if (signedArea(out) > 1f) out else null // it ate itself
}

// Corners off with a circular arc
private fun roundedPath(poly: List<Point>, round: Float): Path {
    val n = poly.size
    val enter = ArrayList<Point>(n)
    val leave = ArrayList<Point>(n)
    val weight = FloatArray(n)
    for (i in 0 until n) {
        val v = poly[i]
        val p = poly[(i + n - 1) % n]
        val q = poly[(i + 1) % n]
        val e1 = Vec2(p.x - v.x, p.y - v.y)
        val e2 = Vec2(q.x - v.x, q.y - v.y)
        val u1 = e1.normalize()
        val u2 = e2.normalize()
        val bend = acos(u1.dot(u2).coerceIn(-1f, 1f)) * 0.5f
        val t = round * min(e1.magnitude, e2.magnitude) * 0.5f
        enter += v.offset(u1 * t)
        leave += v.offset(u2 * t)
        weight[i] = sin(bend)
    }
    val b = PathBuilder().moveTo(enter[0])
    for (i in 0 until n) {
        b.conicTo(poly[i], leave[i], weight[i])
        b.lineTo(enter[(i + 1) % n])
    }
    return b.closePath().detach()
}

private val hues = IntArray(COLS * ROWS).also { g ->
    for (j in 0 until ROWS) for (i in 0 until COLS) {
        var k = (hash01(i, j, 3, SEED) * pal.size).toInt() % pal.size
        val left = if (i > 0) g[j * COLS + i - 1] else -1
        val up = if (j > 0) g[(j - 1) * COLS + i] else -1
        while (k == left || k == up) k = (k + 1) % pal.size
        g[j * COLS + i] = k
    }
}

// the widest the shape gets, corner to corner
private fun span(poly: List<Point>): Float {
    var m = 0f
    for (i in poly.indices) for (j in i + 1 until poly.size) {
        m = max(m, poly[i].squaredDistanceTo(poly[j]))
    }
    return sqrt(m)
}

private fun colorOf(s: Scrap): Int {
    val base = pal[hues[s.aj * COLS + s.ai]]
    val shade = (hash01(s.bi, s.bj, 11, SEED) - 0.5f) * 0.26f
    return if (shade > 0f) lighten(base, shade) else darken(base, -shade)
}

private fun Canvas.tile(s: Scrap): Boolean {
    val poly = inset(s.poly, GAP * 0.5f) ?: return false
    if (span(poly) < SPECK) return false
    val path = roundedPath(poly, ROUND)
    fillOf(colorOf(s)).use { drawPath(path, it) }
    path.close()
    return true
}

fun main(args: Array<String>) {
    val gart = Gart.of("craticula", W, H)
    val sw = Stopwatch()
    val g = gart.gartvas()
    g.canvas.clear(PAGE)
    val all = scraps()
    val drawn = all.count { g.canvas.tile(it) }
    gart.saveImage(g, OUT.ensureExtension())
    println("craticula: seed=$SEED, way=${WAYS[WAY].first}, ${COLS}x$ROWS, angle=$ANGLE, ratio=$RATIO, ${drawn} of ${all.size} scraps, ${sw.ms}ms")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
