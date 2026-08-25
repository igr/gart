package sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.alpha
import dev.oblac.gart.gfx.drawRoundBorder
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.rndGaussian
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndi
import dev.oblac.gart.math.rndsgn
import org.jetbrains.skia.Canvas
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * sf12 - a plate from the chamber.
 *
 * a beam comes in from the left through a magnetic field. charged things bend, r = p * RSCALE,
 * and everything is drawn as the trail of bubbles it boils off. the heavy ones shed nothing and
 * cross the plate as long arcs; the light ones bleed momentum every pixel, so the radius keeps
 * shrinking and they wind in to a point. some beam tracks hit something and spray. neutrals leave
 * no trail and come apart further on, two tracks out of nowhere. one event gets the red pencil.
 */
fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("sf12", 1024, 1024)
    println(gart)
    println("seed=$SEED")
    val d = gart.d

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)

    gart.saveImage(g, "$OUT.png")

    if (!headless) gart.window().showImage(g)
}

private val SEED = pi("seed", 71)
private val OUT = ps("out", "sf12")
private val rng = Random(SEED)

private val colorBack = RetroColors.black01
private val colorInk = RetroColors.white01
private val colorBold = RetroColors.red01

// the field
private val BEAM = pi("beam", 11, 1..40)                // tracks coming in from the left edge
private val PINT = pf("pint", 0.6f, 0f..1f)             // chance a beam track hits something before it leaves
private val RSCALE = pf("rscale", 22f, 2f..200f)        // px of bend radius per unit of momentum
private val LOSS = pf("loss", 0.0045f, 0.0005f..0.05f)  // momentum a light track sheds per px. turns in a spiral = ln(p0/pmin) / (2 pi rscale loss), ~6 for p0 = 4
private val DELTA = pf("delta", 1.3f, 0f..20f)          // knock-on electrons per 1000 px of heavy track. the little curls
private val STRAYS = pi("strays", 7, 0..60)             // faint tracks wandering in from the sides, older events
private val BOIL = pi("boil", 1400, 0..10000)           // loose bubbles that arent on any track
private val DOT = pf("dot", 1.15f, 0.3f..5f)            // bubble radius px
private val GAP = pf("gap", 3.4f, 0.8f..12f)            // bubble spacing px on a fast track

private const val STEP = 1.5f
private const val SCATTER = 0.02f      // heading wobble per px, divided by p, so the slow ends crinkle
private const val PMIN = 0.08f         // below this it has stopped. r under 2px anyway
private const val GLOW = 1.6f          // ionisation goes as 1 + GLOW/p, so slow ends come out dense and fat
private const val HEAVY_LOSS = 0.12f   // heavy tracks shed this fraction of LOSS

// the track

private enum class Kind { BEAM, HEAVY, LIGHT, DELTA, STRAY }
private class Dot(val x: Float, val y: Float, val r: Float)
private class Trace(val dots: List<Dot>, val kind: Kind, val event: Int, val p0: Float)
private class End(val x: Float, val y: Float, val heading: Float, val weight: Float)

private val traces = mutableListOf<Trace>()
private val vertices = mutableMapOf<Int, End>()

// slow heavies draw fat, the beam thin. a track keeps one weight its whole length
private fun weightOf(p0: Float) = 1f + 0.4f * (8f / p0).coerceAtMost(1f)

