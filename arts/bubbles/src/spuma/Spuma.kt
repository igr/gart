package spuma

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.bluef
import dev.oblac.gart.color.darken
import dev.oblac.gart.color.greenf
import dev.oblac.gart.color.redf
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.pl
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.between
import dev.oblac.gart.math.frac
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.smin
import dev.oblac.gart.noise.fbm
import dev.oblac.gart.noise.noiseOffset
import dev.oblac.gart.util.parallelBands
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * spuma - a raft of soap bubbles, seen from above.
 *
 * blobs that touch dont stack here, they *fuse*: where two films meet they share one flat
 * wall (the radical axis - the maths hands you plateau's geometry for free), three walls
 * meet in a bright node, and every cell wears its interference colours in rings parallel
 * to its own edge, the way a draining film actually banks its thickness. all of it is one
 * per-pixel shader over the packed circles: no strokes, no fills, just optics.
 *
 * the raft is packed big to small until the gaps are full of gap-fillers, on one big disc
 * that sits mostly off the page - its edge sweeps through the frame as a single long curve
 * and the dark past it stays empty.
 */
private const val W = 1200
private const val H = 1200

private const val SS = 2 // 2x2 supersampling; the films are a pixel and a half wide

private data class Params(
    val seed: Long,
    val out: String,
    val pal: Int, val flip: Int,
    // the raft
    val rx: Float, val ry: Float,
    val raft: Float, val rim: Float,
    val rmax: Float, val rmin: Float,
    val fuse: Float,
    val tries: Int, val nmax: Int,
    val sats: Int,
    val popp: Float, val ghost: Float,
    // the film
    val ringw: Float, val fade: Float, val slice: Float,
    val ringamp: Float, val filmw: Float, val filmamp: Float,
    val wob: Float, val wfreq: Float,
    val jw: Float,
    val glint: Float,
    val liqt: Float, val halo: Float, val pool: Float,
    // the room
    val lightg: Float,
    val mottle: Float, val grain: Float, val vig: Float,
)

private val p = resolveParams()
private val rnd = Random(p.seed)

private val nzoff = noiseOffset(p.seed)

// the inks /////

private val ramp = Palettes.coolPalette(p.pal).let { if (p.flip != 0) it.reversed() else it }

// the liquid the raft floats on, and the void past the raft. both pulled off the ramp so any
// palette brings its own night with it
private val liq = darken(ramp.sample(p.liqt), 0.80f)
private val void = darken(liq, 0.45f)
private val liqR = redf(liq)
private val liqG = greenf(liq)
private val liqB = bluef(liq)
private val voidR = redf(void)
private val voidG = greenf(void)
private val voidB = bluef(void)

// the fixed parts of the optics, so the shader isnt re-deriving them five million times.
// each one keeps the multiply order it was tuned with
private val poolSigma = p.raft * W * 0.6f
private val pool2 = 2f * poolSigma * poolSigma
private val film2 = 2f * p.filmw * p.filmw
private val jw2 = 2f * p.jw * p.jw

// what a burst cell keeps of its rings, its wall and its window. at ghost 0 it drops to a
// black hole in the raft, and two of those side by side eat the whole picture
private val ghostRing = 0.10f + 0.80f * p.ghost
private val ghostFilm = 0.35f + 0.60f * p.ghost
private val ghostGlint = 0.25f + 0.65f * p.ghost

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("spuma", W, H)

    println(gart)
    println("seed=${p.seed} pal=${p.pal} flip=${p.flip} rx=${p.rx} raft=${p.raft} rmax=${p.rmax} fuse=${p.fuse} ringw=${p.ringw} fade=${p.fade}")

    val g = gart.gartvas()
    render(g)

    val output = p.out.ensureExtension("png")
    gart.saveImage(g, output)

    if (!headless) gart.window().showImage(g)
}

// the raft =================================

private class Bub(val x: Float, val y: Float, val r: Float, val phase: Float, val pop: Boolean) // phase je dokle se iscedio, pop da li je vec pukao

