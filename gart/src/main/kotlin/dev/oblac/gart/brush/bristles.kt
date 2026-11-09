package dev.oblac.gart.brush

import dev.oblac.gart.color.Palette
import dev.oblac.gart.gfx.closedPathOf
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.offset
import dev.oblac.gart.math.between
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.noise
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathMeasure
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * One point of a [Spine]: where it is ([at]), the unit [normal] across the stroke, the [arc]
 * length along the spine and the [half] width of the stroke there, both in px.
 */
data class SpinePoint(val at: Point, val normal: Vec2, val arc: Float, val half: Float)

/**
 * The centre line of a wide stroke: [points] that are [step] px apart. [Bristles] ride a spine.
 *
 * Make one from a path with [along], or make the points yourself when you walk the curve.
 */
class Spine(val points: List<SpinePoint>, val step: Float) {
    init {
        require(points.size >= 2) { "a spine needs at least 2 points" }
        require(step > 0f) { "step must be > 0" }
    }

    val size get() = points.size

    companion object {
        /**
         * Samples the first contour of [path] every [step] px. [half] gives the half width at
         * `t`, 0 at the start of the path and 1 at its end. The last point is less than one step
         * from the end.
         */
        fun along(path: Path, step: Float = 3f, half: (t: Float) -> Float): Spine {
            require(step > 0f) { "step must be > 0" }
            val m = PathMeasure(path, false)
            try {
                val length = m.length
                require(length >= step) { "the path must be at least one step long" }
                val n = (length / step).toInt() + 1
                val points = List(n) { k ->
                    val s = k * step
                    val at = m.getPosition(s) ?: error("no position at $s")
                    val tan = m.getTangent(s) ?: error("no tangent at $s")
                    val dir = Vec2(tan.x, tan.y).normalize()
                    SpinePoint(at, Vec2(-dir.y, dir.x), s, half(s / length))
                }
                return Spine(points, step)
            } finally {
                m.close()
            }
        }
    }
}

/**
 * A wide brush of many bristles, the way a house painter's brush lays paint. Every bristle is
 * one stroke of the [hair] brush, and all of them ride one [Spine], each at its own offset
 * across it. The stroke gets dry gaps, a dark body between the bristles, and ends where the outer
 * bristles lift first. It is lit as a round bundle from the [light] side.
 *
 * Draw it with [drawBristles]. Each stroke takes its own tone on the [ramp], it can run out of
 * paint along the way, and it can land with a round head.
 *
 * @property ramp  colours from dark to light, sampled in OKLab
 * @property pitch px between bristles. At about 3 they melt into a sheen at screen size, at 5 they read as stripes
 * @property hair  the brush of one bristle. Its pressure is set along each bristle, so keep its own pressure flat
 * @property light x and y of the unit vector to the light. The rest of it points out of the page
 * @property body  how much darker than the stroke tone the body between the bristles is, on the ramp
 */
data class Bristles(
    val ramp: Palette,
    val pitch: Float = 4.6f,
    val hair: Brush = Brushes.pen.copy(scatter = 0.08f, sharpness = 1f, grain = 0.92f, opacity = 0.85f, pressure = Pressure.Flat),
    val light: Vec2 = Vec2(-0.42f, -0.62f),
    val body: Float = 0.38f,
) {
    init {
        require(pitch > 0f) { "pitch must be > 0" }
        require(light.x * light.x + light.y * light.y <= 1f) { "light must not be longer than 1" }
    }

    private val lut = IntArray(1024) { ramp.sampleOklab(it / 1023f) }
    internal fun ink(v: Float) = lut[(v.coerceIn(0f, 1f) * 1023).toInt()]
    internal val lz = sqrt(1f - light.x * light.x - light.y * light.y)

    /**
     * The outline of the body under the bristles of a stroke on [spine]. It comes to a point at
     * both ends, where the outer bristles lift, and it narrows away where the [load] runs out.
     * With [roundHead] the start is round. With [head] the start follows the points where the
     * bristles land. Give it the same [load], [roundHead] and [head] as [drawBristles] got. Use
     * it for a shadow or a clip.
     */
    fun outline(spine: Spine, load: ((t: Float) -> Float)? = null, roundHead: Boolean = false, head: ((t: Float) -> Float)? = null): Path {
        if (head != null) return landedOutline(spine, goneOf(spine, load), head)
        val pts = spine.points
        val len = pts.last().arc - pts[0].arc
        val gone = goneOf(spine, load)
        val cap = headLength(spine)
        // cut square, the body showed as a flat dark wedge past the last bristles. it dries
        // up before the bristles do, by the time 0.6 of the load is gone it is the centre line
        fun h(k: Int): Float {
            val from = pts[k].arc - pts[0].arc
            val shape = if (roundHead) {
                // half an ellipse at the start, the same curve the first bristles start on
                val head = if (cap > 0f) 1f - (1f - min(from / cap, 1f)).let { it * it } else 1f
                min(head, ((len - from - 50f) / 150f).coerceIn(0f, 1f))
            } else {
                ((minOf(from, len - from) - 50f) / 150f).coerceIn(0f, 1f)
            }
            return pts[k].half * sqrt(shape * smoothstep(0.6f, 0.2f, gone[k]))
        }
        val left = pts.mapIndexed { k, sp -> sp.at.offset(sp.normal * h(k)) }
        val right = pts.mapIndexed { k, sp -> sp.at.offset(sp.normal * -h(k)) }
        return closedPathOf(left + right.reversed())
    }
}

