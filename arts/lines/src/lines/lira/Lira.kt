package lines.lira

import dev.oblac.gart.Gart
import dev.oblac.gart.color.BwColors
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.gfx.closedPathOf
import dev.oblac.gart.gfx.fillOf
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
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.util.Stopwatch
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/*
 * lira - one line, again and again. every echo is the line a moment later: its kinks grow
 * and slide a bit, and a small new bead appears about halfway. the first line is red and twice
 * as wide. every echo is thinner than the one before, the last ones are hairlines.
 *
 * the line goes down the page as x = f(y): two waves, two big kinks and the late bead. every
 * kink has a start and an end, and the echo at time t has all of them at t. I draw in painters
 * order and each edge fills everything right of it. so when a fast kink pushes an echo over
 * the one before, the later one covers it.
 */
private const val H = 2000
private val W = shortSide(H, ISO_RATIO)
private val SEED = pi("seed", 9)
private val OUT = ps("out", "lira")
private val SS = pi("ss", 3, 1..4)

private val LINES = pi("lines", 14, 3..60)
private val THIN = pf("thin", 0.35f, 0.05f..1.5f) // last echo width against FAT. 1 doesnt fade
private val CHANGE = pf("change", 1f, 0f..3f) // how much the line drifts by the last echo. 0 is a plain copy

private const val FAT = 0.65f // ribbon width
private const val SLIDE = 0.08f // how far a kink can slide
private const val BEAD = 40f // bead half height, px
private const val MARGIN = 0.07f
private const val PAD = 120f
private const val DY = 0.5f // px between

// inks
private val GROUND = BwColors.inkBlack
private val ECHO = BwColors.warmPaper
private val FIRST = NipponColors.col044_BENIHI

/// lines

private enum class Kind { BEAD, NOTCH, STEP, BELLY }

private class Kink(
    val kind: Kind,
    val c0: Float, val c1: Float, // centre, px down the page
    val s0: Float, val s1: Float, // half size, px
    val a0: Float, val a1: Float, // how far it pushes the line, in pitches
    val born: Float = 0f,
) {
    fun at(y: Float, t: Float): Float {
        val u = (y - lerp(c0, c1, t)) / lerp(s0, s1, t)
        var a = lerp(a0, a1, t)
        if (born > 0f) a *= smoothstep(born, born + 0.25f, t)
        return a * when (kind) {
            Kind.BEAD -> if (abs(u) < 1f) sqrt(1f - u * u) else 0f
            Kind.NOTCH -> max(0f, 1f - abs(u))
            Kind.STEP -> 0.5f * tanh(1.5f * u)
            Kind.BELLY -> exp(-u * u)
        }
    }
}

private class Wave(val a0: Float, val a1: Float, val f: Float, val p0: Float, val p1: Float) {
    fun at(y: Float, t: Float) = lerp(a0, a1, t) * sin(TAUf * f * y / H + lerp(p0, p1, t))
}

private class Line(val waves: List<Wave>, val kinks: List<Kink>, val lean: Float) {
    fun at(y: Float, t: Float): Float {
        var x = lean * t * (y / H - 0.5f) // leans a bit more every echo
        for (w in waves) x += w.at(y, t)
        for (k in kinks) x += k.at(y, t)
        return x
    }
}

private fun line(rnd: Random): Line {
    fun wave(a0: Float, a1: Float, f: Float): Wave {
        val p = rnd.between(0f, TAUf)
        return Wave(a0, lerp(a0, a1, CHANGE), f, p, p + CHANGE * rnd.between(-0.6f, 0.6f))
    }
    // one long slow wave and a short weak one, both deepen and roll a bit
    val waves = listOf(
        wave(rnd.between(0.3f, 0.9f), rnd.between(1f, 2.2f), rnd.between(0.35f, 0.75f)),
        wave(rnd.between(0.05f, 0.25f), rnd.between(0.1f, 0.5f), rnd.between(1.4f, 2.2f)),
    )

    // two big kinks and the late bead, each in its own third of the page
    val shapes = listOf(Kind.BEAD, Kind.BEAD, Kind.NOTCH, Kind.STEP, Kind.BELLY)
    val kinds = listOf(shapes.random(rnd), shapes.random(rnd), Kind.BEAD)
    val slots = kinds.indices.shuffled(rnd)
    val kinks = kinds.mapIndexed { i, kind -> kink(rnd, kind, slots[i], late = i == 2) }
    return Line(waves, kinks, CHANGE * rnd.between(-1.2f, 1.2f))
}