// big bubbles claim their spot first, the small ones squeeze into what is left - packing
// order is doing the size distribution, not a distribution. the raft itself is one big disc
// mostly off the page, so its edge sweeps through the frame as a single long curve
private fun buildRaft(): List<Bub> {
    val out = ArrayList<Bub>()
    val cx = W * p.rx
    val cy = H * p.ry
    val rad = p.raft * W

    // the raft edge wanders, so the sweep has capes and bays instead of being a compass arc
    fun domain(x: Float, y: Float): Float {
        val nx = (x - cx) / rad
        val ny = (y - cy) / rad
        val ang = atan2(ny, nx)
        val edge = 1f + p.rim * fbm(cos(ang) * 1.7f + nzoff + 31f, sin(ang) * 1.7f, octaves = 2, lacunarity = 2.1f, gain = 0.5f)
        return hypot(nx, ny) / edge
    }

    val m = W * 0.06f // run past the frame so the raft bleeds off it cleanly
    for (i in 0 until p.tries) {
        if (out.size >= p.nmax) break
        val t = (i.toFloat() / p.tries).pow(0.55f) // 0.55 da veliki ne pojedu sve pokusaje, sitnima treba vise
        val r = W * lerp(p.rmax, p.rmin, t) * rnd.between(0.82f, 1.18f)

        val x = rnd.between(-m, W + m)
        val y = rnd.between(-m, H + m)
        if (domain(x, y) > 1f) continue

        // too deep a merge and the wall maths goes ugly; and dont let a big one swallow it whole
        val crowded = out.any { o ->
            val d = hypot(x - o.x, y - o.y)
            d < (r + o.r) * (1f - p.fuse) || d < o.r - r * 0.55f
        }
        if (crowded) continue
        out += Bub(x, y, r, rnd.between(0f, 1f), rnd.nextFloat() < p.popp)
    }

    // a few loners drifted off the raft. they get no walls, just their own ringed skin
    var placed = 0
    var guard = 0
    while (placed < p.sats && guard < 500) { // guard da ne vrti zauvek kad nema mesta
        guard++
        val ang = rnd.between(0f, TAUf)
        val nr = rnd.between(1.04f, 1.3f)
        val x = cx + cos(ang) * rad * nr
        val y = cy + sin(ang) * rad * nr
        val r = W * rnd.between(0.008f, 0.022f)
        if (x < r + 8 || x > W - r - 8 || y < r + 8 || y > H - r - 8) continue
        if (out.any { hypot(x - it.x, y - it.y) < (r + it.r) * 1.06f }) continue
        out += Bub(x, y, r, rnd.between(0f, 1f), false)
        placed++
    }
    return out
}

// the raft as the shader wants it: flat arrays, plus spatial buckets so a sample only ever
// asks its neighbourhood. the bucket margin covers film + halo
private const val BS = 72 // kofa od 72px, po oku
private class Foam(bubs: List<Bub>) {
    val bx = FloatArray(bubs.size) { bubs[it].x }
    val by = FloatArray(bubs.size) { bubs[it].y }
    val br = FloatArray(bubs.size) { bubs[it].r }
    val bp = FloatArray(bubs.size) { bubs[it].phase }
    val pop = BooleanArray(bubs.size) { bubs[it].pop }
    val rmax = br.max()

    // where the raft sits, for the pool of light underneath it
    val cxm = bx.average().toFloat()
    val cym = by.average().toFloat()

    private val gw = ceil(W.toFloat() / BS).toInt()
    private val gh = ceil(H.toFloat() / BS).toInt()
    private val cells: Array<IntArray>

    init {
        val tmp = Array(gw * gh) { ArrayList<Int>(8) }
        val m = 30f
        bubs.forEachIndexed { i, bb ->
            val x0 = max(0, ((bb.x - bb.r - m) / BS).toInt())
            val x1 = min(gw - 1, ((bb.x + bb.r + m) / BS).toInt())
            val y0 = max(0, ((bb.y - bb.r - m) / BS).toInt())
            val y1 = min(gh - 1, ((bb.y + bb.r + m) / BS).toInt())
            for (gy in y0..y1) for (gx in x0..x1) tmp[gy * gw + gx].add(i)
        }
        cells = Array(gw * gh) { tmp[it].toIntArray() }
    }

    fun at(x: Float, y: Float): IntArray {
        val gx = min(gw - 1, max(0, (x / BS).toInt()))
        val gy = min(gh - 1, max(0, (y / BS).toInt()))
        return cells[gy * gw + gx]
    }
}

// the optics =================================

// a round falloff, the shape every soft thing in here is made of. den is two sigma squared,
// worked out at the call site so the maths stays in the order it was tuned in
private fun gauss(dx: Float, dy: Float, den: Float) = exp(-(dx * dx + dy * dy) / den)

