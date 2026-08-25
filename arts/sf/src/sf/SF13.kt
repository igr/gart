package sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.alpha
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.gfx.drawRoundBorder
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.outlineOf
import dev.oblac.gart.gfx.segmentHitsCircle
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.rndb
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndi
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.noiseOffset
import dev.oblac.gart.vector.Vec3
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PathBuilder
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * sf13 - scratched.
 *
 * a scratchboard plate: everything is a line cut into the black. spheres are turned on the
 * lathe, one line per latitude, and the line swells where the light hits and thins to a hair in
 * the dark, so the tone is nothing but line weight. the sun is turned the same way but in red,
 * with a rougher grain that reads as granulation, and it sits in a pond of rings that thin to
 * nothing where a body is in the way, which leaves the shadows as wedges. a moon that sits in
 * its planets shadow gets cut in red as well.
 */
fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("sf13", 1024, 1024)
    println(gart)
    println("seed=$SEED")
    val d = gart.d

    val big = gart.gartvas(Dimension(d.w * SS, d.h * SS))
    draw(big.canvas, d)

    // box average, not scaleImage - skias samplers leave hairlines at this spacing rippling
    val g = big.downsample(SS)
    g.canvas.drawRoundBorder(d, 10f, 40f, colorBack)

    gart.saveImage(g, "$OUT.png")
    if (!headless) gart.window().showImage(g)
}

private val SEED = pi("seed", 49)
private val OUT = ps("out", "sf13")
private val rng = Random(SEED)
private const val SS = 3                            // lines this thin need the supersample
private val NZOFF = noiseOffset(SEED.toLong())      // simplex is seedless, shift the window instead

private val colorBack = RetroColors.black01
private val colorInk = RetroColors.white01
private val colorBold = RetroColors.red01

// the cut ----
private val PITCH = pf("pitch", 3.2f, 1.5f..8f)     // px between latitude lines
private val FILL = pf("fill", 0.95f, 0.3f..1.2f)    // widest line as a share of the pitch. at 1 the lit side goes solid
private val HAIR = pf("hair", 0.22f, 0f..1f)        // thinnest line, px
private val GAMMA = pf("gamma", 0.8f, 0.2f..3f)     // shade curve. under 1 the terminator is soft, over it goes hard
private val RELIEF = pf("relief", 0.5f, 0f..1.5f)   // surface noise on the line weight, reads as maria and bands
private val ECLIPSE = pf("eclipse", 0.12f, 0f..1f)  // how much light a body in shadow still gets

// the sky ----
private val BODIES = pi("bodies", 6, 1..14)
private val SUNR = pf("sun", 120f, 20f..400f)
private val GRAIN = pf("grain", 0.8f, 0f..2f)       // granulation on the sun, on top of relief
private val RINGS = pi("rings", 70, 0..400)         // ripples round the sun
private val RGAP = pf("rgap", 9f, 2f..40f)          // gap between the first two, px. it opens up going out
private val RINGW = pf("ringw", 2.0f, 0.2f..5f)     // ring line width at the sun, px

// a body sits in the picture plane at z = 0. axis is its pole, a unit vector in 3d. the frame is
// kept as plain floats, the lathe loop is hot
private class Body(val x: Float, val y: Float, val r: Float, axis: Vec3) {
    val ax = axis.x; val ay = axis.y; val az = axis.z
    val ux: Float; val uy: Float; val uz: Float   // u and v span the equator
    val vx: Float; val vy: Float; val vz: Float
    init {
        val (u, v) = axis.basis()
        ux = u.x; uy = u.y; uz = u.z
        vx = v.x; vy = v.y; vz = v.z
    }

    // a point on the surface, relative to the centre: cr out along the equator at (ct, st), sr up
    // the pole. one per coordinate rather than a vec3, the lathe loop is hot
    fun sx(cr: Float, sr: Float, ct: Float, st: Float) = cr * (ct * ux + st * vx) + sr * ax
    fun sy(cr: Float, sr: Float, ct: Float, st: Float) = cr * (ct * uy + st * vy) + sr * ay
    fun sz(cr: Float, sr: Float, ct: Float, st: Float) = cr * (ct * uz + st * vz) + sr * az
}

private var sunX = 0f; private var sunY = 0f
private lateinit var sun: Body
private val bodies = mutableListOf<Body>()

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)
    cast(d)
    drawRings(c, d)
    drawSun(c, sun)
    for (b in bodies) drawPlanet(c, b)
}

// nothing overlaps, thats a rule of the plate. the order is the rng order as much as who gets
// the room: sun, planets biggest first, then the moons
private fun cast(d: Dimension) {
    castSun(d)
    castPlanets(d)
    castMoons(d)
}

// in a corner, any of the four
private fun castSun(d: Dimension) {
    sunX = d.wf * rng.rndf(0.05f, 0.28f); sunY = d.hf * rng.rndf(0.05f, 0.28f)
    if (rng.rndb()) sunX = d.wf - sunX
    if (rng.rndb()) sunY = d.hf - sunY
    sun = Body(sunX, sunY, SUNR, axis())
}

