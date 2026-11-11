package sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.rgb
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.offset
import dev.oblac.gart.gfx.times
import dev.oblac.gart.gfx.vecTo
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.seededRandom
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.noiseOffset
import dev.oblac.gart.util.parallelForRows
import dev.oblac.gart.vector.Vec2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point

/**
 * sf15 - black hole.
 *
 * this is a schwarzschild hole with a thin hot disk around it. the view is from just above the
 * plane of the disk. each pixel is a photon. i trace it backward with the real orbit equation,
 * u'' + u = 3Mu^2. when the photon hits the disk, falls into the hole or escapes, the trace stops.
 *
 * the disk glows with the page-thorne flux. gravity and the spin of the disk shift the light. this
 * shift divides the flux by (1+z)^4.
 *
 * the plot looks like a mainframe printout drawn with a pen. it has no tones, only dust on rings of
 * one radius. the light only changes how thick the dust is. the left side moves toward us, so its
 * rings are dense. the right side moves away from us, so its light is redder. as a result, some of
 * its grains are red.
 *
 * the shadow is red sand. the amount of sand decreases from the middle to the rim. at the rim,
 * almost no sand is left. the near disk crosses the shadow in cream.
 */
fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("sf15", W, H)
    println(gart)
    println("seed=$SEED")
    val d = gart.d

    val plate = expose(d)
    val big = gart.supersampled(SS)
    big.canvas.clear(colorBack)
    shadow(big.canvas)
    if (HALO > 0f) halo(big.canvas)
    // the gas in front of the hole stays cream, its glowing after all
    ink(big.canvas, d, plate)
    val g = big.downsample(SS)
    develop(g)
    addGrain(g, 0.04f, SEED)

    gart.saveImage(g, "$OUT.png")
    if (!headless) gart.window().showImage(g)
}

private const val W = 1024
private const val H = 1024

private val SEED = pi("seed", 1)
private val OUT = ps("out", "sf15")
private val SS = pi("ss", 3, 1..4)

private val TILT = pf("tilt", 80f, 0f..89f)       // degrees off the disk axis. 90 would be edge on
private val ROUT = pf("rout", 60f, 8f..80f)       // outer edge of the disk, in M
private val RINGS = pi("rings", 28, 4..120)       // isoradials from the inner edge to rout
private val PITCH = pf("pitch", 0.4f, 0.05f..2f)  // M of arc between two dots on a ring
private val GROW = pf("grow", 2f, 0.2f..6f)     // how far the grains stray off the ring
private val DENS = pf("dens", 6f, 0.1f..8f)     // grains per px of ring at full light
private val CLUMP = pf("clump", 0.8f, 0f..2f)    // how lumpy the dust is. 0 = even rings
private val CLUMP_ACROSS = pf("clumpx", 6f, 0.5f..30f)  // lumps per unit of ln r, ring to ring
private val CLUMP_ALONG = pf("clumpy", 1.2f, 0.2f..8f)  // round the orbit; small = long streaks
private val HOT = pf("hot", 28f, 0f..60f)          // the hot spot, extra grains at its heart. 0 = none
private val HOT_R = pf("hotr", 8f, 6f..60f)       // where it orbits, M
private val HOT_PHI = pf("hotphi", 270f, -360f..360f)  // where on the orbit, degrees
private val HOT_W = pf("hotw", 3.5f, 0.3f..6f)    // how wide across the rings, M
private val HOT_TAIL = pf("hottail", 4f, 1f..12f) // tail over head length
private val GAMMA = pf("gamma", 0.5f, 0.2f..1.5f)  // how the grains follow the flux. 0.75 left the far side bare

// the lobe under the hole is real: its the underside of the far disk, seen past the near rim.
// a bigger rout hides more of it behind the near side. dont "fix" it

private val colorBack = RetroColors.black01
private val colorInk = RetroColors.white01
private val colorBold = RetroColors.red01
private val colorShift = RetroColors.red01  // grains going away


// the hole, M = 1 everywhere