// one sample of the foam. rgb out through a flat array, because seven million boxed colour
// objects is how a render dies
private fun sample(sx: Float, sy: Float, f: Foam, out: FloatArray) {
    val cands = f.at(sx, sy)

    // owner = least power distance. thats the whole voronoi
    var pd1 = Float.MAX_VALUE
    var i1 = -1
    for (ci in cands) {
        val dx = sx - f.bx[ci]
        val dy = sy - f.by[ci]
        val pd = dx * dx + dy * dy - f.br[ci] * f.br[ci]
        if (pd < pd1) {
            pd1 = pd; i1 = ci
        }
    }

    if (i1 < 0 || pd1 >= 0f) { // van splava
        // off the raft: the void, a wide pool of light the liquid catches under the colony,
        // and a breath of halo where a film is close
        val pool = p.pool * gauss(sx - f.cxm, sy - f.cym, pool2)
        var vr = lerp(voidR, liqR + 0.05f, pool)
        var vg = lerp(voidG, liqG + 0.05f, pool)
        var vb = lerp(voidB, liqB + 0.05f, pool)
        if (i1 >= 0 && p.halo > 0f) {
            val d = sqrt(pd1 + f.br[i1] * f.br[i1]) - f.br[i1]
            val hI = p.halo * exp(-d / 7f)
            // not a lerp - the lift sits inside the difference. tidy it into one and the render moves
            vr += (liqR - vr + 0.10f) * hI
            vg += (liqG - vg + 0.10f) * hI
            vb += (liqB - vb + 0.10f) * hI
        }
        out[0] = vr; out[1] = vg; out[2] = vb
        return
    }

    val bx1 = f.bx[i1]
    val by1 = f.by[i1]
    val r1 = f.br[i1]
    val popped = f.pop[i1]
    val dx1 = sx - bx1
    val dy1 = sy - by1
    val d1 = sqrt(dx1 * dx1 + dy1 * dy1)

    // distance to my own skin, and to the two nearest boundaries overall - wall+wall makes a
    // triple node, wall+skin the bright pinch where an inner film lands on the raft's rim.
    // us runs through a soft-min so the rings round their corners deep in the cell while the
    // wall lines themselves stay dead crisp on the raw distance
    var w1 = r1 - d1
    var w2 = Float.MAX_VALUE
    var us = w1
    val sk = 0.16f * r1 // corner rounding radius for the bands
    for (ci in cands) {
        if (ci == i1) continue
        val cd = hypot(f.bx[ci] - bx1, f.by[ci] - by1)
        if (cd < 1e-3f) continue
        val dxk = sx - f.bx[ci]
        val dyk = sy - f.by[ci]
        val pdk = dxk * dxk + dyk * dyk - f.br[ci] * f.br[ci]
        val w = (pdk - pd1) / (2f * cd) // exact distance to the radical axis, not an approximation
        if (w < w1) {
            w2 = w1; w1 = w
        } else if (w < w2) {
            w2 = w
        }
        us = smin(w, us, sk)
    }

    val u = max(0f, w1)
    val ur = max(0f, us)

    // rings parallel to the cell edge. the wavelength barely moves with size, so a small
    // bubble only ever fits one pale wash of colour and the giants bank up three or four
    // bands - which is exactly how a raft drains, young skins plain, old ones striped
    val rq = r1 / f.rmax
    val lam = p.ringw * (0.45f + 1.1f * sqrt(rq))
    val tau = ur / lam + f.bp[i1] * 1.7f +
        fbm(sx * p.wfreq + nzoff + 411f, sy * p.wfreq, octaves = 2, lacunarity = 2.2f, gain = 0.5f) * p.wob
    val tri = 1f - abs(2f * frac(tau) - 1f) // pingpong, so the ramp cycles without a seam

    // every bubble owns a slice of the ramp instead of the whole of it - neighbours at
    // different drain stages is what makes the raft read in colour chords, not one hue.
    // and the film burns brightest right against the wall and cools going in - without that
    // lift the whole raft sits at one dusty exposure
    val wc = 0.18f + 0.64f * f.bp[i1]
    val ring = ramp.sample(wc + (tri - 0.5f) * p.slice)
    val lift = 0.30f * exp(-ur / (lam * 0.9f))
    val rr = lerp(redf(ring), 1f, lift)
    val rg = lerp(greenf(ring), 1f, lift)
    val rb = lerp(bluef(ring), 1f, lift)

    var ringI = p.ringamp * exp(-ur / (p.fade * lam)) * (0.35f + 0.65f * min(1f, rq / 0.75f))
    var ridge = p.filmamp * exp(-(u * u) / film2)
    if (popped) {
        ringI *= ghostRing
        ridge *= ghostFilm
    }

    // colour goes on in three coats: rings, then the bright film line, then the node
    var lr = lerp(liqR, rr, ringI)
    var lg = lerp(liqG, rg, ringI)
    var lb = lerp(liqB, rb, ringI)

    val fr = lerp(rr, 1f, 0.78f)
    val fg = lerp(rg, 1f, 0.78f)
    val fb = lerp(rb, 1f, 0.78f)
    lr = lerp(lr, fr, ridge)
    lg = lerp(lg, fg, ridge)
    lb = lerp(lb, fb, ridge)

    if (w2 < Float.MAX_VALUE) {
        // where a second boundary closes in too, films meet - plateau's node, lit up.
        // not a lerp either: the 0.62 multiplies after j, and that order is the render
        val j = gauss(w1, w2, jw2)
        lr += (fr - lr) * j * 0.62f
        lg += (fg - lg) * j * 0.62f
        lb += (fb - lb) * j * 0.62f
    }

    // the room shows up twice on every bubble: a hard little window up-left and its dim
    // partner across the floor. the gap-fillers are too small to hold a window - on them
    // it would just be sparkle noise, so it fades out under about a dozen px
    val o1 = 0.52f * r1
    val o2 = 0.58f * r1
    val gs = max(2.2f, r1 * 0.105f)
    val gs2 = 2f * gs * gs
    val g1 = gauss(sx - (bx1 - o1 * 0.60f), sy - (by1 - o1 * 0.78f), gs2)
    val g2 = gauss(sx - (bx1 + o2 * 0.55f), sy - (by1 + o2 * 0.74f), gs2 * 0.55f)
    val gmul = ((r1 - 5f) / 14f).coerceIn(0f, 1f)
    val gI = (g1 + g2 * 0.28f) * p.glint * gmul * (if (popped) ghostGlint else 1f)

    out[0] = lerp(lr, 1f, gI)
    out[1] = lerp(lg, 1f, gI)
    out[2] = lerp(lb, 1f, gI)
}