// one giant, two middling, the rest small. 300 throws each, one that fits nowhere is left out
private fun castPlanets(d: Dimension) {
    val radii = List(BODIES) { i ->
        when {
            i == 0 -> rng.rndf(120f, 190f)
            i < 3 -> rng.rndf(45f, 95f)
            else -> rng.rndf(14f, 36f)
        }
    }
    for (r in radii) {
        for (attempt in 0 until 300) {
            val x = d.wf * rng.rndf(0.1f, 0.9f); val y = d.hf * rng.rndf(0.1f, 0.9f)
            if (!fits(x, y, r, 18f)) continue
            bodies += Body(x, y, r, axis())
            break
        }
    }
}

// hung off anything large enough. half of them straight down-sun so they sit in the shadow
private fun castMoons(d: Dimension) {
    for (p in bodies.toList()) {
        if (p.r < 60f) continue
        repeat(rng.rndi(0, 3)) {
            val r = p.r * rng.rndf(0.12f, 0.28f)
            val away = atan2(p.y - sunY, p.x - sunX)
            val a = if (rng.rndb()) away + rng.rndf(-0.12f, 0.12f) else rng.rndf(0f, 2 * PIf)
            val dist = p.r + r + rng.rndf(10f, 55f)
            val x = p.x + cos(a) * dist; val y = p.y + sin(a) * dist
            if (x < 0f || x > d.wf || y < 0f || y > d.hf) return@repeat
            if (fits(x, y, r, 8f)) bodies += Body(x, y, r, axis())
        }
    }
}

private fun fits(x: Float, y: Float, r: Float, gap: Float): Boolean {
    if (hypot(x - sunX, y - sunY) < r + SUNR + 40f) return false
    return bodies.none { hypot(x - it.x, y - it.y) < r + it.r + gap }
}

// a pole leaning some way
private fun axis(): Vec3 {
    val a = rng.rndf(0f, 2 * PIf)
    val e = rng.rndf(-0.7f, 0.7f)
    return Vec3(cos(a) * cos(e), sin(a) * cos(e), sin(e))
}

// light on a body: the direction to the sun in the plane, tipped a little toward the viewer so
// we see a bit more than half. all one sun so this is per body, not per point
private fun light(b: Body): Vec3 {
    var lx = sunX - b.x; var ly = sunY - b.y
    val l = hypot(lx, ly).coerceAtLeast(1e-3f); lx /= l; ly /= l
    val lz = 0.3f
    val n = sqrt(1f + lz * lz)
    return Vec3(lx / n, ly / n, lz / n)
}

// is the 3d point (px,py,pz), relative to the plate, inside some other bodys shadow cylinder.
// minR skips small occluders - a moon cant put a whole planet in the dark
private fun shadowed(px: Float, py: Float, pz: Float, self: Body, L: Vec3, minR: Float = 0f): Boolean {
    for (o in bodies) {
        if (o === self || o.r < minR) continue
        val dx = px - o.x; val dy = py - o.y; val dz = pz
        val t = dx * L.x + dy * L.y + dz * L.z
        if (t > 0f) continue   // sun side of it
        val qx = dx - t * L.x; val qy = dy - t * L.y; val qz = dz - t * L.z
        if (qx * qx + qy * qy + qz * qz < o.r * o.r) return true
    }
    return false
}

// ==== the lathe ====

private val xs = FloatArray(8192); private val ys = FloatArray(8192); private val hw = FloatArray(8192)

// cut in red, lit from inside so the shade is limb darkening, and the relief runs finer and
// rougher so it reads as granulation with a few dark spots
private fun drawSun(c: Canvas, s: Body) {
    val bump = rng.rndf(5f, 8f)
    lathe(c, s, fillOf(colorBold), along = bump, across = bump, rough = GRAIN) { _, _, pz ->
        0.6f + 0.4f * pz / s.r }
}

// planets and moons are the same thing at different sizes: lit by the sun, dark where another
// body stands in the way
private fun drawPlanet(c: Canvas, b: Body) {
    val L = light(b)
    // a moon with its centre in the dark is a blood moon, cut in red and a touch lighter
    val red = shadowed(b.x, b.y, 0f, b, L, minR = b.r)
    val ink = fillOf(if (red) colorBold else colorInk)
    // relief is noise on the surface. a few features per body whatever its size, and some bodies
    // get it stretched round the parallels so it reads as bands, gas giant style
    val bump = rng.rndf(1.5f, 3.5f)
    val banded = rng.rndf() < 0.4f
    val along = if (banded) rng.rndf(4f, 8f) else bump
    val across = if (banded) 0.6f else bump
    lathe(c, b, ink, along, across, rough = RELIEF, weight = if (red) 0.7f else 1f) { px, py, pz ->
        var lam = (px * L.x + py * L.y + pz * L.z) / b.r
        if (lam < 0f) lam = 0f
        if (!red && shadowed(b.x + px, b.y + py, pz, b, L)) lam *= ECLIPSE
        lam
    }
}