private const val R_IN = 6.0   // last stable orbit, 6M. inside this the gas just falls
private const val STEP = 0.01  // dpsi for rk4, the thin ring in the hole wants it small

private val ci = cos(TILT * PI / 180)
private val si = sin(TILT * PI / 180)

// the frame, in M. the hole sits right and low, the hump
// arches over it. with the hole higher (looky -2.5) the arch fell off the top in scraps. 17
// across was tight, 25 gives the arch room. at looky 0 the bottom was empty, 4 sits it down
// level and centred (26 across) was a diagram, 84 showed the whole disk and read as a hat
private val VIEW = pf("view", 25f, 8f..90f)        // M across
private val ROT = pf("rot", 0f, -90f..90f)         // the whole sky turned, degrees. -16 slanted it, level is calmer
private val LOOK_X = pf("lookx", -2.5f, -30f..30f)  // frame centre, M from the hole
private val LOOK_Y = pf("looky", 4f, -30f..30f)
private val PXM = W / VIEW  // px per M
private fun scale(d: Dimension) = d.wf / VIEW
private val rc = cos(ROT * PI.toFloat() / 180)
private val rs = sin(ROT * PI.toFloat() / 180)

// where the hole lands on the canvas, px. rotation turns round it, so it stays put
private val HOLE = Point(W / 2f - LOOK_X * PXM, H / 2f + LOOK_Y * PXM)
private val SHADOW = 3 * sqrt(3f)  // shadow radius in M

// what the plate saw: flux (0..1), and where on the disk it came from. rad 0 where nothing hit
private class Plate(w: Int, h: Int) {
    val flux = FloatArray(w * h)
    val rad = FloatArray(w * h)
    val phi = FloatArray(w * h)
    val irad = FloatArray(w * h)  // the gas falling in, inside 6M. 0 where none
    val iphi = FloatArray(w * h)
    val gz = FloatArray(w * h)    // 1+z, what the light lost (or won) on the way here
}

// what one pixel saw: where on the disk, the flux and 1+z it brought, where it crossed the
// infall. 0 where it missed
private class Hit(val r: Float, val phi: Float, val flux: Float, val gz: Float, val ir: Float, val iphi: Float) {
    constructor(r: Double, phi: Double, flux: Double, gz: Double, ir: Double, iphi: Double) :
        this(r.toFloat(), phi.toFloat(), flux.toFloat(), gz.toFloat(), ir.toFloat(), iphi.toFloat())
}

private fun expose(d: Dimension): Plate {
    val pl = Plate(d.w, d.h)
    val s = scale(d)
    parallelForRows(d.h) { y0, y1 ->
        for (y in y0 until y1) for (x in 0 until d.w) {
            val ux = (x + 0.5f - d.cx) / s + LOOK_X
            val uy = (d.cy - y - 0.5f) / s + LOOK_Y
            val sky = Point(ux * rc - uy * rs, ux * rs + uy * rc)
            val hit = observe(sky) ?: continue
            val i = y * d.w + x
            pl.flux[i] = hit.flux
            pl.rad[i] = hit.r
            pl.phi[i] = hit.phi
            pl.irad[i] = hit.ir
            pl.iphi[i] = hit.iphi
            pl.gz[i] = hit.gz
        }
    }
    val top = pl.flux.max()
    for (i in pl.flux.indices) pl.flux[i] /= top
    return pl
}

/**
 * i trace one photon backward from the screen point [at], in M. the photon moves in one plane, the
 * plane of the line of sight and the screen offset. psi is the angle from the start of the path to
 * the photon, measured at the hole. u is 1/r.
 *
 * the photon crosses the plane of the disk where its height at psi changes sign. the disk is
 * opaque, so the trace stops at the first crossing inside the disk. the result is what the pixel
 * saw, as a [Hit]. when the photon crosses no gas, the result is null.
 */