// the exposure -----

private fun render(g: Gartvas) {
    val bubs = buildRaft()
    println("bubbles=${bubs.size} (popped=${bubs.count { it.pop }})")
    val foam = Foam(bubs)

    val m = Gartmap(g)
    val px = m.pixels
    val seed = p.seed.toInt()
    val inv = 1f / (SS * SS)

    // banded over the cores. every row belongs to one thread, so the picture is the same
    // whatever the core count - verify the usual way, render twice and shasum
    parallelBands(H) { y0, y1 ->
        val one = FloatArray(3)
        for (y in y0 until y1) {
            val ky = 0.5f - y.toFloat() / H
            for (x in 0 until W) {
                var ar = 0f
                var ag = 0f
                var ab = 0f
                for (sy in 0 until SS) for (sx in 0 until SS) {
                    sample(x + (sx + 0.5f) / SS, y + (sy + 0.5f) / SS, foam, one)
                    ar += one[0]; ag += one[1]; ab += one[2]
                }

                // one slow key light across the tank, mottle in the liquid, grain over the lot
                val lgr = 1f + p.lightg * (ky + (0.5f - x.toFloat() / W)) +
                    p.mottle * fbm(x * 0.0016f + nzoff, y * 0.0016f, octaves = 2, lacunarity = 2.3f, gain = 0.5f)
                val gn = (hash01(x, y + 1, seed) - 0.5f) * 2f * p.grain * 0.14f

                val r = ((ar * inv * lgr + gn) * 255f).toInt().coerceIn(0, 255)
                val gg = ((ag * inv * lgr + gn) * 255f).toInt().coerceIn(0, 255)
                val b = ((ab * inv * lgr + gn) * 255f).toInt().coerceIn(0, 255)
                px[y * W + x] = (0xFF shl 24) or (r shl 16) or (gg shl 8) or b
            }
        }
    }
    m.drawToCanvas(g)

    g.canvas.drawVignette(g.d, p.vig) // vinjeta za kraj
}

// knobs -----------------------------------------