private fun kink(rnd: Random, kind: Kind, slot: Int, late: Boolean): Kink {
    val c0 = H * (0.1f + 0.8f * (slot + rnd.between(0.25f, 0.75f)) / 3)
    val c1 = c0 + CHANGE * H * rnd.between(-SLIDE, SLIDE)
    val sign = if (rnd.nextBoolean()) 1f else -1f
    return when (kind) {
        Kind.BEAD -> {
            val size = if (late) 0.7f else rnd.between(1f, 1.6f)
            val push = sign * size * rnd.between(0.7f, 1f) // about a pitch, less reads as a glitch
            val grow = rnd.between(1.2f, 2.4f)

            val born = if (late) rnd.between(0.25f, 0.55f) / max(CHANGE, 1e-3f) else 0f
            val r = BEAD * size
            Kink(kind, c0, c1, r, r * lerp(1f, grow, CHANGE * 0.6f), push, push * lerp(1f, grow, CHANGE), born)
        }
        Kind.NOTCH -> {
            val s = BEAD * rnd.between(1f, 1.5f)
            Kink(kind, c0, c1, s, s, sign * 0.8f, sign * lerp(0.8f, 1.6f, CHANGE))
        }
        Kind.STEP -> {
            val s = H * rnd.between(0.012f, 0.025f)
            val push = sign * rnd.between(0.6f, 1.2f)
            Kink(kind, c0, c1, s, s, push, push * lerp(1f, rnd.between(1.5f, 3f), CHANGE))
        }
        Kind.BELLY -> { // a belly is big, it slides only half as far
            val s = H * rnd.between(0.05f, 0.09f)
            val push = sign * rnd.between(0.6f, 1.2f)
            Kink(kind, c0, lerp(c0, c1, 0.5f), s, s, push, push * lerp(1f, rnd.between(1.5f, 3f), CHANGE))
        }
    }
}

// ---- edges

private fun smooth(xs: FloatArray, r: Int) {
    val n = xs.size
    val tmp = FloatArray(n)
    fun at(i: Int) = xs[i.coerceIn(0, n - 1)]
    repeat(2) {
        var acc = 0f
        for (i in -r..r) acc += at(i)
        for (i in 0 until n) {
            tmp[i] = acc / (2 * r + 1)
            acc += at(i + r + 1) - at(i - r)
        }
        tmp.copyInto(xs)
    }
}

private fun rightOf(edge: List<Point>): Path =
    closedPathOf(edge + Point(W * 3f, edge.last().y) + Point(W * 3f, edge.first().y))

fun main(args: Array<String>) {
    val gart = Gart.of("lira", W, H)
    val sw = Stopwatch()
    val line = line(seededRandom(SEED))

    // the fit: the left edge of the first line (it starts at -FAT) sits on one margin,
    // the right edge of the last echo on the other
    val ys = FloatArray(((H + 2 * PAD) / DY).toInt() + 1) { -PAD + it * DY }
    val inside = ys.filter { it in 0f..H.toFloat() }
    val lo = inside.minOf { line.at(it, 0f) }
    val hi = inside.maxOf { line.at(it, 1f) }
    val run = LINES - 1 + FAT * THIN
    val pitch = W * (1 - 2 * MARGIN) / (run + FAT + hi - lo)
    val x0 = W * MARGIN - pitch * (lo - FAT)

    val big = gart.supersampled(SS)
    val c = big.canvas
    c.clear(GROUND)
    c.scale(SS.toFloat(), SS.toFloat())

    // an edge at u fills everything right of it
    fun edge(u: Float, color: Int) {
        val t = max(u, 0f) / run
        val xs = FloatArray(ys.size) { x0 + pitch * (u + line.at(ys[it], t)) }
        smooth(xs, (6 / DY).toInt())
        c.drawPath(rightOf(ys.indices.map { Point(xs[it], ys[it]) }), fillOf(color))
    }

    edge(-FAT, FIRST)
    edge(FAT, GROUND)
    for (i in 1 until LINES) {
        edge(i.toFloat(), ECHO)
        edge(i + FAT * lerp(1f, THIN, i / (LINES - 1f)), GROUND)
    }

    val g = big.downsample(SS)
    gart.saveImage(g, OUT.ensureExtension())
    println("lira: seed=$SEED, kinks=${line.kinks.joinToString { it.kind.name.lowercase() }}, ${sw.ms}ms")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
