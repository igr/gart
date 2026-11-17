package sf

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.BwColors
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.flow2.VectorField
import dev.oblac.gart.flow2.plus
import dev.oblac.gart.flow2.trace
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.gfx.blurPaint
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.lerp
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.ISO_RATIO
import dev.oblac.gart.math.longSide
import dev.oblac.gart.math.seededRandom
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.noiseOffset
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Shader
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * voyager - one light-day. voyager 1 gets that far out on 2026-11-18.
 */

private const val W = 1200
private val H = longSide(W, ISO_RATIO)
private val SEED = pi("seed", 1)
private val SS = pi("ss", 3, 1..4)
private val OUT = ps("out", "voyager")

private const val MINUTES = 1440 // one day

private val voyager = Point(pf("vx", 0.84f, 0f..1f) * W, pf("vy", 0.075f, 0f..1f) * H)
private val earth = Point(pf("ex", 0.18f, 0f..1f) * W, pf("ey", 0.88f, 0f..1f) * H)
private val spread = pf("spread", 150f, 0f..1000f) // source strength, px. the plume ends up 2 pi of this wide
private val swirl = pf("swirl", 0.32f, 0f..3f)     // how much the current wanders

// the signal leaves voyager warm and cools on the way, it is nearly earths blue when it gets home
private val signal = Palette(0xFFFFC49A, 0xFFFF86B0, 0xFF7C9CFF)
private val earthInk = 0xFF6FBEFF.toInt()
private val voyagerInk = 0xFFFFF1DC.toInt()
private val starInk = Palette(0xFFFFEEDD, 0xFFDCE4FF)

private val nz = noiseOffset(SEED.toLong()) * 100f
private val dist = hypot(earth.x - voyager.x, earth.y - voyager.y)
private val home = atan2(earth.y - voyager.y, earth.x - voyager.x)
private val way = Vec2(cos(home), sin(home)) // the straight way home

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("voyager", W, H)
    println(gart)

    val big = gart.supersampled(SS)
    val river = river()
    sky(big.canvas)
    haze(big.canvas, seededRandom(SEED * 7919 + 5))
    stars(big.canvas, river, seededRandom(SEED * 7919 + 3))
    scratch(big.canvas, river, seededRandom(SEED))

    val g = big.downsample(SS)
    shine(g)
    star(g.canvas, voyager, voyagerInk, 1.3f, 70)
    star(g.canvas, earth, earthInk, 2.1f, 170)

    gart.saveImage(g, OUT.ensureExtension("png"))
    if (!headless) gart.window().showImage(g)
}

// the river

private fun river(): List<Line> {
    val sc = 760f
    val stream = VectorField { x, y, out ->
        val dx = x - voyager.x
        val dy = y - voyager.y
        val r2 = max(dx * dx + dy * dy, 1f)
        out.set(way.x + spread * dx / r2, way.y + spread * dy / r2)
    }
    val field = stream + VectorField.curl({ x, y -> swirl * sc * SimplexNoise.noise(x / sc + nz, y / sc) }, 2f)
    val box = Rect(-30f, -30f, W + 30f, H + 30f)
    return List(MINUTES) { j ->
        val th = home + PI.toFloat() + (j + 0.5f) * 2f * PI.toFloat() / MINUTES // one a minute, the seam behind
        val start = Point(voyager.x + 2f * cos(th), voyager.y + 2f * sin(th))
        Line(field.trace(start, 8000, 1.5f, bounds = box))
    }
}

// one line of it, walked by arc length s
private class Line(private val pts: List<Point>) {
    private val arc = FloatArray(pts.size)
    init {
        for (i in 1 until pts.size) arc[i] = arc[i - 1] + hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
    }
    val len = arc.last()
    val aim = exp(-sq(pts.minOf { hypot(it.x - earth.x, it.y - earth.y) } / 110f)) // the few that pass close to home

    fun at(s: Float): Point {
        if (pts.size < 2) return pts[0]
        var i = arc.binarySearch(s.coerceIn(0f, len))
        if (i < 0) i = -i - 2
        i = i.coerceIn(0, pts.size - 2)
        val t = ((s - arc[i]) / max(arc[i + 1] - arc[i], 1e-4f)).coerceIn(0f, 1f)
        return lerp(pts[i], pts[i + 1], t)
    }
}

private const val FADE = 0.8f

// all of them leave from one dot, ease off there or its a blob
private fun loud(s: Float) = (smoothstep(0f, 120f, s) * 0.85f + 0.15f) * (dist / (dist + FADE * 0.6f * s)).pow(1.3f)

// the sky!

