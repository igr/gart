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
import dev.oblac.gart.flow2.trace
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
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.between
import dev.oblac.gart.math.dist
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.seededRandom
import dev.oblac.gart.math.smoothstep
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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

private const val N = 2000
private val SEED = pi("seed", 3)
private val OUT = ps("out", "cincinnus-original")
private val SS = pi("ss", 3, 1..4)

private val STROKES = pi("strokes", 60, 1..300)
private val EYES = pi("eyes", 5, 1..12)
private val DOTS = pi("dots", 4000, 0..20000)
private val ROUGH = pf("rough", 1f, 0f..2f)

private const val REACH = 0.3f
private const val FAT = 0.4f
private const val SHUT = 0.9f
private const val LONG = 1600f
private const val DRY = 4f
private const val DEEP = 0.4f
private const val DS = 3f
private const val RISE = 2.5f
private const val PRESS = 0.2f
private const val SHORT = 0.5f
private const val SPREAD = 12
private const val HWMAX = 120f
private const val HOOK = 1f
private const val HWMIN = 30f
private const val RMIN = 25f
private const val MAXLEN = 9000f
private const val MARGIN = 500f

private val CRIMSON = Palette(
    0xFF1A0308, 0xFF3D0713, 0xFF6E0F22, 0xFFA3142F,
    0xFFCB1B45, 0xFFE0485A, 0xFFF08A80, 0xFFF9C9B6,
)
private val LIGHT = Vec2(-0.21f, -0.31f)
private val BRUSH = Bristles(CRIMSON, light = LIGHT)
private val PAGE = Rect(-20f, -20f, N + 20f, N + 20f)

private class Stroke(val spine: Spine, val size: Float, val salt: Int, val head: (Float) -> Float)

private fun load(t: Float) = 1f - t.pow(DRY)

private fun headOf(half: Float, rnd: Random): (Float) -> Float {
    val lean = rnd.between(-0.6f, 0.6f)
    val long = lerp(1f, rnd.between(0.55f, 1.2f), ROUGH)
    val tilt = ROUGH * lean * long
    val lumps = rnd.between(1.5f, 4f)
    val ph = rnd.between(0f, 100f)
    val salt = rnd.nextInt()
    fun raw(t: Float) = half * (0.8f * long * (1f - sqrt(1f - t * t)) + tilt * t +
        ROUGH * (0.18f * noise(t * lumps + ph) + 0.06f * hash01((t * 1e4f).toInt(), salt, 0)))
    val low = (0..64).minOf { raw(-1f + it / 32f) }
    return { t -> raw(t) - low }
}

private class Flow(val field: VectorField, val eyes: List<Point>)

private fun offPage(rnd: Random): Point {
    while (true) {
        val p = Point(N * rnd.between(-0.4f, 1.4f), N * rnd.between(-0.4f, 1.4f))
        if (max(max(-p.x, p.x - N), max(-p.y, p.y - N)) > 0.08f * N) return p
    }
}

private fun field(rnd: Random, shape: Random): Flow {
    val eyes = mutableListOf<Point>()
    repeat(EYES) {
        shape.nextFloat()
        eyes += List(SPREAD) { offPage(shape) }.maxBy { q -> eyes.minOfOrNull { dist(it, q) } ?: 0f }
        shape.nextFloat()
        shape.nextFloat()
    }
    val f = eyes.map {
        VectorField.vortex(it.x, it.y, spin = -rnd.between(0.6f, 1.4f), reach = N * REACH * rnd.between(0.7f, 1.3f))
    }.reduce { a, b -> a + b }
    return Flow(f, eyes)
}

private fun aside(f: VectorField, p: Point): Point {
    val v = f(p)
    val l = hypot(v.x, v.y)
    if (l == 0f) return Point(p.x + 1.5f * RMIN, p.y)
    return Point(p.x - v.y / l * 1.5f * RMIN, p.y + v.x / l * 1.5f * RMIN)
}

private fun bends(pts: List<Point>): FloatArray {
    val n = pts.size
    fun heading(i: Int) = atan2(pts[i + 1].y - pts[i].y, pts[i + 1].x - pts[i].x)
    return FloatArray(n) { k ->
        val i0 = (k - 4).coerceIn(0, n - 2)
        val i1 = (k + 4).coerceIn(0, n - 2)
        var turn = heading(i1) - heading(i0)
        while (turn > PI) turn -= TAUf
        while (turn < -PI) turn += TAUf
        if (i1 == i0 || abs(turn) < 1e-4f) 1e6f else (i1 - i0) * DS / abs(turn)
    }
}

