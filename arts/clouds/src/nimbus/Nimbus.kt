package nimbus

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.BwColors
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.colorScale
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.march.RayCamera
import dev.oblac.gart.math.WIDE_SCREEN_RATIO
import dev.oblac.gart.math.between
import dev.oblac.gart.math.seededRandom
import dev.oblac.gart.math.shortSide
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.pixels.boxDownsample
import dev.oblac.gart.util.Stopwatch
import dev.oblac.gart.util.parallelForRows
import dev.oblac.gart.vector.Vec3
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * nimbus - one puffy cloud over a flat plain, under a purple sky. i draw it the way a hand draws a
 * cloud, not the way the sky makes one.
 *
 * the cloud is a heap of balls. a row of big balls sits on the base. heads rise behind the row,
 * and the first head is the highest. each ball grows a few smaller bumps, mostly up and away from
 * the middle. then the bumps grow smaller bumps of their own. the base cuts the heap flat. three
 * huge balls hang a little under the base, so the bottom sags in soft arcs.
 *
 * a ray from the eye stops at the first spot of the heap above the base. under the base, only the
 * spots in a belly ball count. the sun is high on the left. the shading has no gradients: each spot
 * gets one of three flat tones, lit, half lit or in shade. the tone comes from the sun, the soft
 * shadow of the big balls, and how much the other balls crowd the spot. the flat cut and the belly
 * look down, so they get a pink from the plain.
 *
 * the sky and the plain are dark muted gradients, a bit darker at the edges, so the white cloud
 * is the only bright thing. far away, the plain becomes hazy. the cloud casts a soft shadow on the
 * plain.
 *
 * the seed changes the heap. sag sets how far the belly hangs under the base. at 0, the bottom is
 * a straight cut.
 *
 * [Cloud] holds only the shape. it finds where a ray hits the heap, and how much sun and open
 * space a spot gets. [shade] and [plain] pick the colors.
 */
private const val H = 2000
private val W = shortSide(H, WIDE_SCREEN_RATIO)

private val SEED = pi("seed", 14)
private val SAG = pf("sag", 0.12f, 0f..0.4f) // how far the belly hangs under the base. 0 is a straight cut
private val SS = pi("ss", 3, 1..4)
private val OUT = ps("out", "nimbus")

// the eye stands on the plain and looks up a bit, so the horizon sits low in the frame
private val CAM = RayCamera(Vec3(0f, 0.3f, 0f), pitch = 15.5f, fov = 40f, W * SS, H * SS)
private const val DIST = 10f // the cloud floats this far away
private const val BASE = 2f // and this high

// high on the left and a bit behind me, so the shadow lands right of the cloud
private val SUN = Vec3(-0.45f, 0.85f, -0.85f).normalize()

// purple rain, but muted and dark. the bright purple pulled the eye off the cloud
private val SKY = Palette(0xFF8C5CA8, 0xFF5B2F7A, 0xFF2B1240)
private val PLAIN = Palette(0xFF7A2559, 0xFF93487F, 0xFF4E1640)
private val CLOUD = Palette(0xFFE4D9F2, 0xFFB7A3D8, 0xFFE7BFD8)
private val LIT = BwColors.bg01

private const val NONE = -1
private const val UNDER = -2 // the flat cut or a belly ball, seen from below

// how much the backdrop darkens at the edges
private const val VIG = 0.3f

private class Ball(val pos: Vec3, val r: Float, val level: Int)

// true when the other ball sits all inside this one
private fun Ball.swallows(o: Ball) = (o.pos - pos).length() + o.r < r * 0.98f

// the heap ---------------------------------