private fun observe(at: Point): Hit? {
    val x = at.x.toDouble()
    val y = at.y.toDouble()
    val b = hypot(x, y)
    if (b < 1e-6 || b > ROUT + 3) return null  // past rout+3 a ray only ever crosses outside the disk
    val sa = y / b
    var psi = 0.0
    var u = 0.0
    var w = 1.0 / b
    var zp = ci
    // first crossing inside 6M, the thin gas falling in. it does not block the disk behind
    var ir = 0.0
    var iph = 0.0
    while (psi < 3 * PI) {
        val u0 = u
        // rk4 on u' = w, w' = 3u^2 - u
        val k1u = w
        val k1w = 3 * u * u - u
        val ua = u + 0.5 * STEP * k1u
        val k2u = w + 0.5 * STEP * k1w
        val k2w = 3 * ua * ua - ua
        val ub = u + 0.5 * STEP * k2u
        val k3u = w + 0.5 * STEP * k2w
        val k3w = 3 * ub * ub - ub
        val uc = u + STEP * k3u
        val k4u = w + STEP * k3w
        val k4w = 3 * uc * uc - uc
        u += STEP / 6 * (k1u + 2 * k2u + 2 * k3u + k4u)
        w += STEP / 6 * (k1w + 2 * k2w + 2 * k3w + k4w)
        psi += STEP

        if (u >= 0.5 || u < 0.0) break   // horizon, or gone back out to infinity

        val z = cos(psi) * ci + sin(psi) * sa * si
        if ((z < 0) != (zp < 0)) {
            val t = zp / (zp - z)
            val ux = u0 + t * (u - u0)
            if (ux > 0) {
                val r = 1 / ux
                // the point in 3d: cos(psi) o + sin(psi) s, o = (0, -si, ci) toward us
                val pc = psi - STEP * (1 - t)
                val dx = sin(pc) * x / b
                val dy = -cos(pc) * si + sin(pc) * sa * ci
                if (r >= R_IN && r <= ROUT) return Hit(r, atan2(dy, dx), seen(r, at.x), shift(r, at.x), ir, iph)
                if (r < R_IN && r > 2.05 && ir == 0.0) {
                    ir = r
                    iph = atan2(dy, dx)
                }
            }
        }
        zp = z
    }
    return if (ir > 0) Hit(0.0, 0.0, 0.0, 0.0, ir, iph) else null
}

/**
 * this is the flux that gets to the plate from radius r. the gas orbits at omega = r^-1.5. the
 * photon has L_z = -x sin(tilt). as a result, 1+z = (1 + omega x sin(tilt)) / sqrt(1 - 3/r). the
 * left side (x < 0) moves toward us, so its light is bluer and brighter.
 */
private fun seen(r: Double, x: Float): Double = emitted(r) / shift(r, x).pow(4)

private fun shift(r: Double, x: Float): Double = (1 + r.pow(-1.5) * x * si) / sqrt(1 - 3 / r)

// page-thorne for a = 0, the constants dropped. zero at 6M, peaks near 9.5M
private fun emitted(r: Double): Double {
    val sr = sqrt(r)
    val s3 = sqrt(3.0)
    val s6 = sqrt(6.0)
    val log = ln((sr + s3) * (s6 - s3) / ((sr - s3) * (s6 + s3)))
    return (sr - s6 + s3 / 2 * log) / ((r - 3) * r.pow(2.5))
}

// the pen //////////////////////////////

// the plotter: rings of one r (isoradials), and on each ring a point every PITCH M of arc.
// every dot is one computed (r, phi) on the disk, carried to wherever the light puts it.
// q counts rings from the inner edge. log spacing keeps the outer rings from crowding the far side
private fun ring(r: Float): Float = RINGS * ln(r / R_IN.toFloat()) / ln(ROUT / R_IN.toFloat())

private fun ringR(k: Int): Float = R_IN.toFloat() * (ROUT / R_IN.toFloat()).pow(k.toFloat() / RINGS)

// the light goes into how far grains stray off the ring. bright rings swell, dim ones stay a thread
private const val DOT_MIN = 0.06f  // in M, so the spread grows with the view
private const val DOT_MAX = 0.16f

private const val TAU = (2 * PI).toFloat()

