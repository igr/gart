package lines.cincinnus

import dev.oblac.gart.Gart
import dev.oblac.gart.brush.Bristles
import dev.oblac.gart.brush.Spine
import dev.oblac.gart.brush.SpinePoint
import dev.oblac.gart.brush.drawBristles
import dev.oblac.gart.color.BwColors
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.alpha
import dev.oblac.gart.flow2.Integrator
import dev.oblac.gart.flow2.VectorField
import dev.oblac.gart.flow2.plus
import dev.oblac.gart.flow2.streamlines
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.between
import dev.oblac.gart.math.dist
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.seededRandom
import dev.oblac.gart.math.wrapPi
import dev.oblac.gart.noise.noise
import dev.oblac.gart.util.Stopwatch
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * cincinnus, the short way. a flow field of whirls with their eyes off the page, the lib lays
 * evenly spaced streamlines over it, each line is cut into strokes, and each stroke gets the
 * crinis brush. the brush is 2.5 gaps wide, so nearly no ground shows.
 *
 * the strokes that bend the most go on top. light at the top of the page, deep at the bottom.
 */
private const val N = 2000
private val SEED = pi("seed", 1)
private val OUT = ps("out", "cincinnus")
private val SS = pi("ss", 3, 1..4)

private val EYES = pi("eyes", 5, 1..12) // whirls round the page
private val GAP = pf("gap", 110f, 30f..400f) // px between the lines. the brush is 2.5 of it wide
private val DOTS = pi("dots", 4000, 0..20000) // specks of dirt

private const val REACH = 0.3f // how far out a whirl holds, share of the side
private const val DS = 3f // px between spine points
private const val DRY = 4f // the paint holds nearly to the end, then goes quick
private const val DEEP = 0.25f // the top strokes this much lighter than the ones under them
private const val FALL = 0.8f // light at the top of the page to deep at the bottom, on the ramp
private const val BAND = 3f // the gradient goes as u^3: a wide even middle, the change near the edges. 1 is linear

private val CRIMSON = Palette(
    0xFF1A0308, 0xFF3D0713, 0xFF6E0F22, 0xFFA3142F,
    0xFFCB1B45, 0xFFE0485A, 0xFFF08A80, 0xFFF9C9B6,
)
private val BRUSH = Bristles(CRIMSON, light = Vec2(-0.21f, -0.31f))
private val PAGE = Rect(-20f, -20f, N + 20f, N + 20f)

private class Swash(val spine: Spine, val bend: Float, val head: (Float) -> Float)

private fun load(t: Float) = 1f - t.pow(DRY)

// the field

private fun field(rnd: Random): VectorField {
    while (true) {
        val eyes = mutableListOf<Point>()
        repeat(EYES) {
            eyes += List(12) { offPage(rnd) }.maxBy { q -> eyes.minOfOrNull { dist(it, q) } ?: 0f }
        }
        // negative spin is counter clockwise on the screen
        val f = eyes.map {
            VectorField.vortex(it.x, it.y, spin = -rnd.between(0.6f, 1.4f), reach = N * REACH * rnd.between(0.7f, 1.3f))
        }.reduce { a, b -> a + b }
        if (!hasEye(f)) return f
    }
}

// an eye is where the flow turns once round a small cell. walk round each 40 px cell and add up
// how far the flow turns: a full turn is an eye, a saddle turns the other way
private fun hasEye(f: VectorField): Boolean {
    val cell = 40f
    fun angle(x: Float, y: Float) = f(Point(x, y)).let { atan2(it.y, it.x) }
    fun turn(a: Float, b: Float) = wrapPi(b - a)
    for (j in -2 until (N / cell).toInt() + 2) for (i in -2 until (N / cell).toInt() + 2) {
        val x = i * cell
        val y = j * cell
        val a = listOf(angle(x, y), angle(x + cell, y), angle(x + cell, y + cell), angle(x, y + cell))
        val sum = turn(a[0], a[1]) + turn(a[1], a[2]) + turn(a[2], a[3]) + turn(a[3], a[0])
        if (sum < -PI) return true // the walk is clockwise on the screen, so an eye adds up to -2pi
    }
    return false
}

private fun offPage(rnd: Random): Point {
    while (true) {
        val p = Point(N * rnd.between(-0.4f, 1.4f), N * rnd.between(-0.4f, 1.4f))
        if (max(max(-p.x, p.x - N), max(-p.y, p.y - N)) > 0.08f * N) return p
    }
}

// strokes

// where each bristle lands, px from the start: a slant, blunt or long, in clumps
private fun headOf(half: Float, rnd: Random): (Float) -> Float {
    val long = rnd.between(0.55f, 1.2f)
    val tilt = rnd.between(-0.6f, 0.6f) * long
    val lumps = rnd.between(1.5f, 4f)
    val ph = rnd.between(0f, 100f)
    val salt = rnd.nextInt()
    fun raw(t: Float) = half * (0.8f * long * (1f - sqrt(1f - t * t)) + tilt * t +
        0.18f * noise(t * lumps + ph) + 0.06f * hash01((t * 1e4f).toInt(), salt, 0))
    val low = (0..64).minOf { raw(-1f + it / 32f) }
    return { t -> raw(t) - low }
}