private fun heap(rnd: Random): Cloud {
    val balls = ArrayList<Ball>()
    // the body: a row of big balls in front. the base cuts them a bit under the middle
    val half = rnd.between(1.0f, 1.3f)
    val nb = rnd.between(4, 5)
    for (i in 0 until nb) {
        val u = i / (nb - 1f) * 2f - 1f
        val r = (0.62f - 0.2f * u * u) * rnd.between(0.9f, 1.1f)
        balls += Ball(Vec3(u * half * 0.8f, BASE + 0.12f * r, DIST - 0.25f + rnd.between(-0.15f, 0.15f)), r, 0)
    }
    val bodyZ = balls.map { it.pos.z }.average().toFloat()

    // heads rise behind the body. the first one goes the highest
    repeat(rnd.between(3, 5)) { h ->
        val main = h == 0
        var x = if (main) rnd.between(-0.45f, 0.45f) else rnd.between(-half * 0.85f, half * 0.85f)
        val top = BASE + if (main) rnd.between(1.6f, 2.1f) else rnd.between(0.7f, 1.3f) * (1f - 0.4f * abs(x) / half)
        var r = if (main) rnd.between(0.5f, 0.6f) else rnd.between(0.36f, 0.48f)
        var y = BASE + 0.45f
        val z = DIST + rnd.between(-0.05f, 0.3f)
        while (true) {
            balls += Ball(Vec3(x, y, z), r, 0)
            // a neighbour beside it, so a head is a bunch and not a stack of balls. stacks looked like snowmen
            if (rnd.nextFloat() < 0.7f) {
                val side = if (rnd.nextBoolean()) 1f else -1f
                balls += Ball(Vec3(x + side * r * rnd.between(0.6f, 0.9f), y - r * 0.25f, z + rnd.between(-0.3f, 0.2f)), r * rnd.between(0.7f, 0.9f), 0)
            }
            if (y + r >= top) break
            y += r * rnd.between(0.5f, 0.7f)
            x += rnd.between(-0.25f, 0.25f) * r
            r *= 0.93f
        }
    }
    val mid = balls.map { it.pos.x }.average().toFloat()
    val bumps = sprout(balls, balls.toList(), rnd, 1, 3..5, 0.42f..0.62f, -0.15f, mid)
    sprout(balls, bumps, rnd, 2, 0..3, 0.36f..0.5f, 0.15f, mid)

    // the belly
    val x0 = balls.minOf { it.pos.x - it.r }
    val x1 = balls.maxOf { it.pos.x + it.r }
    val belly = List(3) { k ->
        val r = rnd.between(2.2f, 3.6f)
        val x = x0 + (x1 - x0) * (0.2f + 0.3f * k) + rnd.between(-0.15f, 0.15f)
        Ball(Vec3(x, BASE - SAG * rnd.between(0.55f, 1f) + r, bodyZ + rnd.between(-0.1f, 0.1f)), r, 0)
    }
    return Cloud(balls, belly)
}

// a few bumps on each parent, pushed up and away from the middle. a bump that hides inside a
// bigger ball does nothing, so i drop it
private fun sprout(
    all: MutableList<Ball>, parents: List<Ball>, rnd: Random,
    level: Int, kids: IntRange, ratio: ClosedFloatingPointRange<Float>, lowest: Float, mid: Float
): List<Ball> {
    val born = ArrayList<Ball>()
    for (p in parents) repeat(rnd.between(kids.first, kids.last)) {
        val dir = Vec3(
            rnd.between(-1f, 1f) + if (p.pos.x >= mid) 0.6f else -0.6f,
            rnd.between(lowest, 1.2f),
            rnd.between(-1.1f, 0.9f),
        )
        val r = p.r * rnd.between(ratio.start, ratio.endInclusive)
        val off = (p.r + r * rnd.between(-0.55f, -0.25f)) / max(dir.length(), 1e-3f)
        val b = Ball(p.pos + dir * off, r, level)
        if (b.pos.y + b.r * 0.5f > BASE && all.none { it.swallows(b) } && born.none { it.swallows(b) }) born += b
    }
    all += born
    return born
}

// tracing =====

private class Hit(bellies: Int) {
    var id = NONE
    var t = 0f
    // where the ray runs through the parts that count. 0 is the air above the base, then the bellies
    val from = FloatArray(bellies + 1)
    val to = FloatArray(bellies + 1)
}

private class Cloud(private val balls: List<Ball>, private val belly: List<Ball>) {
    val n = balls.size

    // short names for the loops that run per ray
    private fun x(i: Int) = balls[i].pos.x
    private fun y(i: Int) = balls[i].pos.y
    private fun z(i: Int) = balls[i].pos.z
    private fun r(i: Int) = balls[i].r

    // only the big balls cast shadows. the small bumps barely change them
    private val casters = balls.indices.filter { balls[it].level <= 1 }.toIntArray()

    val bellies = belly.size

    // one ball round the whole heap. most rays miss it and skip the loop
    private val mid = Vec3(
        balls.map { it.pos.x }.average().toFloat(),
        balls.map { it.pos.y }.average().toFloat(),
        balls.map { it.pos.z }.average().toFloat(),
    )
    private val bound = Ball(mid, balls.maxOf { (it.pos - mid).length() + it.r }, 0)

    // i tried a normal shared by the balls near a crease. it hid the straight seams, but the
    // puffs melted into one blob. a puff that reads beats a clean seam
    fun normal(i: Int, p: Vec3) = (p - balls[i].pos) / r(i)

    // the first spot of the cloud on the ray. a spot counts when it is in a ball, and above
    // the base or in a belly ball
    fun trace(d: Vec3, h: Hit) {
        h.id = NONE
        if (d.y < 1e-5f || cross(bound, d) == null) return
        h.from[0] = (BASE - CAM.eye.y) / d.y
        h.to[0] = Float.POSITIVE_INFINITY
        for (k in 0 until bellies) {
            val s = cross(belly[k], d)
            h.from[k + 1] = s?.from ?: 1f // a miss is an empty span
            h.to[k + 1] = s?.to ?: 0f
        }
        var best = Float.MAX_VALUE
        for (i in 0 until n) {
            val s = cross(balls[i], d) ?: continue
            if (s.from >= best) continue
            for (k in h.from.indices) {
                val t = max(s.from, h.from[k])
                if (t < min(s.to, h.to[k]) && t < best) {
                    best = t
                    h.id = if (s.from >= h.from[k]) i else UNDER
                }
            }
        }
        h.t = best
    }