// good luck!
private fun ink(c: Canvas, d: Dimension, pl: Plate) {
    val rng = seededRandom(SEED)
    val p = fillOf(colorInk)

    val tint = Tint(fillOf(colorShift), seededRandom(SEED + 5))
    val rad = pl.rad
    val q = FloatArray(rad.size) { if (rad[it] > 0f) ring(rad[it]) else -1f }

    val nodes = ArrayList<Node>()

    // finds every (ring, spoke) node inside the triangle. closed = the long edge counts
    fun tri(c0: Corner, c1: Corner, c2: Corner, closed: Boolean) {
        // barycentric in (q, phi)
        val ax = c1.q - c0.q
        val ay = c1.phi - c0.phi
        val bx = c2.q - c0.q
        val by = c2.phi - c0.phi
        val det = ax * by - ay * bx
        if (abs(det) < 1e-12f) return
        // the cell is affine in (q, phi), these are its two edges on screen
        val du = c0.at.vecTo(c1.at)
        val dv = c0.at.vecTo(c2.at)
        val k0 = ceil(minOf(c0.q, c1.q, c2.q)).toInt()
        val k1 = floor(maxOf(c0.q, c1.q, c2.q)).toInt()
        for (k in max(k0, 0)..min(k1, RINGS)) {
            val dp = TAU / spokes(k)
            val j0 = ceil(minOf(c0.phi, c1.phi, c2.phi) / dp).toInt()
            val j1 = floor(maxOf(c0.phi, c1.phi, c2.phi) / dp).toInt()
            for (j in j0..j1) {
                val ex = k - c0.q
                val ey = j * dp - c0.phi
                val u = (ex * by - ey * bx) / det
                val v = (ax * ey - ay * ex) / det
                if (u < 0f || v < 0f) continue
                if (if (closed) u + v > 1f else u + v >= 1f) continue
                val f = c0.flux + u * (c1.flux - c0.flux) + v * (c2.flux - c0.flux)
                val g = c0.gz + u * (c1.gz - c0.gz) + v * (c2.gz - c0.gz)
                // screen per M outward, and per M along the orbit
                val r = ringR(k)
                val out = (du * by - dv * ay) / det * dqdr(r)
                val along = (dv * ax - du * bx) / det / r
                val len = max(hypot(along.x, along.y), 1e-6f)
                val at = c0.at.offset(du * u).offset(dv * v)
                nodes.add(Node(k, Math.floorMod(j, spokes(k)), at, f.pow(GAMMA), abs(out.cross(along)) / len, len * PITCH, along / len, g))
            }
        }
    }

    // phi turned to sit next to phi0, so a cell on the seam at +-pi doesnt span the whole disk
    fun corner(i: Int, x: Int, y: Int, phi0: Float): Corner {
        var ph = pl.phi[i]
        while (ph - phi0 > PI) ph -= TAU
        while (ph - phi0 < -PI) ph += TAU
        return Corner(Point(x + 0.5f, y + 0.5f), q[i], ph, pl.flux[i], pl.gz[i])
    }

    // where the rings or the dots on them pack tighter than one a px (the thin ring in the hole,
    // the feet of it) there is nothing to plot along, so the pen just stipples by the light
    for (y in 0 until d.h) for (x in 0 until d.w) {
        val i = y * d.w + x
        if (rad[i] <= 0f) continue
        var slope = 0f
        var alone = true
        for (j in intArrayOf(i - 1, i + 1, i - d.w, i + d.w)) {
            if (j < 0 || j >= rad.size || abs(j % d.w - x) > 1) continue
            if (!same(rad[i], rad[j])) continue
            alone = false
            slope = max(slope, max(abs(q[j] - q[i]), arc(pl, i, j)))
        }
        if (!alone && slope < 0.4f) continue
        val v = pl.flux[i].pow(GAMMA)
        if (rng.nextFloat() < 0.3f * DENS / 6 * (lit(v) * gas(rad[i], pl.phi[i]) + v * HOT * hot(rad[i], pl.phi[i]))) dot(c, tint.pick(p, pl.gz[i]), rng, Point(x + rng.nextFloat(), y + rng.nextFloat()), grain(v))
    }

    // every cell of four pixel centres that all see the same image, cut into two triangles
    for (y in 0 until d.h - 1) for (x in 0 until d.w - 1) {
        val i = y * d.w + x
        val r0 = rad[i]
        if (!same(r0, rad[i + 1]) || !same(r0, rad[i + d.w]) || !same(r0, rad[i + d.w + 1])) continue

        val qs = maxOf(q[i], q[i + 1], q[i + d.w], q[i + d.w + 1]) - minOf(q[i], q[i + 1], q[i + d.w], q[i + d.w + 1])
        if (qs > 0.4f) continue
        if (max(arc(pl, i, i + 1), arc(pl, i, i + d.w)) > 0.8f || arc(pl, i, i + d.w + 1) > 0.8f) continue
        val phi0 = pl.phi[i]
        val right = corner(i + 1, x + 1, y, phi0)
        val below = corner(i + d.w, x, y + 1, phi0)
        tri(corner(i, x, y, phi0), right, below, false)
        tri(corner(i + d.w + 1, x + 1, y + 1, phi0), below, right, true)
    }
    for (o in nodes) dust(c, p, rng, o, tint)
    if (INFALL > 0f) infall(c, d, pl, p, rng)
}