// turns one body: a ribbon per parallel, as wide as the light there times the relief noise
// (stretched along/across the parallels, rough is how much). shade(px, py, pz) is the light
// at a surface point, relative to the centre, 0..1. weight scales the whole cut
private inline fun lathe(
    c: Canvas, b: Body, ink: Paint,
    along: Float, across: Float, rough: Float, weight: Float = 1f,
    shade: (Float, Float, Float) -> Float,
) {
    c.save()
    c.clipPath(PathBuilder().addCircle(b.x * SS, b.y * SS, b.r * SS).detach())
    c.clear(colorBack)
    val n = max(3, (PIf * b.r / PITCH).toInt())
    for (k in 0 until n) {
        val phi = -PIf / 2 + (k + 0.5f) * PIf / n
        val cp = cos(phi); val sp = sin(phi)
        val cr = cp * b.r; val sr = sp * b.r
        val m = max(24, (2 * PIf * cr / 1.5f).toInt())
        // walk the parallel, keeping the runs on the near side
        var cnt = 0
        val start = firstHidden(b, cr, sr, m)
        for (j in 0..m) {
            val i = (start + j) % m
            val th = i * 2 * PIf / m
            val ct = cos(th); val st = sin(th)
            val px = b.sx(cr, sr, ct, st); val py = b.sy(cr, sr, ct, st); val pz = b.sz(cr, sr, ct, st)
            if (pz <= 0f) { flush(c, ink, cnt); cnt = 0; continue }
            val lam = shade(px, py, pz)
            // relief in the bodys own frame, (sp, cp ct, cp st) being the point on its unit sphere, so
            // along stretches it round the parallels into bands. b.r in the second slot keeps bodies apart
            val relief = 1f + rough * SimplexNoise.noise(sp * along + NZOFF, cp * ct * across + b.r, cp * st * across)
            val mz = -sp * (ct * b.uz + st * b.vz) + cp * b.az   // z of the meridian tangent
            val w = cut(lam, relief, weight, mz)
            xs[cnt] = (b.x + px) * SS; ys[cnt] = (b.y + py) * SS; hw[cnt] = w * SS * 0.5f
            cnt++
        }
        flush(c, ink, cnt)
    }
    c.restore()
}

// first sample of the parallel on the far side, so the walk starts hidden and a run never gets
// split at the seam. 0 if there is none, then the whole circle is in view anyway
private fun firstHidden(b: Body, cr: Float, sr: Float, m: Int): Int {
    for (i in 0 until m) {
        val th = i * 2 * PIf / m
        if (b.sz(cr, sr, cos(th), sin(th)) <= 0f) return i
    }
    return 0
}

// the width of one line: a hair in the dark up to fill of the pitch in the light, times the
// relief. capped at the room it has - the parallels crowd toward the limb and lines that
// overlap there moire into rings, so the lit limb goes solid and the dark one fades to nothing
private fun cut(lam: Float, relief: Float, weight: Float, mz: Float): Float {
    var w = HAIR + (PITCH * FILL - HAIR) * lam.pow(GAMMA) * relief
    w *= weight
    val room = PITCH * sqrt(1f - mz * mz)
    return w.coerceAtMost(room * FILL + HAIR * (room / 1.5f).coerceAtMost(1f))
}

private fun flush(c: Canvas, ink: Paint, cnt: Int) {
    if (cnt > 1) c.drawPath(outlineOf(xs, ys, hw, cnt), ink)
}

// ==== the pond ====

// plain circles round the sun, the gap opening and the line fading going out. tried wobbles,
// spiral phase steps, gaps that breathe into wave fronts - all of it fought the planets
private fun drawRings(c: Canvas, d: Dimension) {
    val far = hypot(d.wf, d.hf)
    // seam of each ring points from the frame centre out through the sun, where nobody looks
    val seam = atan2(sunY - d.cy, sunX - d.cx)
    var r = SUNR * 1.12f
    for (k in 0 until RINGS) {
        if (r > far) break
        val t = k / RINGS.toFloat()
        val ink = fillOf(alpha(colorInk, (235 * (1f - t).pow(1.2f)).toInt().coerceAtLeast(24)))
        drawRing(c, r, RINGW * (1f - 0.7f * t), seam, ink)
        r += RGAP * (1f + 0.04f * k)
    }
}

// one ring, w wide. a body in the way still throws a shadow: the ring runs through it thinned
// to the eclipse weight
private fun drawRing(c: Canvas, r: Float, w: Float, seam: Float, ink: Paint) {
    val m = max(96, (2 * PIf * r / 2f).toInt()).coerceAtMost(8000)
    var cnt = 0
    for (i in 0..m) {
        val th = seam + i * 2 * PIf / m
        val x = sunX + cos(th) * r; val y = sunY + sin(th) * r
        val ww = if (lit(x, y)) w else w * ECLIPSE
        xs[cnt] = x * SS; ys[cnt] = y * SS; hw[cnt] = ww * SS * 0.5f
        cnt++
    }
    flush(c, ink, cnt)
}

// can the point see the sun, in the plane
private fun lit(x: Float, y: Float) = bodies.none { segmentHitsCircle(sunX, sunY, x, y, it.x, it.y, it.r) }
