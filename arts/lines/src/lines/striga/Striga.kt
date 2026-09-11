package lines.striga

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.brush.Brushes
import dev.oblac.gart.brush.drawBrush
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.gfx.Circle
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.offset
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.util.Stopwatch
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.Color
import org.jetbrains.skia.Point
import kotlin.math.pow
import kotlin.random.Random

private const val W = 1200
private const val H = 1200
private val SEED = pi("seed", 1)
private val OUT = ps("out", "striga")
private val SS = pi("ss", 3, 1..4)
private val STACKS = pi("stacks", 7, 2..20)
private val PITCH = pf("pitch", 7.5f, 3f..24f)
private val WIDE = pf("wide", 0.68f, 0.2f..1f)
private val EDGE = pf("edge", 0.36f, 0f..1f)
private val FADE = pf("fade", 0.3f, 0.02f..1f)
private val VANISH = pf("vanish", 0.8f, 0f..1.5f)
private val THIN = pf("thin", 0.4f, 0.02f..1f)
private val WANDER = pf("wander", 0.12f, 0f..0.4f)
private val DISC = pf("disc", 1.5f, 0f..4f)
private val DISCX = pf("discx", 0.3f, 0f..1f)
private val CUT = pf("cut", 0.5f, 0f..1f)
private val SPREAD = pf("spread", 0.2f, 0f..0.5f)
private val PASSES = pi("passes", 3, 1..8)

private const val MARGIN = 0.1f
private const val GAP = 0.18f
private const val DISCY = 0.5f
private const val MIX = 0.03f
private const val SOLID = 0.05f
private const val STRAY = 0.025f
private const val MESS = 1f
private const val SHADOW = 0.4f
private const val GRAIN = 0.03f

private val x0 = MARGIN * W
private val y0 = MARGIN * H
private val stackPitch = (W - 2f * x0) / STACKS

private val paper = 0xFFF7F6F3.toInt()
private val umber = 0xFF6A6058.toInt()
private val inks = Palette(0xFFE2DDD4, 0xFF1B1A18)
private val drift = Palette(0xFFD3CEC4, 0xFF4A4642)

private val BAND = Brushes.charcoal.weight + 2f * Brushes.charcoal.scatter

private fun Int.at(a: Float) = alpha((255f * a.coerceIn(0f, 1f)).toInt())

private fun falloff(at: Float, over: Float, u: Float) = 1f - smoothstep(at - over / 2f, at + over / 2f, u)

private class Stick(val a: Point, val b: Point, val w: Float, val up: Vec2, val black: Boolean, val ink: Int, val front: Boolean)

private fun layout(rnd: Random): List<Stick> {
    val len = stackPitch * (1f - GAP)
    val n = ((H - 2f * y0) / PITCH).toInt()
    val sticks = ArrayList<Stick>(STACKS * n)
    for (k in 0 until STACKS) {
        val left = x0 + k * stackPitch + (stackPitch - len) / 2f
        val edge = EDGE + rnd.rndf(-WANDER, WANDER)
        val vanish = VANISH + rnd.rndf(-WANDER, WANDER)
        val cut = CUT + rnd.rndf(-SPREAD, SPREAD)
        for (i in 0 until n) {
            val u = (i + 0.5f) / n
            if (rnd.nextFloat() >= falloff(vanish, THIN, u)) continue
            val p = lerp(STRAY, 1f - MIX, falloff(edge, FADE, u))
            val roll = rnd.nextFloat()
            val black = u < SOLID || roll < p
            sticks += stick(left, H - y0 - (i + 0.5f) * PITCH, len, black, u < cut, rnd)
        }
    }
    return sticks
}

private fun stick(left: Float, cy: Float, len: Float, black: Boolean, front: Boolean, rnd: Random): Stick {
    val y = cy + rnd.rndf(-0.18f, 0.18f) * PITCH * MESS
    val x = left + len / 2f + rnd.rndf(-0.03f, 0.03f) * len * MESS
    val l = len * (1f + rnd.rndf(-0.05f, 0.05f) * MESS)
    val along = Vec2.of(Degrees(rnd.rndf(-1f, 1f) * MESS))
    val w = PITCH * WIDE * (1f + rnd.rndf(-0.15f, 0.15f) * MESS)
    val centre = Point(x, y)
    val half = along * (l / 2f)
    return Stick(centre.offset(-half.x, -half.y), centre.offset(half), w, Vec2(along.y, -along.x), black, ink(black, rnd), front)
}

private fun ink(black: Boolean, rnd: Random): Int {
    val i = if (black) 1 else 0
    return lerpColor(inks[i], drift[i], rnd.nextFloat().pow(2) * if (black) 0.35f else 0.5f)
}

private fun Canvas.stroke(s: Stick, ox: Float, oy: Float, width: Float, color: Int, rnd: Random) =
    drawBrush(s.a.offset(ox, oy), s.b.offset(ox, oy), Brushes.charcoal, color, width / BAND, rnd)

private fun Canvas.draw(sticks: List<Stick>, rnd: Random) {
    for (s in sticks) shadow(s, rnd)
    for (s in sticks) repeat(PASSES) { stroke(s, 0f, 0f, s.w, s.ink, rnd) }
}

private fun Canvas.shadow(s: Stick, rnd: Random) {
    val ox = -s.up.x * 0.45f * s.w - s.up.y * 0.3f * s.w
    val oy = -s.up.y * 0.45f * s.w + s.up.x * 0.3f * s.w
    stroke(s, ox, oy, 1.25f * s.w, umber.at(SHADOW), rnd)
}

private fun Canvas.disc(sticks: List<Stick>, rnd: Random) {
    val disc = Circle(DISCX * W, DISCY * H, DISC * stackPitch)
    drawCircle(disc, fillOf(Color.WHITE))
    save()
    clipPath(disc.toPath(), ClipMode.INTERSECT, true)
    draw(sticks.filter { it.front }, rnd)
    restore()
}

fun main(args: Array<String>) {
    val gart = Gart.of("striga", W, H)
    val sw = Stopwatch()
    val big = gart.supersampled(SS)
    val c = big.canvas
    c.clear(paper)
    c.scale(SS.toFloat(), SS.toFloat())
    val rnd = Random(SEED)
    val sticks = layout(rnd)
    c.draw(sticks, rnd)
    if (DISC > 0f) c.disc(sticks, rnd)
    val g = big.downsample(SS)
    addGrain(g, GRAIN, SEED)
    println("striga: seed=$SEED, ${sticks.size} sticks, ${sticks.count { it.black }} black, ${sw.ms}ms")

    gart.saveImage(g, OUT.ensureExtension())
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