/**
 * Draws one stroke of [bristles] along [spine]: the dark body first, then the bristles.
 * [tone] (0..1 on the ramp) is the stroke's own tone, every bristle varies it a little.
 * [seed] moves the slow streaks, so two strokes with the same tone do not match. Bristles that
 * run outside [bounds] are cut there and skipped, give the page plus a margin. All the random
 * decisions come from [rnd].
 *
 * [load] is how much paint is left at `t`, 0 at the start of the spine and 1 at its end: 1 is
 * full, 0 is dry. As it runs out, the bristles thin, long dry streaks open in them, the paint
 * goes lighter and the body under them narrows away. Null is full all the way. A stroke that
 * lands loaded and runs dry: `load = { t -> (1f - t).pow(0.7f) }`.
 *
 * With [roundHead] the stroke lands with a round head: the bristles press out where the brush
 * lands, so the start is round, not pointed. The end still lifts to a point.
 *
 * [head] sets the start to any shape. It gives the point where each bristle lands: for the
 * bristle at `t` across the stroke (-1 on one edge, 1 on the other), the px along the spine where
 * it touches down. Use it for a slant, a blunt front or clumps of bristles. A value under 0 is 0.
 * The body under the bristles follows it. When [head] is set, [roundHead] has no effect.
 */
fun Canvas.drawBristles(
    spine: Spine,
    bristles: Bristles,
    tone: Float,
    rnd: Random,
    seed: Int = 0,
    bounds: Rect? = null,
    load: ((t: Float) -> Float)? = null,
    roundHead: Boolean = false,
    head: ((t: Float) -> Float)? = null,
) {
    drawPath(bristles.outline(spine, load, roundHead, head), fillOf(bristles.ink(tone - bristles.body)))
    val gone = goneOf(spine, load)
    val cap = headLength(spine)
    val count = (2 * spine.points.maxOf { it.half } / bristles.pitch).toInt()
    val run = BristleRun(this, bristles, rnd)
    for (j in 0 until count) {
        val t0 = -1f + 2f * (j + rnd.nextFloat()) / count
        val wid = bristles.pitch * rnd.between(0.45f, 0.85f)
        val base = tone + 0.07f + 0.4f * (rnd.nextFloat() - 0.5f)
        val ph = rnd.between(0f, 500f)
        // the brush ends: the outer bristles let go first
        val trim = ((30f + 140f * t0 * t0 + 60f * rnd.nextFloat()) / spine.step).toInt()
        // a round head: the further out a bristle is, the later it starts, on half an ellipse
        val first = when {
            head != null -> ((head(t0).coerceAtLeast(0f) + spine.step * rnd.nextFloat()) / spine.step).toInt()
            !roundHead -> trim
            else -> ((cap * (1f - sqrt(1f - t0 * t0)) + spine.step * rnd.nextFloat()) / spine.step).toInt()
        }
        for (k in first until spine.size - trim) {
            val sp = spine.points[k]
            val s = sp.arc
            val t = t0 + 0.035f * noise(s / 380f + ph)
            // gone dry, a bristle thins to nothing. gaps cut square read as glitches
            val g = gone[k]
            val dry = noise(s / 45f + ph * 1.3f)
            val lim = 0.95f - 0.5f * t * t - 0.4f * g
            var dw = smoothstep(lim, lim - 0.15f, dry) * (0.8f + 0.4f * noise(s / 90f + ph * 2.1f))
            if (g > 0f) {
                // running out: the bristles thin and long streaks open where a bristle runs dry for
                // a while. the long ones are what turn the tail to stripes. full, there are none
                val hi = 1.2f - 1.75f * g.pow(1.5f)
                dw *= (1f - 0.3f * g) * smoothstep(hi, hi - 0.2f, noise(s / 220f + ph * 0.7f))
            }
            val p = sp.at.offset(sp.normal * (sp.half * t))
            val outside = bounds != null && (p.x < bounds.left || p.x > bounds.right || p.y < bounds.top || p.y > bounds.bottom)
            if (abs(t) > 1.02f || dw <= 0.02f || outside) {
                run.flush(wid)
                continue
            }
            // the stroke as a soft round bundle. more light than 0.22 makes pipes of them
            val q = t * 0.85f
            val lam = sp.normal.dot(bristles.light) * q + sqrt(1f - q * q) * bristles.lz
            var v = base
            v += 0.22f * (lam - bristles.lz)
            v += 0.14f * noise(s / 1500f + seed * 0.37f) // the load runs out and comes back
            v += 0.2f * SimplexNoise.noise(t * 2.2f + seed * 3.1f, s / 700f)
            v += 0.1f * noise(s / 140f + ph * 0.7f)
            v -= 0.05f * smoothstep(0.8f, 1f, abs(t))
            if (g > 0f) v += 0.16f * g // thinner paint, lighter
            run.add(p, dw, v, wid)
        }
        run.flush(wid)
    }
}