// inside 6M there is no orbit left, the gas spirals in. off the last stable orbit the plunge is
// exact: 1/r = 1/6 + 2/psi^2, and it hits the horizon at psi = sqrt6. a few streams of grains,
// each from the inner ring down to the hole, born thin at 6M
// at least AI proposed this fn
private fun infall(c: Canvas, d: Dimension, pl: Plate, p: Paint, rng: Random) {
    val dice = seededRandom(SEED + 4)
    val ends = FloatArray(STREAMS) { dice.nextFloat() * TAU }
    val s6 = sqrt(6f)
    for (y in 0 until d.h) for (x in 0 until d.w) {
        val i = y * d.w + x
        val r = pl.irad[i]
        if (r <= 0f) continue
        val psi = sqrt(2f / (1f / r - 1f / 6f))
        var acc = 0f
        for (e in ends) {
            var dp = pl.iphi[i] - (e - SPIN * (psi - s6))
            dp -= TAU * floor(dp / TAU + 0.5f)
            acc += exp(-(dp * r / 0.18f).pow(2))
        }
        val born = ((R_IN.toFloat() - r) / 0.8f).coerceIn(0f, 1f)
        if (rng.nextFloat() < INFALL * acc * born) dot(c, p, rng, Point(x + rng.nextFloat(), y + rng.nextFloat()), FINE)
    }
}

private fun spokes(k: Int) = max(8, (TAU * ringR(k) / PITCH).roundToInt())

// one plotted point on ring k, spoke j. w = px across the ring per M, sp = px to the next spoke
private class Node(
    val k: Int, val j: Int, val at: Point, val v: Float, val w: Float, val sp: Float,
    val along: Vec2,  // unit, along the ring
    val g: Float,     // 1+z
)

private class Corner(val at: Point, val q: Float, val phi: Float, val flux: Float, val gz: Float)

// how much gas there is at (r, phi) on the disk, over the even disk. it is clumpy: noise in
// ln r and round the orbit, slow along it, since the spin shears every lump into a streak
private fun gas(r: Float, phi: Float): Float {
    val a = CLUMP_ALONG
    val n = SimplexNoise.noise(ln(r) * CLUMP_ACROSS + noff, cos(phi) * a, sin(phi) * a)
    // and the outer disk thins away, so the near rings fade into the ground
    val t = ln(r / R_IN.toFloat()) / ln(ROUT / R_IN.toFloat())
    val edge = if (RIM > 0f) ((1 - t) / RIM).coerceIn(0f, 1f).let { it * it * (3 - 2 * it) } else 1f
    return max(0f, 1f + CLUMP * 1.6f * n) * edge
}

