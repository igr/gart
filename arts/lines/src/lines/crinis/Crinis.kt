package lines.crinis

import dev.oblac.gart.Gart
import dev.oblac.gart.brush.Bristles
import dev.oblac.gart.brush.Spine
import dev.oblac.gart.brush.SpinePoint
import dev.oblac.gart.brush.drawBristles
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.alpha
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.ISO_RATIO
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.between
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.seededRandom
import dev.oblac.gart.math.shortSide
import dev.oblac.gart.noise.noise
import dev.oblac.gart.util.Stopwatch
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * crinis - hair, laid down band over band. a band is one curve and all its hairs ride it, each
 * at its own offset, so its a brush with a hundred or two bristles. the soft shadow a band
 * throws on the ones under it is what makes the layers read.
 *
 * I walk every curve by its bend: two slow waves of curvature, and a lean that winds it round
 * one eye. the next band starts where the fewest went so far, and the page fills up by itself.
 * the brush is the lib Bristles now, this file only says where the bands go.
 */
private const val H = 2000
private val W = shortSide(H, ISO_RATIO)
private val SEED = pi("seed", 1)
private val OUT = ps("out", "crinis")
private val SS = pi("ss", 3, 1..4)

private val BANDS = pi("bands", 16, 1..80) // 8 still covers the page, 32 is busy
private val BAND = pf("band", 0.38f, 0.05f..0.8f) // width, share of W. 0.22 is spaghetti
private val PITCH = pf("pitch", 4.6f, 1f..20f) // px between hairs. 3.4 went to sheen at screen size, 5 to stripes
private val TURN = pf("turn", 1f, 0f..3f) // 0.5 gives long calm sweeps, 1.4 coils
private val SWIRL = pf("swirl", 0.6f, 0f..1f) // 0 is a heap of spaghetti, 1 coils them all round the eye

private const val DS = 3f // walk step, px
private const val MAXLEN = 7000f // just a guard

private val INK = Palette(
    0xFF011132, 0xFF092244, 0xFF19365A, 0xFF2B4C71, 0xFF436589,
    0xFF5E80A2, 0xFF7A9CBB, 0xFF9AB9D7, 0xFFB9D7F1, 0xFFD6E8F7,
)
private val BRISTLES = Bristles(INK, pitch = PITCH)
private val PAGE = Rect(-20f, -20f, W + 20f, H + 20f)

private class Band(val spine: Spine, val tone: Float, val salt: Int)
private class Step(val p: Point, val th: Float, val s: Float)
private class Eye(val at: Point, val spin: Float)

// the walk

private fun band(rnd: Random, eye: Eye, start: Point, salt: Int, depth: Float): Band {
    val hw = W * BAND * rnd.between(0.6f, 1.3f) / 2
    // the inner edge folds over once the radius drops under the half width.
    // 1.45 not 1, the band fans out up to 35% on top of it
    val kmax = 1f / (hw * 1.45f)
    val p1 = H * rnd.between(0.8f, 1.8f)
    val f1 = rnd.between(0f, TAUf)
    val a1 = rnd.between(0.3f, 1.1f) * TURN
    val p2 = H * rnd.between(0.35f, 0.6f) // and a shorter weak one on top
    val f2 = rnd.between(0f, TAUf)
    val a2 = rnd.between(0f, 0.2f) * TURN

    // round the eye: start tangent to it, then lean as much as the orbit needs
    val r = hypot(start.x - eye.at.x, start.y - eye.at.y).coerceAtLeast(hw * 1.5f)
    val orbit = eye.spin * (hw * 1.45f) / r * rnd.between(0.5f, 1.1f)
    val bias = lerp(rnd.between(-0.3f, 0.3f), orbit, SWIRL) * TURN
    fun k(s: Float) = kmax * (a1 * sin(TAUf * s / p1 + f1) + a2 * sin(TAUf * s / p2 + f2) + bias).coerceIn(-1f, 1f)
    val wp = rnd.between(0f, 100f)
    val th0 = atan2(start.y - eye.at.y, start.x - eye.at.x) + eye.spin * TAUf / 4 +
        (1f - SWIRL * 0.8f) * rnd.between(-TAUf / 2, TAUf / 2)
    val m = hw * 1.3f + 80f // this far off the page a band still counts, its edge can poke back in
    fun inside(p: Point) = p.x > -m && p.x < W + m && p.y > -m && p.y < H + m

    // a band that comes back over itself stops there, like a brush lifting. drawn hair by
    // hair, the two passes wove into each other like a basket
    fun crosses(p: Point, s: Float, pts: List<Step>): Boolean {
        val gap = hw * 2.1f
        for (i in pts.indices step 2) {
            val q = pts[i]
            if (abs(q.s - s) < hw * 3f) continue
            val dx = q.p.x - p.x
            val dy = q.p.y - p.y
            if (dx * dx + dy * dy < gap * gap) return true
        }
        return false
    }

    val fwd = mutableListOf<Step>()
    fun walk(dir: Float): List<Step> {
        var p = start
        var th = th0
        var s = 0f
        var outside = 0f
        var i = 0
        val out = mutableListOf<Step>()
        while (abs(s) < MAXLEN && outside < 500f) { // 500 px out it can still turn back
            th += dir * k(s + dir * DS / 2) * DS
            p = Point(p.x + dir * cos(th) * DS, p.y + dir * sin(th) * DS)
            s += dir * DS
            // every 4th step is plenty, thats 12 px and the gap is hundreds
            if (i++ % 4 == 0 && inside(p) && (crosses(p, s, out) || crosses(p, s, fwd))) break
            out += Step(p, th, s)
            outside = if (inside(p)) 0f else outside + DS
        }
        val last = out.indexOfLast { inside(it.p) }
        return out.subList(0, (last + 2).coerceAtMost(out.size))
    }

    fwd += walk(1f)
    val steps = buildList {
        addAll(walk(-1f).reversed())
        add(Step(start, th0, 0f))
        addAll(fwd)
    }
    // the arc goes negative behind the start. the width fans out, pinches in
    val spine = Spine(steps.map {
        SpinePoint(it.p, Vec2(-sin(it.th), cos(it.th)), it.s, hw * (1f + 0.35f * noise(it.s / 1100f + wp)))
    }, DS)
    // later bands a touch lighter. 0.12 lit the top one like a spotlight
    return Band(spine, rnd.between(0.42f, 0.78f) + 0.06f * (depth - 0.5f), salt)
}