private fun sky(c: Canvas) {
    val shader = Shader.makeRadialGradient(
        earth.x * SS, earth.y * SS, dist * 1.25f * SS,
        gradientOf(intArrayOf(BwColors.nightTeal, BwColors.obsidian, BwColors.abyss), floatArrayOf(0f, 0.45f, 1f)),
    )
    c.drawPaint(paint().apply { this.shader = shader })
    shader.close()
}

// the galaxy band crosses the way home, not along it. ragged edge, dark lanes in it
private const val MILKY = 1.3f
private val bandAt = Point(0.5f * W, 0.6f * H)
private const val BAND_TILT = 0.5f // radians down to the right, about 30 deg

private fun band(p: Point): Float {
    val d = -(p.x - bandAt.x) * sin(BAND_TILT) + (p.y - bandAt.y) * cos(BAND_TILT)
    val along = (p.x - bandAt.x) * cos(BAND_TILT) + (p.y - bandAt.y) * sin(BAND_TILT)
    val wob = 60f * SimplexNoise.noise(along / 400f + nz, 7.3f)
    val w = 0.13f * W * (1f + 0.35f * SimplexNoise.noise(along / 520f, 2.1f + nz))
    val lanes = smoothstep(-0.55f, 0.25f, SimplexNoise.noise(p.x / 140f + nz, p.y / 90f + 11f)) // dust lanes cut it
    return exp(-sq((d - wob) / w)) * (0.25f + 0.75f * lanes)
}

private fun haze(c: Canvas, rnd: Random) {
    val glow = blurPaint(28f * SS, BlendMode.PLUS)
    repeat(1400) {
        val p = Point(rnd.nextFloat() * W, rnd.nextFloat() * H)
        val b = band(p)
        if (rnd.nextFloat() > b) return@repeat
        glow.color = lerpColor(0xFFFFE6D0.toInt(), 0xFFCAD6FF.toInt(), rnd.nextFloat())
        glow.alpha = (7 * MILKY * b).toInt().coerceIn(1, 255)
        c.drawCircle(p.x * SS, p.y * SS, (25f + 45f * rnd.nextFloat()) * SS, glow)
    }

    // and the dust, stars too small to count
    val dust = paint()
    repeat((30000 * MILKY).toInt()) {
        val p = Point(rnd.nextFloat() * W, rnd.nextFloat() * H)
        val b = band(p)
        if (rnd.nextFloat() > b * b) return@repeat
        dust.color = starInk.sample(rnd.nextFloat())
        dust.alpha = (50 + 110 * rnd.nextFloat() * b).toInt()
        c.drawCircle(p.x * SS, p.y * SS, 0.5f * SS, dust) // 0.3 vanished in the downsample
    }
}

private const val STARS = 9000

// few, faint, clumped. none as bright as home, and they hide where the river runs
private fun stars(c: Canvas, river: List<Line>, rnd: Random) {
    val cell = 24f
    val cols = (W / cell).toInt() + 1
    val crowd = IntArray(cols * ((H / cell).toInt() + 1))
    fun cellOf(p: Point) = (p.y / cell).toInt() * cols + (p.x / cell).toInt()
    for (k in river.indices step 4) {
        val line = river[k]
        var s = 0f
        while (s < line.len) {
            val p = line.at(s)
            if (outside(p) == 0f) crowd[cellOf(p)]++
            s += 8f
        }
    }
    val dot = paint()
    val halo = blurPaint(1.6f * SS, BlendMode.PLUS)
    repeat(STARS) {
        val p = Point(rnd.nextFloat() * W, rnd.nextFloat() * H)
        val clump = smoothstep(-0.35f, 0.55f, SimplexNoise.noise(p.x / 300f + nz * 2f, p.y / 300f - 40f))
        val keep = (0.45f * clump + MILKY * band(p)) * (1f / (1f + crowd[cellOf(p)] * 0.12f))
        val u = rnd.nextFloat()
        val tint = rnd.nextFloat()
        if (rnd.nextFloat() > keep) return@repeat
        val b = u.pow(5f) // mostly at the edge of seeing
        val col = starInk.sample(tint)
        dot.color = col
        dot.alpha = (60 + 160 * b).toInt()
        c.drawCircle(p.x * SS, p.y * SS, (0.35f + 0.75f * b) * SS, dot)
        if (b > 0.5f) {
            halo.color = col
            halo.alpha = (60 * b).toInt()
            c.drawCircle(p.x * SS, p.y * SS, 3f * SS, halo)
        }
    }
}

// the scratches

private const val MANY = 0.42f    // how many scratches
private const val OPEN = -0.1f    // noise cut for where they may land. lower opens more sky
private const val BEAM = 1.6f     // extra on the lines that pass by home
private const val FRAG = 130f     // typical scratch length, px
private const val PACKETS = 0.25f // share of scratches that come as a packet
private const val GAIN = 1.4f