// walks one particle until it stops or leaves, leaving bubbles. heavy tracks throw off the
// occasional knock-on electron on the way, which is its own track. electrons draw at the weight
// of whatever they came off, so a curl is the same line as its ray, just wound up
private fun fly(
    d: Dimension,
    x0: Float, y0: Float, heading0: Float, p0: Float, q: Int,
    kind: Kind, event: Int,
    maxLen: Float = Float.MAX_VALUE, weight: Float = weightOf(p0),
): End {
    val light = kind == Kind.LIGHT || kind == Kind.DELTA
    val loss = LOSS * if (light) 1f else HEAVY_LOSS
    var x = x0; var y = y0; var h = heading0; var p = p0
    var s = 0f; var acc = 0f; var turned = 0f
    val dots = mutableListOf<Dot>()
    val margin = 60f   // enough for a curl to dip out and come back

    while (p > PMIN && s < maxLen) {
        if (x < -margin || x > d.wf + margin || y < -margin || y > d.hf + margin) break
        val dh = q * STEP / (p * RSCALE)
        h += dh + rng.rndGaussian(0f, SCATTER) * sqrt(STEP) / p.coerceAtLeast(0.3f)
        turned += abs(dh)
        // heavies dont loop. past ~0.8 pi they thin out and go, as if dipping out of the plane
        val fade = if (light) 1f else 1f - ((turned - 0.8f * PIf) / (0.4f * PIf)).coerceIn(0f, 1f)
        if (fade <= 0f) break
        val w = weight * (0.25f + 0.75f * fade)   // what this bit of track draws at
        x += cos(h) * STEP; y += sin(h) * STEP
        val ion = 1f + GLOW / p
        p -= STEP * loss * (1f + 0.5f * (ion - 1f))
        s += STEP; acc += STEP

        val spacing = (GAP / ion).coerceAtLeast(0.9f)
        // bunched-up bubbles read heavier than beaded ones, so thin them as they close up. 0.6 at
        // the solid end of a curl makes it sit at the same weight as the ray it came off
        val thin = 0.6f + 0.4f * spacing / GAP
        while (acc >= spacing) {
            acc -= spacing
            // back off along the heading so the bubbles sit at even arc lengths, not on steps
            val bx = x - cos(h) * acc + rng.rndGaussian(0f, 0.4f)
            val by = y - sin(h) * acc + rng.rndGaussian(0f, 0.4f)
            dots += Dot(bx, by, DOT * w * thin * rng.rndf(0.75f, 1.25f))
        }
        if (!light && rng.rndf() < DELTA * STEP / 1000f) {
            // always an electron so they all curl the same way. it peels off tangent to the ray, on the
            // side it will curl toward - launched sideways they looped back behind the point and read as
            // hanging off the wrong end. 0.45 for the top of the angle was still a visible kink
            rng.rndsgn()   // used to pick the side. kept so the cast doesnt reshuffle
            fly(d, x, y, h - rng.rndf(0.02f, 0.2f), rng.rndf(0.25f, 1.6f), -1, Kind.DELTA, event, weight = w)
        }
    }
    traces += Trace(dots, kind, event, p0)
    return End(x, y, h, weight)
}

private fun vertex(d: Dimension, at: End, event: Int) {
    vertices[event] = at
    repeat(rng.rndi(2, 7)) {
        val wide = rng.rndf() < 0.2f   // mostly forward, now and then one goes off sideways
        val spread = if (wide) rng.rndf(-2.2f, 2.2f) else rng.rndGaussian(0f, 0.5f)
        if (rng.rndf() < 0.35f) {
            // electrons only ever go forward, a curl has to leave as the continuation of the ray
            fly(d, at.x, at.y, at.heading + spread * if (wide) 0.2f else 1f, rng.rndf(0.8f, 6.5f), rng.rndsgn(), Kind.LIGHT, event, weight = at.weight)
        } else fly(d, at.x, at.y, at.heading + spread, rng.rndf(8f, 35f), rng.rndsgn(), Kind.HEAVY, event)
    }
    // a neutral leaves without a trail and comes apart further on. a vee out of nothing
    if (rng.rndf() < 0.55f) {
        val h = at.heading + rng.rndGaussian(0f, 0.45f)
        val dist = rng.rndf(50f, 280f)
        val x = at.x + cos(h) * dist; val y = at.y + sin(h) * dist
        val open = rng.rndf(0.12f, 0.4f)
        fly(d, x, y, h + open, rng.rndf(8f, 26f), 1, Kind.HEAVY, event)
        fly(d, x, y, h - open, rng.rndf(8f, 26f), -1, Kind.HEAVY, event)
    }
    // a photon does the same but turns into a pair of electrons curling opposite ways
    if (rng.rndf() < 0.45f) {
        val h = at.heading + rng.rndGaussian(0f, 0.6f)
        val dist = rng.rndf(40f, 220f)
        val x = at.x + cos(h) * dist; val y = at.y + sin(h) * dist
        val e = rng.rndf(2.5f, 8f); val share = rng.rndf(0.25f, 0.75f)
        fly(d, x, y, h + 0.04f, e * share, 1, Kind.LIGHT, event, weight = at.weight)
        fly(d, x, y, h - 0.04f, e * (1f - share), -1, Kind.LIGHT, event, weight = at.weight)
    }
}