private fun strokeFrom(flow: Flow, start: Point, rnd: Random, heads: Random, salt: Int, low: Boolean = false): Stroke? {
    val f = flow.field
    val bounds = Rect(-MARGIN, -MARGIN, N + MARGIN, N + MARGIN)
    val path = f.trace(start, (MAXLEN / DS).toInt(), DS, Integrator.RK4, bounds)
    if (path.size < 40) return null

    var away = false
    var round = -1
    for (i in path.indices) {
        val d = dist(path[i], start)
        if (d > 30f) away = true
        else if (away && d < 2 * DS) {
            round = i
            break
        }
    }
    val size = if (round > 0) round * DS / TAUf else flow.eyes.minOf { hypot(start.x - it.x, start.y - it.y) }
    if (size < RMIN) return null
    val hw = (size * FAT * rnd.between(0.85f, 1.15f)).coerceAtMost(HWMAX)

    val head = headOf(hw, heads)
    val back = f.trace(start, ((head(0f) + 20f) / DS).toInt(), -DS, Integrator.RK4, bounds).reversed()
    val full = back + path.drop(1)

    val share = rnd.between(SHORT, 1f)
    val most = if (round > 0) {
        val len = min(SHUT * round * DS * rnd.between(0.95f, 1.03f) + 120f + 0.4f * hw, LONG)
        (len / DS).toInt().coerceIn(2, round)
    } else {
        val last = back.size - 1 + path.indexOfLast { it.x > -hw && it.x < N + hw && it.y > -hw && it.y < N + hw }
        (last + 2).coerceIn(2, full.size).coerceAtMost((LONG / DS).toInt().coerceAtLeast(2))
    }
    var end = (most * share).toInt().coerceAtLeast(2)
    if (low && full[end - 1].y < N / 2f) {
        val ends = ((most * SHORT).toInt().coerceAtLeast(2)..most).filter {
            val q = full[it - 1]
            q.y > N / 2f && q.y < N - 60f && q.x > 60f && q.x < N - 60f
        }
        if (ends.isNotEmpty()) end = ends[rnd.nextInt(ends.size)]
    }
    val pts = full.subList(0, end)
    if (pts.size < 20) return null

    val fit = bends(pts)
    val n = pts.size
    val w = (2f * hw / DS).toInt().coerceAtLeast(4)
    val lows = FloatArray(n) { k -> (max(0, k - w)..min(n - 1, k + w)).minOf { 0.8f * fit[it] } }
    val rooms = FloatArray(n) { k -> (max(0, k - w)..min(n - 1, k + w)).map { lows[it] }.average().toFloat() }
    val ph = rnd.between(0f, 100f)
    val points = List(n) { k ->
        val a = pts[(k - 1).coerceAtLeast(0)]
        val b = pts[(k + 1).coerceAtMost(n - 1)]
        val l = hypot(b.x - a.x, b.y - a.y)
        val t = k / (n - 1f)
        val room = rooms[k]
        val landing = 1f + 0.12f * (1f - smoothstep(0f, 0.05f, t))
        val half = hw * (0.75f + 0.25f * load(t)) * landing * (1f + PRESS * noise(t * 3f + ph))
        SpinePoint(pts[k], Vec2(-(b.y - a.y) / l, (b.x - a.x) / l), k * DS, min(half, room))
    }
    if (points.sumOf { it.half.toDouble() } / n < HWMIN) return null
    return Stroke(Spine(points, DS), size, salt, head)
}

private fun unhooked(s: Stroke): Stroke? {
    val pts = s.spine.points
    val hw = pts.maxOf { it.half }
    val fit = bends(pts.map { it.at })
    var best = 0 until 0
    var from = 0
    for (k in 0..pts.size) {
        if (k == pts.size || fit[k] < HOOK * hw) {
            if (k - from > best.last - best.first + 1) best = from until k
            from = k + 1
        }
    }
    if (best.first == 0 && best.last == pts.size - 1) return s
    if (best.last - best.first < 20) return null
    return Stroke(Spine(pts.subList(best.first, best.last + 1), DS), s.size, s.salt, s.head)
}

private class Cover(val cell: Float) {
    val side = (N / cell).toInt() + 1
    val count = IntArray(side * side)

    fun pick(rnd: Random): Point {
        val lo = count.min()
        val cells = count.indices.filter { count[it] == lo }
        val i = cells[rnd.nextInt(cells.size)]
        return Point((i % side + rnd.nextFloat() - 0.5f) * cell, (i / side + rnd.nextFloat() - 0.5f) * cell)
    }

    fun mark(p: Point) {
        val i = (p.x / cell).roundToInt().coerceIn(0, side - 1)
        val j = (p.y / cell).roundToInt().coerceIn(0, side - 1)
        count[j * side + i]++
    }