// how far along the round head goes before the stroke is at its full width: 0.8 of the half
// width, so the head is a bit flatter than a half circle
private fun headLength(spine: Spine) = 0.8f * spine.points[0].half

// the body under a stroke with its own head covers what the bristles cover: the front where
// they land, then along both edges to the end. a hull of what is down filled the gap between
// two clumps that land first
private fun landedOutline(spine: Spine, gone: FloatArray, head: (t: Float) -> Float): Path {
    val pts = spine.points
    val last = pts.size - 1
    val len = pts[last].arc - pts[0].arc
    // t across the stroke at the spine point k. the end lifts to a point and the body narrows
    // away with the load, as with the other heads
    fun at(k: Int, t: Float): Point {
        val from = pts[k].arc - pts[0].arc
        val end = sqrt(((len - from - 50f) / 150f).coerceIn(0f, 1f))
        val dry = sqrt(smoothstep(0.6f, 0.2f, gone[k]))
        return pts[k].at.offset(pts[k].normal * (pts[k].half * t.coerceIn(-end, end) * dry))
    }
    val ts = FloatArray(HEAD_SAMPLES) { -1f + 2f * it / (HEAD_SAMPLES - 1) }
    val lands = IntArray(HEAD_SAMPLES) { (head(ts[it]).coerceAtLeast(0f) / spine.step).toInt().coerceAtMost(last) }
    val outline = ArrayList<Point>(HEAD_SAMPLES + 2 * pts.size)
    for (i in ts.indices) outline += at(lands[i], ts[i])
    for (k in lands.last() + 1..last) outline += at(k, 1f)
    for (k in last downTo lands.first() + 1) outline += at(k, -1f)
    return closedPathOf(outline)
}

// t steps across the stroke for the body of a head
private const val HEAD_SAMPLES = 129

// how much of the load is gone at each point of the spine, 0 when there is no load
private fun goneOf(spine: Spine, load: ((t: Float) -> Float)?) =
    FloatArray(spine.size) { if (load == null) 0f else 1f - load(it / (spine.size - 1f)).coerceIn(0f, 1f) }

// a bristle goes down in runs, a run ends where it goes dry or out of bounds. a stroke of the hair
// brush is one colour, so a run is cut again where its tone has moved. the next piece starts on
// the last point of the one before, or the bristle breaks there
private class BristleRun(val c: Canvas, val bristles: Bristles, val rnd: Random) {
    private val pts = mutableListOf<Point>()
    private val dws = mutableListOf<Float>()
    private var sum = 0f
    private var v0 = 0f
    private var lastV = 0f

    fun add(p: Point, dw: Float, v: Float, wid: Float) {
        if (pts.isEmpty()) {
            v0 = v
        } else if (abs(v - v0) > 0.035f || pts.size >= 120) {
            val last = pts.last()
            val lastDw = dws.last()
            flush(wid)
            pts += last
            dws += lastDw
            sum += lastV
            v0 = v
        }
        pts += p
        dws += dw
        sum += v
        lastV = v
    }

    fun flush(wid: Float) {
        if (pts.size >= 2) {
            val press = dws.toFloatArray()
            // dot size goes with pressure squared, so the width goes in as its root
            c.drawBrush(pts.toList(), bristles.hair, bristles.ink(sum / pts.size), wid / bristles.hair.weight, rnd,
                pressure = { t -> sqrt(press[(t * (press.size - 1)).roundToInt()]) })
        }
        pts.clear()
        dws.clear()
        sum = 0f
    }
}
