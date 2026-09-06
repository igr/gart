package cuneus

import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.gfx.Triangle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.lerp
import dev.oblac.gart.gfx.plus
import dev.oblac.gart.gfx.drawTriangle
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.gfx.times
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.PathEffect
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.random.Random

private const val W = 1200
private const val H = 1800

private val OUT = ps("out", "cuneus")
private val SEED = pi("seed", 1)
private val SS = pi("ss", 3, 1..4)
private val COLS = pi("cols", 2, 1..5)
private val HOPS = pi("hops", 4, 2..12)
private val SHIFT = pf("shift", 0.5f, -1f..1f)        // odd columns are reflected and slide up by this much of a hop. 0 meets apex to apex, 1 undoes the mirror
private val PITCH = pf("pitch", 0.03f, 0.01f..0.1f)
private val EYE = pf("eye", 0.28f, 0.1f..0.6f)
private val PINCH = pf("pinch", 0.73f, 0f..1f)        // 0 triangles stay similar, 1 the inner ones go to needles

private val INK = RetroColors.black01
private val PAPER = RetroColors.white01
private val ACCENT = RetroColors.red01

private val RW = W * SS
private val RH = H * SS

private const val JITTER = 0.06f    // each vertex slides this much of the hop. 0.25 reads hand-cut
private const val WANDER = 0.03f    // eye wander per wedge, share of column width

private class Cast(val ys: FloatArray, val wander: List<Point>)

private fun cast(rnd: Random, hop: Float): Cast {
    val mid = RH / 2f
    val ys = FloatArray(HOPS + 5) { i -> mid + (i - 2 - HOPS / 2f) * hop + (rnd.nextFloat() - 0.5f) * 2f * JITTER * hop }
    val wander = List(HOPS + 3) {
        Point((rnd.nextFloat() - 0.5f) * 2f * WANDER, (rnd.nextFloat() - 0.5f) * 2f * WANDER)
    }
    return Cast(ys, wander)
}

private const val LINE = 0.36f  // width

private fun triangles(b1: Point, b2: Point, a: Point, cw: Float, wander: Point): List<Triangle> {
    val e0 = lerp(lerp(b1, b2, 0.5f), a, EYE)
    val e = e0 + wander * cw
    val pitch = PITCH * cw
    val reach = abs(e.x - b1.x)
    val half = abs(b2.y - b1.y) * 0.5f
    val out = ArrayList<Triangle>()
    var k = 0
    while (true) {
        val t = k * pitch / reach
        val u = 1f - t
        if (u * half < 1.5f * LINE * pitch) break   // a triangle smaller than its size is blob
        val g = (1f - PINCH) * u + PINCH * u * u
        val c1 = lerp(b1, e, t)
        val c2 = lerp(b2, e, t)
        val ak = lerp(e, a, g)
        out += Triangle(c1, ak, c2)
        k++
    }
    return out
}

// the page

private const val MARGIN = 0.03425f
private const val TALL = 1.15f      // the zigzag spans this many page heights, centred, so the end wedges run off the frame and the corners stay dense
private const val ROUND = 0.5f      // corner radius, share of the pitch. 0.25 barely shows, 1 blunts the needles
private const val CORE = 5          // triangles of the "brick eye", innermost first

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("cuneus", W, H)
    println(gart)

    val rnd = Random(SEED)
    val big = gart.supersampled(SS)
    val c = big.canvas
    c.clear(INK)

    val cw = (RW - 2f * MARGIN * RW) / COLS
    val hop = TALL * RH / HOPS
    val cast = cast(rnd, hop)
    val paper = strokeOf(PAPER, LINE * PITCH * cw).apply {
        strokeJoin = PaintStrokeJoin.MITER
        strokeMiter = 12f
        pathEffect = PathEffect.makeCorner(ROUND * PITCH * cw)
    }

    // odd columns slide
    val offs = FloatArray(COLS) { col -> if (col % 2 == 1) SHIFT * hop else 0f }
    val cand = ArrayList<Int>()
    for (col in 0 until COLS) for (i in 0 until cast.ys.size - 2) {
        val ey = cast.ys[i + 1] - offs[col]
        if (ey > 0f && ey < RH) cand += col * 1000 + i
    }
    val brick = cand.random(rnd)

    for (col in 0 until COLS) {
        val x0 = MARGIN * RW + col * cw
        val x1 = x0 + cw

        // background
        c.drawRect(Rect(x0, 0f, x1, RH.toFloat()), fillOf(INK))
        val flip = col % 2 == 1

        val v = List(cast.ys.size) { i -> Point(if ((i % 2 == 0) != flip) x0 else x1, cast.ys[i] - offs[col]) }
        for (i in 0 until v.size - 2) {
            val w = cast.wander[i]
            val tris = triangles(v[i], v[i + 2], v[i + 1], cw, Point(if (flip) -w.x else w.x, w.y))
            val hot = col * 1000 + i == brick
            for ((k, tri) in tris.withIndex()) {
                paper.color = if (hot && k >= tris.size - CORE) ACCENT else PAPER
                c.drawTriangle(tri, paper)
            }
        }
    }
    val g = big.downsample(SS)

    gart.saveImage(g, "$OUT.png")
    if (!headless) gart.window().showImage(g)
}