private fun resolveParams(): Params {
    val p = Params(
        seed = pl("seed", 2L), // 2 je hero
        out = ps("out", "spuma"),
        pal = pi("pal", 34), // any cool palette plays; 34 is the magenta-and-gold two-chord raft
        flip = pi("flip", 0),

        rx = pf("rx", 0.02f), // centre of the raft disc, in page widths. off the page is the point
        ry = pf("ry", 0.50f),
        raft = pf("raft", 0.80f), // its radius, in page widths. 0.8 from 0.02 reaches four fifths across
        rim = pf("rim", 0.12f), // how ragged the sweep is. 0 is a compass arc
        rmax = pf("rmax", 0.07f),
        rmin = pf("rmin", 0.0022f), // the gap-fillers. this is where the density comes from
        fuse = pf("fuse", 0.22f), // how deep neighbours may sink into each other. 0 just kisses and the walls vanish, 0.3 goes honeycomb
        tries = pi("tries", 220000), // the late small tries are the ones that fill the crevices
        nmax = pi("nmax", 2500),
        sats = pi("sats", 0), // loners past the raft edge. the dark mostly wants to stay empty
        popp = pf("popp", 0.02f), // fraction of cells already burst
        ghost = pf("ghost", 0.45f), // how much light a burst cell keeps. 0 is a black hole, 1 you cant tell

        ringw = pf("ringw", 20f), // band wavelength, px, before size scaling
        fade = pf("fade", 0.85f), // how many rings survive before the film goes dark
        slice = pf("slice", 0.38f), // how much of the ramp one bubble may wear. 1 is the lot
        ringamp = pf("ringamp", 0.97f),
        filmw = pf("filmw", 1.35f), // the bright wall line. this is the drawing
        filmamp = pf("filmamp", 0.85f),
        wob = pf("wob", 0.35f), // drift of the bands off true. 0 is machine-turned
        wfreq = pf("wfreq", 0.006f),
        jw = pf("jw", 6f), // node glow radius at triple points
        glint = pf("glint", 0.8f),
        liqt = pf("liqt", 0.5f), // where on the ramp the liquid is sampled from
        halo = pf("halo", 0.30f),
        pool = pf("pool", 0.3f),

        lightg = pf("lightg", 0.10f),
        mottle = pf("mottle", 0.045f),
        grain = pf("grain", 0.10f),
        vig = pf("vig", 0.5f),
    )

    require(p.pal in 1..181) { "pal must be 1..181" }
    require(p.flip in 0..1) { "flip must be 0 or 1" }
    require(p.rx in -1f..2f) { "rx must be between -1 and 2" }
    require(p.ry in -1f..2f) { "ry must be between -1 and 2" }
    require(p.raft in 0.1f..2f) { "raft must be between 0.1 and 2" }
    require(p.rim in 0f..0.5f) { "rim must be between 0 and 0.5" }
    require(p.rmax in 0.02f..0.2f) { "rmax must be between 0.02 and 0.2" }
    require(p.rmin in 0.001f..0.06f && p.rmin < p.rmax) { "rmin must be between 0.001 and 0.06, under rmax" }
    require(p.fuse in 0f..0.45f) { "fuse must be between 0 and 0.45" }
    require(p.tries in 200..300000) { "tries must be between 200 and 300000" }
    require(p.nmax in 10..6000) { "nmax must be between 10 and 6000" }
    require(p.sats in 0..30) { "sats must be between 0 and 30" }
    require(p.popp in 0f..0.5f) { "popp must be between 0 and 0.5" }
    require(p.ghost in 0f..1f) { "ghost must be between 0 and 1" }
    require(p.ringw in 2f..40f) { "ringw must be between 2 and 40" }
    require(p.fade in 0.3f..8f) { "fade must be between 0.3 and 8" }
    require(p.slice in 0.05f..1f) { "slice must be between 0.05 and 1" }
    require(p.ringamp in 0f..1f) { "ringamp must be between 0 and 1" }
    require(p.filmw in 0.4f..6f) { "filmw must be between 0.4 and 6" }
    require(p.filmamp in 0f..1f) { "filmamp must be between 0 and 1" }
    require(p.wob in 0f..2f) { "wob must be between 0 and 2" }
    require(p.wfreq in 0.0005f..0.05f) { "wfreq must be between 0.0005 and 0.05" }
    require(p.jw in 1f..30f) { "jw must be between 1 and 30" }
    require(p.glint in 0f..1f) { "glint must be between 0 and 1" }
    require(p.liqt in 0f..1f) { "liqt must be between 0 and 1" }
    require(p.halo in 0f..1f) { "halo must be between 0 and 1" }
    require(p.pool in 0f..1f) { "pool must be between 0 and 1" }
    require(p.lightg in 0f..0.5f) { "lightg must be between 0 and 0.5" }
    require(p.mottle in 0f..0.5f) { "mottle must be between 0 and 0.5" }
    require(p.grain in 0f..0.5f) { "grain must be between 0 and 0.5" }
    require(p.vig in 0f..1.4f) { "vig must be between 0 and 1.4" }

    return p
}