private val ink = paint().apply {
    blendMode = BlendMode.PLUS
    strokeWidth = 0.45f * SS // under a pixel. scratches, not wires
    strokeCap = PaintStrokeCap.ROUND
}

private fun scratch(c: Canvas, river: List<Line>, rnd: Random) {
    val step = 4f
    for ((k, line) in river.withIndex()) {
        var s = 0f
        while (s < line.len) {
            val p = line.at(s)
            val out = outside(p)
            if (out > 40f) {
                s += out - 30f // cant get back in faster than that
                continue
            }
            val sky = smoothstep(OPEN, OPEN + 0.6f, SimplexNoise.noise(p.x / 380f + nz, p.y / 380f))
            val odds = 0.0011f * MANY * step * (sky + BEAM * line.aim) * (0.4f + 0.6f * loud(s).coerceAtMost(1f))
            if (rnd.nextFloat() < odds) packet(c, river, rnd, k, s / line.len)
            s += step
        }
    }
}

private fun packet(c: Canvas, river: List<Line>, rnd: Random, k: Int, f: Float) {
    val l = (FRAG * exp((rnd.nextFloat() + rnd.nextFloat() + rnd.nextFloat() - 1.5f) * 0.9f)).coerceIn(6f, 600f)
    val amp = 0.55f + 0.45f * rnd.nextFloat()
    if (rnd.nextFloat() < PACKETS) {
        val m = 2 + rnd.nextInt(8)
        val gap = 4 + rnd.nextInt(6) // closer than 4 and the comb smears into a smudge
        val shear = (rnd.nextFloat() - 0.5f) * 1.2f
        wave(c, river, k, f, l, m, gap, shear, amp)
    } else {
        stroke(c, river, k, f * river[k].len, l, amp)
    }
}

private fun wave(c: Canvas, river: List<Line>, k: Int, f: Float, l: Float, m: Int, gap: Int, shear: Float, amp: Float) {
    for (i in -m..m) {
        val kk = k + i * gap
        if (kk !in river.indices) continue
        val e = exp(-2.2f * sq(i.toFloat() / m))
        stroke(c, river, kk, f * river[kk].len + i * gap * shear, l * (0.3f + 0.7f * sqrt(e)), amp * e)
    }
}

private fun stroke(c: Canvas, river: List<Line>, k: Int, mid: Float, l: Float, amp: Float) {
    var prev: Point? = null
    var t = -l / 2f
    while (t <= l / 2f) {
        val s = mid + t
        val p = river[k].at(s)
        val taper = (1f - sq(2f * t / l)).coerceAtLeast(0f).pow(1.5f)
        val flick = 0.65f + 0.35f * SimplexNoise.noise(s / 5f, k * 0.37f + nz) // the glassy bit
        val a = amp * taper * flick * loud(s) * GAIN
        if (prev != null) segment(c, prev, p, a)
        prev = p
        t += 1.5f
    }
}

// plumbing

private fun segment(c: Canvas, a: Point, b: Point, alpha: Float) {
    val level = (alpha.coerceIn(0f, 1f) * 255).toInt()
    if (level == 0) return
    val t = ((a.x - voyager.x) * way.x + (a.y - voyager.y) * way.y) / dist // how far home, along the way
    ink.color = signal.sampleOklab(t)
    ink.alpha = level
    c.drawLine(a.x * SS, a.y * SS, b.x * SS, b.y * SS, ink)
}

private fun outside(p: Point) = max(max(-p.x, p.x - W), max(-p.y, p.y - H)).coerceAtLeast(0f)
private fun sq(x: Float) = x * x

// a soft copy on top, so the threads shimmer a little
private fun shine(g: Gartvas) {
    val img = g.snapshot()
    g.canvas.drawImage(img, 0f, 0f, blurPaint(2.2f, BlendMode.PLUS, 48, FilterTileMode.CLAMP)) // 72 was too loud
}

private fun star(c: Canvas, at: Point, color: Int, r: Float, halo: Int) {
    fun glow(sigma: Float, a: Int) = blurPaint(sigma, BlendMode.PLUS).apply {
        this.color = color
        alpha = a
    }
    c.drawCircle(at.x, at.y, r * 12f, glow(r * 6f, halo / 3))
    c.drawCircle(at.x, at.y, r * 7f, glow(r * 3f, halo))
    c.drawCircle(at.x, at.y, r, fillOf(color))
    c.drawCircle(at.x, at.y, r * 0.45f, fillOf(lerpColor(color, voyagerInk, 0.7f))) // hot middle
}