// radius of the bend at each point, over 4 steps each way
private fun bends(pts: List<Point>): FloatArray {
    val n = pts.size
    fun heading(i: Int) = atan2(pts[i + 1].y - pts[i].y, pts[i + 1].x - pts[i].x)
    return FloatArray(n) { k ->
        val i0 = (k - 4).coerceIn(0, n - 2)
        val i1 = (k + 4).coerceIn(0, n - 2)
        val turn = wrapPi(heading(i1) - heading(i0))
        if (i1 == i0 || abs(turn) < 1e-4f) 1e6f else (i1 - i0) * DS / abs(turn)
    }
}

private fun strokeOf(pts: List<Point>, hw: Float, rnd: Random): Swash {
    val n = pts.size

    val fit = bends(pts)
    val w = (2f * hw / DS).toInt()
    val lows = FloatArray(n) { k -> (max(0, k - w)..min(n - 1, k + w)).minOf { 0.8f * fit[it] } }
    val ph = rnd.between(0f, 100f)
    val points = List(n) { k ->
        val a = pts[(k - 1).coerceAtLeast(0)]
        val b = pts[(k + 1).coerceAtMost(n - 1)]
        val l = hypot(b.x - a.x, b.y - a.y)
        val t = k / (n - 1f)
        val room = (max(0, k - w)..min(n - 1, k + w)).map { lows[it] }.average().toFloat()
        val half = hw * (0.75f + 0.25f * load(t)) * (1f + 0.2f * noise(t * 3f + ph))
        SpinePoint(pts[k], Vec2(-(b.y - a.y) / l, (b.x - a.x) / l), k * DS, min(half, room))
    }
    return Swash(Spine(points, DS), fit.sumOf { 1.0 / it }.toFloat() / n, headOf(hw, rnd))
}

private fun cut(line: List<Point>, hw: Float, rnd: Random): List<Swash> {
    val out = mutableListOf<Swash>()
    var from = 0
    var short = 200f // the first one any length, or the lines cut in step
    while (from < line.size - 20) {
        val last = min(line.size, from + (1600f / DS).toInt())
        val ends = min(last, from + (short / DS).toInt())..last
        var to = ends.random(rnd)

        // more of them end on the lower half: one that would end up top ends low if it can
        fun at(i: Int) = line[min(i, line.size - 1)]
        val low = ends.filter { it < line.size && at(it).y > N / 2f && at(it).y < N - 60f && at(it).x in 60f..N - 60f }
        if (at(to).y < N / 2f && low.isNotEmpty()) to = low.random(rnd)
        short = 600f
        out += strokeOf(line.subList(from, to), hw, rnd)
        if (to == line.size) break
        from = (to - (rnd.between(150f, 350f) / DS).toInt()).coerceAtLeast(from + 10)
    }
    return out
}

// draw

// the two crinis shadows, a wide soft one and a tight one
private fun shadow(c: Canvas, path: Path) {
    for ((sigma, a, off) in listOf(Triple(60f, 0xC0, 20f), Triple(10f, 0xD0, 5f))) {
        val p = paint().apply {
            color = CRIMSON.sample(0f).alpha(a)
            maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, sigma)
        }
        c.save()
        c.translate(off * 0.5f, off)
        c.drawPath(path, p)
        c.restore()
    }
}

fun main(args: Array<String>) {
    val gart = Gart.of("cincinnus", N, N)
    val sw = Stopwatch()
    val rnd = seededRandom(SEED)

    val flow = field(rnd)
    val hw = GAP * 1.25f // 0.75 left 2-4% bare where lines stop, 1.25 about 0.3%
    val lines = flow.streamlines(gart.d, rnd, dSep = GAP, step = DS, minLength = 300f, margin = 3 * hw, rk = Integrator.RK4)

    // straight ones down first, the most bent on top
    val strokes = lines.flatMap { cut(it.points, hw, rnd) }.sortedBy { it.bend }

    val big = gart.supersampled(SS)
    val c = big.canvas
    c.clear(CRIMSON.sample(0.02f))
    c.scale(SS.toFloat(), SS.toFloat())
    val hairs = Random(SEED * 7919 + 1)
    val dotRnd = Random(SEED * 7919 + 8)
    for ((i, s) in strokes.withIndex()) {

        // dirt goes in between the strokes, more of it up top or the strokes cover it all
        repeat(DOTS / strokes.size) {
            val r = 0.8f + 6f * dotRnd.nextFloat().pow(3)
            if (dotRnd.nextFloat() < i / strokes.size.toFloat()) {
                c.drawCircle(N * dotRnd.nextFloat(), N * dotRnd.nextFloat(), r, fillOf(BwColors.richBlack))
            }
        }
        val depth = i / max(strokes.size - 1f, 1f)

        // light at the top, deep at the bottom. a wider spread and noise patches read as random
        val y = s.spine.points[s.spine.size / 2].at.y / N
        val u = 2f * y - 1f // -1 at the top, 1 at the bottom
        val tone = rnd.between(0.4f, 0.7f) + DEEP * (depth.pow(1.2f) - 0.5f) - FALL / 2 * sign(u) * abs(u).pow(BAND)
        shadow(c, BRUSH.outline(s.spine, ::load, head = s.head))
        c.drawBristles(s.spine, BRUSH, tone, hairs, i, PAGE, ::load, head = s.head)
    }

    val g = big.downsample(SS)
    addGrain(g, 0.14f, SEED)
    gart.saveImage(g, OUT.ensureExtension())
    println("cincinnus simple: seed=$SEED, lines=${lines.size}, strokes=${strokes.size}, ${sw.ms}ms")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