private fun generate(d: Dimension) {
    // the beam. one kind of particle, one charge, so the whole bundle leans the same way
    for (i in 0 until BEAM) {
        val y = rng.rndGaussian(d.hf * 0.5f, d.hf * 0.15f).coerceIn(d.hf * 0.1f, d.hf * 0.9f)
        val h = rng.rndGaussian(0f, 0.012f)
        val hits = rng.rndf() < PINT
        val len = if (hits) rng.rndf(d.wf * 0.18f, d.wf * 0.8f) else Float.MAX_VALUE
        val end = fly(d, -20f, y, h, rng.rndf(45f, 90f), -1, Kind.BEAM, i, len)
        if (hits) vertex(d, end, i)
    }
    repeat(STRAYS) {
        val (x, y, inward) = when (rng.rndi(4)) {
            0 -> Triple(-20f, rng.rndf(d.hf), 0f)
            1 -> Triple(d.wf + 20f, rng.rndf(d.hf), PIf)
            2 -> Triple(rng.rndf(d.wf), -20f, PIf / 2)
            else -> Triple(rng.rndf(d.wf), d.hf + 20f, -PIf / 2)
        }
        fly(d, x, y, inward + rng.rndf(-0.9f, 0.9f), rng.rndf(12f, 45f), rng.rndsgn(), Kind.STRAY, -1)
    }
}

// the event with the most tracks and the biggest curl wins, unless its vertex sits out near the edge
private fun pickRed(d: Dimension): Int {
    return vertices.keys.maxByOrNull { ev ->
        val v = vertices.getValue(ev)
        val mine = traces.filter { it.event == ev }
        val curl = mine.filter { it.kind == Kind.LIGHT }.maxOfOrNull { it.p0 } ?: 0f
        mine.size + curl - 4f * hypot(v.x - d.cx, v.y - d.cy) / d.wf
    } ?: traces.filter { it.event >= 0 }.groupingBy { it.event }.eachCount().maxByOrNull { it.value }?.key ?: -1
}

// the plate ======

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)
    generate(d)
    val redEvent = pickRed(d)

    // the liquid boiling on its own
    repeat(BOIL) {
        c.drawCircle(rng.rndf(d.wf), rng.rndf(d.hf), DOT * rng.rndf(0.5f, 1f), fillOf(colorInk).alpha(rng.rndi(30, 100)))
    }
    fiducials(c, d)

    // strays under everything, the red event on top. sortedBy is stable so the rest keep their order
    val order = traces.sortedBy { if (it.event == redEvent) 2 else if (it.kind == Kind.STRAY) 0 else 1 }
    for (t in order) {
        val red = t.event == redEvent
        val a = when (t.kind) {
            Kind.STRAY -> rng.rndi(70, 120)
            Kind.BEAM -> 240
            Kind.LIGHT -> 225
            else -> 195
        }.let { if (red) 255 else it }
        val paint = fillOf(if (red) colorBold else colorInk).alpha(a)
        val w = if (red) 1.35f else 1f
        t.dots.forEach { c.drawCircle(it.x, it.y, it.r * w, paint) }
    }

    c.drawRoundBorder(d, 10f, 40f, colorInk)
}

// the crosses the camera uses to find itself on the plate
private fun fiducials(c: Canvas, d: Dimension) {
    val paint = strokeOf(colorInk, 1.2f).alpha(130)
    val n = 5; val arm = 7f
    for (i in 0 until n) for (j in 0 until n) {
        val x = d.wf * (0.1f + 0.8f * i / (n - 1)) + rng.rndf(-3f, 3f)
        val y = d.hf * (0.1f + 0.8f * j / (n - 1)) + rng.rndf(-3f, 3f)
        c.drawLine(x - arm, y, x + arm, y, paint)
        c.drawLine(x, y - arm, x, y + arm, paint)
    }
}