    fun add(s: Stroke) {
        val seen = BooleanArray(count.size)
        val pts = s.spine.points
        val last = pts.size - 1f
        val len = last * DS
        val along = 1.5f * DS + 1f
        for (k in pts.indices step 3) {
            val from = k * DS
            val end = ((len - from - 50f) / 150f).coerceIn(0f, 1f)
            val dry = smoothstep(0.6f, 0.2f, 1f - load(k / last))
            val r = pts[k].half * sqrt(end * dry)
            if (r < 1f) continue
            val unit = pts[k].half * sqrt(dry)
            val p = pts[k].at
            val nx = pts[k].normal.x
            val ny = pts[k].normal.y
            val box = max(r, along)
            val i0 = ceil((p.x - box) / cell).toInt().coerceIn(0, side - 1)
            val i1 = floor((p.x + box) / cell).toInt().coerceIn(0, side - 1)
            val j0 = ceil((p.y - box) / cell).toInt().coerceIn(0, side - 1)
            val j1 = floor((p.y + box) / cell).toInt().coerceIn(0, side - 1)
            for (j in j0..j1) for (i in i0..i1) {
                val dx = i * cell - p.x
                val dy = j * cell - p.y
                val off = dx * nx + dy * ny
                if (abs(off) <= r && abs(dx * ny - dy * nx) <= along && s.head(off / unit) <= from) {
                    seen[j * side + i] = true
                }
            }
        }
        for (i in seen.indices) if (seen[i]) count[i]++
    }

    fun bare() = count.count { it == 0 }
}

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

    val flow = field(rnd, Random(SEED * 7919 + 7))
    val headRnd = Random(SEED * 7919 + 6)
    val cover = Cover(20f)
    val laid = mutableListOf<Stroke>()
    var tries = 0
    fun place(cover: Cover, rnd: Random, heads: Random, salt: Int, low: Boolean = false): Stroke? {
        val p = cover.pick(rnd)
        var s = strokeFrom(flow, p, rnd, heads, salt, low)
        if (s == null) {
            s = strokeFrom(flow, aside(flow.field, p), rnd, heads, salt, low)
            cover.mark(p)
        }
        return s?.also { cover.add(it) }
    }
    while ((laid.size < STROKES || cover.bare() > 0) && tries++ < 1000) {
        place(cover, rnd, headRnd, laid.size + SEED * 101, low = laid.size < STROKES)?.let { laid += it }
    }
    val main = laid.take(STROKES).sortedByDescending { it.size }.map { unhooked(it) }
    val filler = laid.drop(STROKES).sortedByDescending { it.size }.mapNotNull { unhooked(it) }
    val patch = Cover(20f)
    filler.forEach(patch::add)
    main.filterNotNull().forEach(patch::add)
    val patchRnd = Random(SEED * 7919 + 9)
    val patches = mutableListOf<Stroke>()
    var more = 0
    while (patch.bare() > 0 && more++ < 300) {
        place(patch, patchRnd, patchRnd, laid.size + more + SEED * 101)?.let { patches += it }
    }

    val big = gart.supersampled(SS)
    val c = big.canvas
    c.clear(CRIMSON.sample(0.02f))
    c.scale(SS.toFloat(), SS.toFloat())
    val hairRnd = Random(SEED * 7919 + 1)
    fun paint(s: Stroke, depth: Float, dice: Random = rnd, hairs: Random = hairRnd) {
        val tone = dice.between(0.42f, 0.78f) + DEEP * (depth.pow(RISE) - 0.5f) - 0.1f
        shadow(c, BRUSH.outline(s.spine, ::load, head = s.head))
        c.drawBristles(s.spine, BRUSH, tone, hairs, s.salt, PAGE, ::load, head = s.head)
    }
    for (s in patches) paint(s, 0f, patchRnd, patchRnd)
    val dotRnd = Random(SEED * 7919 + 8)
    val dots = List(DOTS) {
        val r = 0.8f + 6f * dotRnd.nextFloat().pow(3)
        val at = (sqrt(dotRnd.nextFloat()) * (main.size + 1)).toInt().coerceAtMost(main.size)
        Triple(at, Point(N * dotRnd.nextFloat(), N * dotRnd.nextFloat()), r)
    }
    fun dotsAt(i: Int) {
        for ((at, p, r) in dots) if (at == i) {
            c.drawCircle(p.x, p.y, r, fillOf(BwColors.richBlack))
        }
    }
    for (s in filler) paint(s, 0f)
    for ((i, s) in main.withIndex()) {
        dotsAt(i)
        s?.let { paint(it, i / max(main.size - 1f, 1f)) }
    }
    dotsAt(main.size)

    val g = big.downsample(SS)
    addGrain(g, 0.14f, SEED)
    gart.saveImage(g, OUT.ensureExtension())
    println("cincinnus: seed=$SEED, strokes=${main.size} + ${filler.size} filler + ${patches.size} patch, tries=$tries, bare=${cover.bare()}, ${sw.ms}ms")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