// where bands are

// the next band starts in a cell the fewest bands went over so far
private class Cover(val cell: Float) {
    val cw = (W / cell).toInt() + 1
    val ch = (H / cell).toInt() + 1
    val count = IntArray(cw * ch)

    fun pick(rnd: Random): Point {
        val lo = count.min()
        val cells = count.indices.filter { count[it] == lo }
        val i = cells[rnd.nextInt(cells.size)]
        return Point((i % cw + rnd.nextFloat()) * cell, (i / cw + rnd.nextFloat()) * cell)
    }

    // every 5th point of the spine, 15 px apart, is fine for 40 px cells
    fun add(b: Band) {
        val seen = BooleanArray(count.size)
        for (k in 0 until b.spine.size step 5) {
            val p = b.spine.points[k].at
            val r = b.spine.points[k].half
            val i0 = ((p.x - r) / cell).toInt().coerceIn(0, cw - 1)
            val i1 = ((p.x + r) / cell).toInt().coerceIn(0, cw - 1)
            val j0 = ((p.y - r) / cell).toInt().coerceIn(0, ch - 1)
            val j1 = ((p.y + r) / cell).toInt().coerceIn(0, ch - 1)
            for (j in j0..j1) for (i in i0..i1) {
                val dx = (i + 0.5f) * cell - p.x
                val dy = (j + 0.5f) * cell - p.y
                if (dx * dx + dy * dy < r * r) seen[j * cw + i] = true
            }
        }
        for (i in seen.indices) if (seen[i]) count[i]++
    }
}

// draw

private val shadowInk = INK.sample(0f)

// two shadows, a wide soft one and a tight one where the band touches.
private fun shadow(c: Canvas, path: Path) {
    for ((sigma, a, off) in listOf(Triple(60f, 0xC0, 20f), Triple(10f, 0xD0, 5f))) {
        val p = paint().apply {
            color = shadowInk.alpha(a)
            maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, sigma)
        }
        c.save()
        c.translate(off * 0.5f, off)
        c.drawPath(path, p)
        c.restore()
    }
}

fun main(args: Array<String>) {
    val gart = Gart.of("crinis", W, H)
    val sw = Stopwatch()
    val rnd = seededRandom(SEED)

    val eye = Eye(Point(W * rnd.between(0.3f, 0.7f), H * rnd.between(0.3f, 0.7f)), if (rnd.nextBoolean()) 1f else -1f)
    val cover = Cover(40f)
    val bands = List(BANDS) { i ->
        val p = cover.pick(rnd)
        band(rnd, eye, p, i + SEED * 101, i / max(BANDS - 1f, 1f)).also { cover.add(it) }
    }

    val big = gart.supersampled(SS)
    val c = big.canvas
    c.clear(INK.sample(0.02f))
    c.scale(SS.toFloat(), SS.toFloat())
    val hairRnd = Random(SEED * 7919 + 1) // hairs get their own rng
    for (b in bands) {
        shadow(c, BRISTLES.outline(b.spine))
        c.drawBristles(b.spine, BRISTLES, b.tone, hairRnd, b.salt, PAGE)
    }

    val g = big.downsample(SS)
    addGrain(g, 0.14f, SEED)
    gart.saveImage(g, OUT.ensureExtension())
    println("crinis: seed=$SEED, bands=$BANDS, uncovered=${cover.count.count { it == 0 }}, ${sw.ms}ms")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