// the hot spot, 1 at its heart
private fun hot(r: Float, phi: Float): Float {
    if (HOT <= 0f) return 0f
    var dp = phi - HOT_PHI * PI.toFloat() / 180
    while (dp > PI) dp -= TAU
    while (dp < -PI) dp += TAU
    val s = dp * r
    val dr = r - HOT_R

    val st = if (s < 0) 1.2f * HOT_TAIL else 1.2f
    return exp(-(dr / HOT_W).pow(2) - (s / st).pow(2))
}
private val noff = noiseOffset(SEED.toLong() + 3) * 100

// like the sand at the holemore light, more grains. the rings stay, as dust
private fun dust(c: Canvas, p: Paint, rng: Random, o: Node, tint: Tint) {
    val across = min(lerp(DOT_MIN, DOT_MAX, o.v) * GROW * o.w, 0.25f * o.w / dqdr(ringR(o.k)))
    val r = ringR(o.k)
    val phi = o.j * TAU / spokes(o.k)
    val n = (DENS * lit(o.v) * o.sp * gas(r, phi) + rng.nextFloat()).toInt()

    val nh = (DENS * o.v * o.sp * HOT * hot(r, phi) + rng.nextFloat()).toInt()
    val gap = o.w / dqdr(r)
    val side = Vec2(-o.along.y, o.along.x)
    for (i in 0 until n + nh) {
        val a = (rng.nextFloat() - 0.5f) * o.sp
        val b = if (i < n) (rng.nextFloat() + rng.nextFloat() - 1f) * across
        else (rng.nextFloat() + rng.nextFloat() - 1f) * min(across * 1.8f, 0.4f * gap)
        if (i >= n) rng.nextFloat()
        dot(c, tint.pick(p, o.g), rng, o.at.offset(o.along * a).offset(side * b), grain(o.v))
    }
}

// dots along the ring between two pixels: the turn in phi times r over the pitch
private fun arc(pl: Plate, i: Int, j: Int): Float {
    var dp = abs(pl.phi[j] - pl.phi[i])
    if (dp > PI) dp = TAU - dp
    return dp * pl.rad[i] / PITCH
}

// two pixels see the same image of the disk, not the primary next to the second one
private fun same(ra: Float, rb: Float) = ra > 0f && rb > 0f && abs(ra - rb) < 0.15f * min(ra, rb)

// q per M outward
private fun dqdr(r: Float): Float = RINGS / (r * ln(ROUT / R_IN.toFloat()))

private fun lit(v: Float) = if (TOE <= 0f) v else ((v - TOE) / (1 - TOE)).coerceAtLeast(0f)

private fun grain(v: Float) = FINE * (1 + COARSE * (v - 0.4f))

// the stipple pen
private const val FINE = 0.9f

// one dot of ink; never the same twice
private fun dot(c: Canvas, p: Paint, rng: Random, at: Point, size: Float) {
    val q = at.offset((rng.nextFloat() - 0.5f) * 0.03f * PXM, (rng.nextFloat() - 0.5f) * 0.03f * PXM) * SS
    val r = size * (0.85f + 0.3f * rng.nextFloat()) * SS
    p.alpha = 225 + rng.nextInt(31)
    c.drawCircle(q, r, p)
    if (rng.nextInt(14) == 0) {
        c.drawCircle(q.offset((rng.nextFloat() - 0.5f) * r * 1.4f, (rng.nextFloat() - 0.5f) * r * 1.4f), r * 0.8f, p)
    }
}

// the shadow: from far away, every ray with b < 3 sqrt3 M falls in, so its a circle,
// whatever the disk does. the near side of the disk is in front of it, the dots go on top
// the photon ring: light that went round the hole once or more piles up just outside the
// shadow edge. a band of cream grains, the near disk crosses in front of it
private val HALO = pf("halo", 2.4f, 0f..6f)       // band width, px. 0 = none
private val HALO_GAP = pf("halogap", 0.25f, 0f..1f) // M outside the shadow

