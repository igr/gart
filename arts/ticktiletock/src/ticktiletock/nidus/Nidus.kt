package ticktiletock.nidus

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.pixels.boxDownsample
import dev.oblac.gart.pixels.labelRegions
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private val seed = pi("seed", 15)
private val cols = pi("cols", 5, 1..16)  // big cells across
private val depth = pi("depth", 3, 0..5) // how many grids go inside each other
private val kids = pi("kids", 1, 0..8)   // rects in each grid
private val ss = pi("ss", 3, 1..4)
private val out = ps("out", "nidus")

private const val W = 1024
private const val PAPER = BgColors.warmPaper
private const val INK = BgColors.inkBlack

fun main(args: Array<String>) {
    val gart = Gart.of("nidus", W, W)
    val rnd = Random(seed)
    val patches = mutableListOf<Patch>()
    grow(0, 0, 0, cols, cols, rnd, patches)

    val big = gart.supersampled(ss)
    val n = W * ss
    draw(big.canvas, patches, n.toFloat() / cols)

    val px = Gartmap(big).use { it.pixels }
    val ink = BooleanArray(n * n) { px[it] == INK }
    val regions = labelRegions(ink, n, n)
    val edge = regions.touchesEdge()
    val colour = IntArray(regions.count) { if (edge[it]) PAPER else Palettes.cool9.random(rnd) }
    val samples = IntArray(n * n) { if (ink[it]) INK else colour[regions.label[it]] }

    val g = gart.gartvas()
    Gartmap(g).use { m ->
        boxDownsample(samples, ss, m)
        m.drawToCanvas()
    }
    gart.saveImage(g, out.ensureExtension())
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}

// the grids

private class Patch(val level: Int, val x: Int, val y: Int, val w: Int, val h: Int, val coin: BooleanArray) {
    val kids = mutableListOf<Patch>()
}

private const val SIZE = 0.5
private const val SPREAD = 8

private fun grow(level: Int, x: Int, y: Int, w: Int, h: Int, rnd: Random, all: MutableList<Patch>): Patch {
    val p = Patch(level, x, y, w, h, BooleanArray(w * h) { rnd.nextBoolean() })
    all += p
    if (level == depth) return p
    // one cell off the sides of the parent. the page edge has nothing to clash with, so no gap there
    val page = cols shl level
    val left = if (x == 0) 0 else 1
    val top = if (y == 0) 0 else 1
    val roomW = w - left - (if (x + w == page) 0 else 1)
    val roomH = h - top - (if (y + h == page) 0 else 1)
    if (roomW < 1 || roomH < 1) return p
    repeat(kids) {
        var best = IntArray(4)
        var far = -1
        repeat(SPREAD) {
            val kw = 1 + rnd.nextInt(max(1, (roomW * SIZE).toInt()))
            val kh = 1 + rnd.nextInt(max(1, (roomH * SIZE).toInt()))
            val kx = x + left + rnd.nextInt(roomW - kw + 1)
            val ky = y + top + rnd.nextInt(roomH - kh + 1)
            // centres in half cells of this level, so it stays in ints
            val cx = 2 * kx + kw
            val cy = 2 * ky + kh
            // the page edge counts as a neighbour too: a mirror copy of the rect behind it
            val side = 2 * minOf(cx, 2 * page - cx, cy, 2 * page - cy)
            val d = min(side * side, p.kids.minOfOrNull {
                val dx = cx - it.x - it.w / 2
                val dy = cy - it.y - it.h / 2
                dx * dx + dy * dy
            } ?: Int.MAX_VALUE)
            if (d > far) {
                best = intArrayOf(kx, ky, kw, kh)
                far = d
            }
        }
        p.kids += grow(level + 1, 2 * best[0], 2 * best[1], 2 * best[2], 2 * best[3], rnd, all)
    }
    return p
}

// drawing

private fun bg(level: Int) = if (level % 2 == 0) PAPER else INK
private fun fg(level: Int) = if (level % 2 == 0) INK else PAPER

// no aa!!! every sample comes out pure ink or pure paper, the colouring needs that
private fun flatColor(color: Int) = Paint().apply {
    this.color = color
    isAntiAlias = false
}

private fun draw(c: Canvas, patches: List<Patch>, s0: Float) {
    // the cells, big grids first. a kid paints over the part of the parent it sits on
    for (p in patches.sortedBy { it.level }) {
        val s = s0 / (1 shl p.level)
        val ink = flatColor(fg(p.level))
        val hole = flatColor(bg(p.level))
        c.drawRect(Rect.makeXYWH(p.x * s, p.y * s, p.w * s, p.h * s), hole)
        for (j in 0 until p.h) for (i in 0 until p.w) {
            val x0 = (p.x + i) * s
            val y0 = (p.y + j) * s
            val x1 = (p.x + i + 1) * s
            val y1 = (p.y + j + 1) * s
            c.save()
            c.clipRect(Rect.makeLTRB(x0, y0, x1, y1))
            if (p.coin[j * p.w + i]) {
                ring(c, x1, y0, s, ink, hole)
                ring(c, x0, y1, s, ink, hole)
            } else {
                ring(c, x0, y0, s, ink, hole)
                ring(c, x1, y1, s, ink, hole)
            }
            c.restore()
        }
    }

    for (p in patches.sortedByDescending { it.level }) {
        val s = s0 / (1 shl p.level)
        val wing = flatColor(bg(p.level))
        val finer = patches.filter { it.level == p.level + 1 }
        for (j in 0 until p.h) for (i in 0 until p.w) {
            val fx = 2 * (p.x + i)
            val fy = 2 * (p.y + j)
            if (finer.any { fx in it.x until it.x + it.w && fy in it.y until it.y + it.h }) continue
            val x0 = (p.x + i) * s
            val y0 = (p.y + j) * s
            val x1 = (p.x + i + 1) * s
            val y1 = (p.y + j + 1) * s
            c.drawCircle(x0, y0, s / 6f, wing)
            c.drawCircle(x1, y0, s / 6f, wing)
            c.drawCircle(x0, y1, s / 6f, wing)
            c.drawCircle(x1, y1, s / 6f, wing)
        }
    }
}

private fun ring(c: Canvas, x: Float, y: Float, s: Float, ink: Paint, hole: Paint) {
    c.drawCircle(x, y, 2 * s / 3f, ink)
    c.drawCircle(x, y, s / 3f, hole)
}