    // soft shadow: how close the ray to the sun passes each big ball, over how far it went. a bigger k is a harder edge
    fun shadow(p: Vec3, k: Float, skip: Int): Float {
        var s = 1f
        for (j in casters) {
            if (j == skip) continue
            val ox = x(j) - p.x
            val oy = y(j) - p.y
            val oz = z(j) - p.z
            val along = ox * SUN.x + oy * SUN.y + oz * SUN.z
            if (along <= 0f) continue
            val miss = sqrt(max(ox * ox + oy * oy + oz * oz - along * along, 0f)) - r(j)
            s = min(s, (k * miss / along).coerceIn(0f, 1f))
        }
        return s
    }

    // 1 in the open, less where the other balls crowd in front of the face
    fun open(p: Vec3, nrm: Vec3, skip: Int): Float {
        var occ = 0f
        for (j in 0 until n) {
            if (j == skip) continue
            val vx = x(j) - p.x
            val vy = y(j) - p.y
            val vz = z(j) - p.z
            val l2 = vx * vx + vy * vy + vz * vz
            val rr = r(j) * r(j)
            if (l2 > 36f * rr) continue
            val c = (nrm.x * vx + nrm.y * vy + nrm.z * vz) / sqrt(l2)
            if (c > 0f) occ += c * rr / l2
        }
        return (1f - 0.55f * occ).coerceIn(0.35f, 1f)
    }
}

private class Span(val from: Float, val to: Float) // two distances along the ray, not a point

// where the ray goes in and out of a ball, or null when it misses
private fun cross(ball: Ball, d: Vec3): Span? {
    val o = CAM.eye - ball.pos
    val b = o.dot(d)
    val disc = b * b - (o.dot(o) - ball.r * ball.r)
    if (disc <= 0f) return null
    return Span(-b - sqrt(disc), -b + sqrt(disc))
}

private fun shade(cloud: Cloud, d: Vec3, h: Hit, behind: Int): Int {
    cloud.trace(d, h)
    val i = h.id
    if (i == NONE) return behind
    if (i == UNDER) return CLOUD[2] // the flat cut and the belly look down at the plain
    val p = CAM.eye + d * h.t
    val nrm = cloud.normal(i, p)
    // wrapped light: the dark side starts 0.4 past the edge of the ball
    val wrap = ((nrm.dot(SUN) + 0.4f) / 1.4f).coerceIn(0f, 1f)
    val v = wrap * cloud.shadow(p, 4f, i) * (0.55f + 0.45f * cloud.open(p, nrm, i))
    return when {
        v > 0.62f -> LIT
        v > 0.3f -> CLOUD[0]
        else -> CLOUD[1]
    }
}

// the backdrop -----------------------------

// the plain goes hazy far off, and the shadow has a soft edge
private fun plain(cloud: Cloud, d: Vec3): Int {
    val t = -CAM.eye.y / d.y
    val ground = lerpColor(PLAIN[0], PLAIN[1], 1f - exp(-t / 25f))
    val s = cloud.shadow(Vec3(CAM.eye.x + d.x * t, 0f, CAM.eye.z + d.z * t), 3f, NONE)
    return lerpColor(PLAIN[2], ground, smoothstep(0.35f, 0.65f, s))
}

// the sky and the plain as smooth gradients
private fun backdrop(cloud: Cloud): IntArray {
    val back = IntArray(W * H)
    for (y in 0 until H) for (x in 0 until W) {
        val d = CAM.ray((x + 0.5f) * SS, (y + 0.5f) * SS)

        // the pow makes the low sky wider
        val c = if (d.y < 0f) plain(cloud, d) else SKY.sample((d.y / 0.6f).coerceIn(0f, 1f).pow(0.75f))
        val dx = x / W.toFloat() - 0.5f
        val dy = y / H.toFloat() - 0.5f
        back[y * W + x] = colorScale(c, 1f - VIG * smoothstep(0.2f, 0.75f, sqrt(dx * dx + dy * dy)))
    }
    return back
}

fun main(args: Array<String>) {
    val sw = Stopwatch()
    val gart = Gart.of("nimbus", W, H)
    val cloud = heap(seededRandom(SEED))
    val back = backdrop(cloud)
    val ws = W * SS
    val big = IntArray(ws * H * SS)
    parallelForRows(H * SS) { y0, y1 ->
        val h = Hit(cloud.bellies)
        for (y in y0 until y1) for (x in 0 until ws) {
            big[y * ws + x] = shade(cloud, CAM.ray(x + 0.5f, y + 0.5f), h, back[(y / SS) * W + x / SS])
        }
    }
    val g = gart.gartvas()
    Gartmap(g).use { m ->
        boxDownsample(big, SS, m)
        m.drawToCanvas()
    }
    gart.saveImage(g, OUT.ensureExtension())
    println("nimbus: seed=$SEED, ${cloud.n} balls, ${sw.ms}ms")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