private val LOP = pf("lop", 0.8f, 0f..1f)    // how one-sided the photon ring is. 0 = even
private val INFALL = pf("infall", 0.5f, 0f..1f)  // grains a px at the heart of a falling stream. 0 = none
private val STREAMS = pi("streams", 4, 1..12)
private val SPIN = pf("spin", 1f, -1f..1f)     // which way the streams wind; +1 goes with the disk
private val RIM = pf("rim", 0.7f, 0f..1f)      // the outer part of the disk (in log r) where the dust thins away. 0 = none
private val TOE = pf("toe", 0.2f, 0f..0.9f)     // light under this leaves no grain. 0.4 keeps only the left
private val COARSE = pf("coarse", 1f, 0f..3f)  // grain size by light. 0 = one sizee
private val DOPPLER = pf("doppler", 1f, 0f..1f)    // share of second-ink grains where its reddest. 0 = one ink, cream
private val SHIFT_FROM = pf("shiftfrom", 1f, 0.5f..2f)  // 1+z where the second ink starts
private val SAND = pf("sand", 0.92f, 0f..1f)  // how much of the shadow centre the red grains cover
private val FADE = pf("fade", 0.95f, 0f..1f)  // grains lost toward the rim. 0 = even, 1 = none at the rim

// the gas going away comes in redder. one grain in so many is the second ink, more the
// redder it got. a mix of grains, not a tint, two pens on one plate
private class Tint(val p2: Paint, val dice: Random) {
    fun pick(p: Paint, g: Float): Paint {
        if (DOPPLER <= 0f) return p
        val t = ((g - SHIFT_FROM) / 0.4f).coerceIn(0f, 1f)
        return if (dice.nextFloat() < DOPPLER * t * t * (3 - 2 * t)) p2 else p
    }
}

private fun halo(c: Canvas) {
    val rr = (SHADOW + HALO_GAP) * PXM
    val rng = seededRandom(SEED + 2)
    val p = fillOf(colorInk)
    val a0 = atan2(-rs, -rc)
    val n = (TAU * rr * HALO * 2.2f * (1 + LOP)).toInt()
    repeat(n) {
        val a = rng.nextFloat() * TAU
        val w = ((1 + cos(a - a0)) / 2).pow(1.5f)
        val keep = 1 - LOP * (1 - w)
        val r = rr + (rng.nextFloat() + rng.nextFloat() - 1f) * HALO * 0.8f * (1 + LOP * (1.6f * w - 0.6f))
        if (rng.nextFloat() < keep) dot(c, p, rng, HOLE.offset(r * cos(a), r * sin(a)), FINE)
    }
}

private fun shadow(c: Canvas) {
    val rr = SHADOW * PXM
    val rng = seededRandom(SEED + 1)
    val p = fillOf(colorBold)
    val n = (-ln(1f - min(SAND, 0.99f)) * PI.toFloat() * rr * rr / 2.5f).toInt()
    repeat(n) {
        val a = rng.nextFloat() * TAU
        val t = sqrt(rng.nextFloat())
        // t^1.5 keeps the middle full and drops off late, t^2 felt like a vignette
        if (rng.nextFloat() < 1f - FADE * t.pow(1.5f)) dot(c, p, rng, HOLE.offset(rr * t * cos(a), rr * t * sin(a)), FINE)
    }
}

// the print
private fun develop(g: Gartvas) {
    val d = g.d
    val off = noiseOffset(SEED.toLong()) * 100
    val back = colorBack
    val br = red(back)
    val bg = green(back)
    val bb = blue(back)
    val m = Gartmap(g)
    val px = m.pixels
    val half = hypot(d.cx, d.cy)
    for (y in 0 until d.h) for (x in 0 until d.w) {
        val i = y * d.w + x
        val rr = hypot(x - d.cx, y - d.cy) / half
        val k = 1f - 0.2f * rr * rr + 0.07f * SimplexNoise.noise(x / 280f + off, y / 280f)
        val c = px[i]
        px[i] = rgb(
            (br + (red(c) - br) * k).toInt().coerceIn(0, 255),
            (bg + (green(c) - bg) * k).toInt().coerceIn(0, 255),
            (bb + (blue(c) - bb) * k).toInt().coerceIn(0, 255),
        )
    }
    m.drawToCanvas(g)
}
