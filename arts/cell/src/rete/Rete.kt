package rete

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.hashgrid.BucketGrid
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.pl
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.sminCubic
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.noise.fbm
import dev.oblac.gart.noise.noiseOffset
import dev.oblac.gart.pixels.gaussianBlur
import dev.oblac.gart.util.parallelBands
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * rete - a soap-film net, with a few more of itself out of focus behind it.
 *
 * the net is a foam. discs packed big to small, then every pixel asks which disc *surface*
 * is nearest - apollonius rather than voronoi, so the walls bow out of the small bubbles
 * into the big ones the way pressure bends them. the ink is the gap between the nearest
 * surface and the next one, and a smooth-min over the next few is what tapers every wall
 * into a long black spindle at the node, plateau borders for free. no strokes anywhere: one
 * distance field per layer.
 *
 * depth is cheap. same generator again with another seed, blurred and greyed, multiplied in
 * behind. the eye does the rest.
 *
 * it all drains toward the bottom-right corner. nothing is painted there: the cells just get
 * smaller the closer they sit to the drain, the same ratio per step, and their borders fatten
 * until only the film shows as a white line, and then not even that. so the page turns from
 * black at the corner, through white-line cells, to black-line cells - a gradient of cell size.
 */
private const val W = 1200 // kvadrat
private const val H = 1200

private data class Params(
    val seed: Long,
    val out: String,
    val ss: Int,
    // the foam
    val rbig: Float, val rmin: Float, val step: Float, val rvar: Float,
    val froth: Float, val fscale: Float, val clean: Float, val cap: Float, val small: Float,
    val specks: Float,
    val fuse: Float, val dens: Float,
    // the ink
    val wall: Float, val wvar: Float,
    val knot: Float, val kvar: Float,
    val core: Float, val cored: Float, val film: Float,
    // the drain
    val pool: Float, val poolx: Float, val pooly: Float, val prim: Float,
    val wet: Float, val sink: Float, val flood: Float,
    // the depth
    val layers: Int, val blur: Float, val grey: Float, val fade: Float, val zoom: Float,
    // the page
    val paper: Float, val haze: Float, val grain: Float, val vig: Float,
)

private val p = resolveParams()
private val rnd = Random(p.seed)

private val nzoff = noiseOffset(p.seed)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("rete", W, H)

    println(gart)
    println("seed=${p.seed} layers=${p.layers} rbig=${p.rbig} froth=${p.froth} wall=${p.wall} knot=${p.knot} blur=${p.blur} grey=${p.grey}")

    val g = gart.gartvas()
    render(g)

    gart.saveImage(g, p.out.ensureExtension("png"))
    if (!headless) gart.window().showImage(g)
}

// the foam =================================

// the discs of one layer, split over two grids by size - the giants on a coarse one, everything
// under rsplit on a fine one. one grid sized for the specks made the search crawl across the big
// dry cells, hundreds of empty cells per pixel
private class Foam(val n: Int, val x: FloatArray, val y: FloatArray, val r: FloatArray, val coarse: BucketGrid, val fine: BucketGrid)

// the drain ------

// where the foam is wettest: a wobbly disc off the corner, negative inside. nothing is drawn
// from it, it only steers the packing and the ink. the rim wanders the same way spuma's raft
// edge does, so the gradient isnt a compass arc
private class Pool(val cx: Float, val cy: Float, val r: Float, val rim: Float, val nz: Float) {
    fun dist(x: Float, y: Float): Float {
        val dx = x - cx
        val dy = y - cy
        val ang = atan2(dy, dx)
        val edge = 1f + rim * fbm(cos(ang) * 1.7f + nz + 31f, sin(ang) * 1.7f + 7f, octaves = 2, lacunarity = 2.1f, gain = 0.5f)
        return hypot(dx, dy) - r * edge
    }
}

// big discs claim their spots first and the small ones squeeze into whats left, tier by tier.
// the froth field hands every spot a size window: where its low only the big ones may land,
// where its high the big ones are turned away and the mediums take the patch: big clean
// cells on one side, patches of small ones on the other. the tiny ones are a seperate, sparser
// field: let them into every gap and the patches go solid black with pinholes (tried it,
// looked like a sponge)
private fun pack(seed: Long, scale: Float, nz: Float, pool: Pool?): Foam {
    val rnd = Random(seed)
    val rbig = p.rbig * W * scale
    val rmin = p.rmin * W * scale
    val margin = rbig * 1.5f // sites past the page, so the edge cells dont know where the page ends
    val x0 = -margin
    val y0 = -margin
    val fw = W + 2 * margin
    val fh = H + 2 * margin
    // the split is by eye: the coarse cells are about the size of a medium bubble, so a giant
    // spans a few dozen of them and a speck never gets near it
    val rsplit = rbig * 0.06f
    val csC = rsplit * 4f
    val csF = rmin * 2f
    val coarse = BucketGrid(x0, y0, csC, ceil(fw / csC).toInt() + 1, ceil(fh / csC).toInt() + 1)
    val fine = BucketGrid(x0, y0, csF, ceil(fw / csF).toInt() + 1, ceil(fh / csF).toInt() + 1)

    val room = 60000
    val xs = FloatArray(room)
    val ys = FloatArray(room)
    val rs = FloatArray(room)
    val seen = IntArray(room)
    var tick = 0
    var n = 0

    val lnClean = ln(p.clean)
    val lnCap = ln(p.cap)
    val lnSmall = ln(p.small)
    val lnSink = ln(p.sink)

    // the foam is wetter near the liquid, so it runs finer there - the big dry cells sit
    // furthest from the drain and the shore is all small stuff
    fun froth(x: Float, y: Float, shore: Float): Float {
        val f = fbm(x / W * p.fscale + nz, y / W * p.fscale + nz * 0.7f + 5f, octaves = 3, lacunarity = 2.1f, gain = 0.5f)
        val lo = 1f - p.froth
        return smoothstep(lo - 0.1f, lo + 0.1f, 0.5f + f + 0.5f * shore)
    }

    fun speck(x: Float, y: Float): Boolean {
        val f = fbm(x / W * p.fscale * 2.5f + nz + 41f, y / W * p.fscale * 2.5f + nz * 0.7f + 23f, octaves = 3, lacunarity = 2.1f, gain = 0.5f)
        return 0.5f + f > 1f - p.specks
    }

    fun clear(g: BucketGrid, x: Float, y: Float, r: Float): Boolean {
        g.forEachCellTouching(x, y, r) { c ->
            g.forEachIn(c) { j ->
                if (seen[j] != tick) {
                    seen[j] = tick
                    val dx = xs[j] - x
                    val dy = ys[j] - y
                    val lim = r + rs[j] - p.fuse * min(r, rs[j])
                    if (dx * dx + dy * dy < lim * lim) return false
                }
            }
        }
        return true
    }

    fun fits(x: Float, y: Float, r: Float): Boolean {
        tick++
        return clear(coarse, x, y, r) && clear(fine, x, y, r)
    }

    // od najvecih ka najmanjima, sitni se uguraju gde je ostalo mesta
    var tier = rbig
    while (tier >= rmin && n < room) {
        // tries scale with how many of this size would fit, so every tier gets about as saturated
        val tries = ceil(p.dens * fw * fh / (tier * tier)).toInt()
        for (t in 0 until tries) {
            if (n >= room) break
            val x = x0 + rnd.nextFloat() * fw
            val y = y0 + rnd.nextFloat() * fh
            val dp = pool?.dist(x, y) ?: Float.MAX_VALUE
            val shore = smoothstep(p.wet * W * scale, 0f, dp)
            val f = froth(x, y, shore)
            val r = tier * (1f - p.rvar * rnd.nextFloat())
            // on top of the froth window the whole window slides down toward the drain, geometric
            // so every step closer shrinks the cells by the same ratio. thats the gradient. the
            // ceiling never drops under the smallest tier, or the drain would end up empty and
            // the cells around it would stretch across it
            val sz = exp(lnSink * shore)
            if (r > max(rbig * exp(lnCap * f) * sz, rmin * 1.6f)) continue
            if (r < rbig * exp(lerp(lnClean, lnSmall, f)) * sz && !speck(x, y)) continue
            if (!fits(x, y, r)) continue
            xs[n] = x
            ys[n] = y
            rs[n] = r
            (if (r >= rsplit) coarse else fine).add(n, x, y, r)
            n++
        }
        tier *= p.step
    }
    // posle ovoga se nista vise ne dodaje, prefiksne sume racunaju na to
    coarse.seal()
    fine.seal()
    return Foam(n, xs, ys, rs, coarse, fine)
}

// the ink ***

// the four nearest surfaces to one pixel. a class rather than local funs so the hot loop
// doesnt box its counters
private class Probe(private val foam: Foam) {
    val bd = FloatArray(4) // cetiri je dosta, smin gleda samo drugog, treceg i cetvrtog
    val bi = IntArray(4)
    private val seen = IntArray(foam.n)
    private var tick = 0
    private var x = 0f
    private var y = 0f

    fun reset(x: Float, y: Float) {
        this.x = x
        this.y = y
        tick++
        bd.fill(Float.MAX_VALUE)
        bi.fill(-1)
    }

    fun visit(g: BucketGrid, c: Int) {
        g.forEachIn(c) { j ->
            if (seen[j] == tick) return@forEachIn
            seen[j] = tick
            val dx = foam.x[j] - x
            val dy = foam.y[j] - y
            val d = sqrt(dx * dx + dy * dy) - foam.r[j]
            if (d >= bd[3]) return@forEachIn
            var k = 3
            while (k > 0 && bd[k - 1] > d) { bd[k] = bd[k - 1]; bi[k] = bi[k - 1]; k-- }
            bd[k] = d
            bi[k] = j
        }
    }
}

// paints one layer into cov, 0 paper 1 ink, at supersampled size. film gets the bright line
// down the middle of every wall (the film itself, seen through the border) so the core pass
// can gate it before the two are merged
private fun shade(foam: Foam, cov: FloatArray, film: FloatArray, sw: Int, sh: Int, seed: Int, rbig: Float, pool: Pool?) {
    val ss = p.ss
    val aa = 1f / ss
    val coarse = foam.coarse
    val fine = foam.fine
    val tauMax = p.wall * 0.5f * (1f + p.wvar)

    parallelBands(sh) { y0, y1 ->
        val pr = Probe(foam)
        val bd = pr.bd
        val bi = pr.bi
        for (py in y0 until y1) {
            val y = (py + 0.5f) / ss
            for (px in 0 until sw) {
                val x = (px + 0.5f) / ss
                // wet foam near the drain: the borders fatten until the cells go solid black and
                // all thats left of them is the bright film line. 0 on a page with no drain
                val dp = pool?.dist(x, y) ?: Float.MAX_VALUE
                val shore = if (pool != null) smoothstep(p.wet * W, 0f, dp) else 0f
                val fat = 1f + (p.flood - 1f) * shore * sqrt(shore)
                val kfat = 1f + 2f * shore
                pr.reset(x, y)
                val ccx = coarse.col(x)
                val ccy = coarse.row(y)
                val fcx = fine.col(x)
                val fcy = fine.row(y)
                // the two grids are walked ring by ring in step, always the one whose next ring
                // is nearer, so everything not yet seen is at least min(boundC, boundF) away
                var mC = 0
                var mF = 0
                var boundC = -coarse.cs // nothing first met on ring m is closer than (m - 1) * cs
                var boundF = -fine.cs
                while (true) {
                    val bound = min(boundC, boundF)
                    if (bound > bd[3]) break
                    if (bi[0] >= 0) {
                        // a surface further past the nearest one than this cant touch the ink here,
                        // smin dip included. once bound is past bd[0] the owner is final, so its own
                        // k is the right one to bound with - a giant's reach is not a speck's. deep
                        // inside a disc this stops the search after the first ring, most of the page
                        val kmax = p.knot * min(1f, foam.r[bi[0]] / rbig) * (1f + p.kvar) * kfat
                        if (bound - bd[0] > 2f * (tauMax * fat + aa + 1.25f * kmax)) break
                    }
                    if (boundC <= boundF) {
                        if (coarse.ring(ccx, ccy, mC) { pr.visit(coarse, it) }) { mC++; boundC = (mC - 1) * coarse.cs } else boundC = Float.MAX_VALUE
                    } else {
                        if (fine.ring(fcx, fcy, mF) { pr.visit(fine, it) }) { mF++; boundF = (mF - 1) * fine.cs } else boundF = Float.MAX_VALUE
                    }
                    if (boundC == Float.MAX_VALUE && boundF == Float.MAX_VALUE) break
                }

                val i = bi[0]
                val ri = if (i >= 0) foam.r[i] else 0f
                var c = 0f
                var line = 0f
                if (i >= 0 && bi[1] >= 0) {
                    // fillets scale with the bubble, straight linear - plateau borders do, for a given
                    // wetness. anything slower leaves k near the size of the small cells and the smin
                    // rounds them into pebbles instead of just knocking off their corners. cubic smin,
                    // not quad: quads deeper dip made the nodes blobs, and the probes early-out leans
                    // on cubic staying exact outside its band
                    val k = p.knot * min(1f, ri / rbig) * (1f + p.kvar * (hash01(i, 7, seed) - 0.5f) * 2f) * kfat
                    var dd = 0f
                    for (j in 1..3) {
                        val jj = bi[j]
                        if (jj < 0) break
                        // films between little bubbles run thinner than between the giants
                        val thin = (0.55f + 0.45f * sqrt(min(1f, min(ri, foam.r[jj]) / rbig))) * fat
                        val tau = p.wall * 0.5f * thin * (1f + p.wvar * (hash01(min(i, jj), max(i, jj), 3, seed) - 0.5f) * 2f)
                        val t = (bd[j] - bd[0]) * 0.5f - tau
                        dd = if (j == 1) t else sminCubic(dd, t, k)
                    }
                    c = (0.5f - dd * ss).coerceIn(0f, 1f)
                    // the film line sits where the two nearest surfaces tie. grey in a dry node, white
                    // by the drain, and it thins and fades with the cell - thats what takes the
                    // smallest cells the last step to solid black, no liquid painted over anything
                    val lw = p.film * (1f + 0.5f * shore) * min(1f, ri / (0.05f * rbig))
                    val show = lerp(p.core, 1f, shore) * smoothstep(0.02f * rbig, 0.055f * rbig, ri)
                    line = smoothstep(lw + aa, lw - aa, (bd[1] - bd[0]) * 0.5f) * show
                }
                cov[py * sw + px] = c
                film[py * sw + px] = line
            }
        }
    }
}

// the page ======

private fun render(g: Gartvas) {
    val ss = p.ss
    val sw = W * ss
    val sh = H * ss
    val acc = FloatArray(sw * sh) { 1f }
    val cov = FloatArray(sw * sh)
    val tmp = FloatArray(sw * sh)
    val aux = FloatArray(sw * sh)
    val film = FloatArray(sw * sh)

    // all the seeds up front, so adding a layer at the back doesnt reshuffle the front
    val seeds = LongArray(p.layers) { rnd.nextLong() }

    // only the front net drains. the ones behind are other nets, dry
    val pool = if (p.pool > 0f) Pool(W * p.poolx, H * p.pooly, W * p.pool, p.prim, nzoff) else null

    // od pozadine ka napred
    for (k in p.layers - 1 downTo 0) {
        val scale = p.zoom.pow(k)
        val wet = if (k == 0) pool else null
        val foam = pack(seeds[k], scale, nzoff + k * 17.3f, wet)
        shade(foam, cov, film, sw, sh, (seeds[k] xor (seeds[k] ushr 32)).toInt(), p.rbig * W * scale, wet)
        if (p.core > 0f) {
            // a thick film is a lens: a fat border goes light down the middle and stays
            // black along the edges. the line itself comes from the distance field, but its only
            // let through where a blur of the ink says the ink is wide - a blur is a soft erosion,
            // only ink wider than about three sigma gets near 1 - so thin films stay solid and
            // the solid black by the drain shows its whole net of film lines
            System.arraycopy(cov, 0, aux, 0, cov.size)
            gaussianBlur(aux, sw, sh, p.cored / 2.9f * ss, tmp)
            parallelBands(sh) { y0, y1 -> for (i in y0 * sw until y1 * sw) cov[i] *= 1f - film[i] * smoothstep(0.93f, 0.995f, aux[i]) }
        }
        val sigma = p.blur * k * ss
        if (sigma > 0.3f) gaussianBlur(cov, sw, sh, sigma, tmp)
        // front layer is ink, the rest go greyer the further back they sit
        val ink = if (k == 0) 1f else p.grey * p.fade.pow(k - 1)
        parallelBands(sh) { y0, y1 -> for (i in y0 * sw until y1 * sw) acc[i] *= 1f - ink * cov[i] }
        println("layer $k: ${foam.n} bubbles, scale $scale, blur ${p.blur * k}, ink $ink")
    }

    val m = Gartmap(g)
    val px = m.pixels
    val seed = p.seed.toInt()
    val inv = 1f / (ss * ss)
    parallelBands(H) { y0, y1 ->
        for (y in y0 until y1) for (x in 0 until W) {
            var s = 0f
            for (sy in 0 until ss) {
                val row = (y * ss + sy) * sw + x * ss
                for (sx in 0 until ss) s += acc[row + sx]
            }
            s *= inv
            // the backlight isnt even. a slow drift in the paper is what sells it
            val hz = fbm(x * 0.0013f + nzoff, y * 0.0013f + 3f, octaves = 2, lacunarity = 2.3f, gain = 0.5f)
            val paper = p.paper * (1f - p.haze * (0.5f + hz))
            val gn = (hash01(x, y, seed) - 0.5f) * p.grain * 0.3f // zrno, jedva se vidi, ali bez njega je previse cisto
            val v = ((s * paper + gn) * 255f).toInt().coerceIn(0, 255)
            px[y * W + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
    }
    m.drawToCanvas(g)

    g.canvas.drawVignette(g.d, p.vig)
}

// knobs -----------------

private fun resolveParams(): Params {
    val p = Params(
        seed = pl("seed", 3L), // 3 je hero
        out = ps("out", "rete"),
        ss = pi("ss", 2, 1..4),

        rbig = pf("rbig", 0.14f, 0.03f..0.3f), // biggest bubble, in page widths. 0.1 gave 600 cells in front and read as texture
        rmin = pf("rmin", 0.002f, 0.001f..0.05f), // smallest, at the drain. 2.4px: small enough to read as black once the film line has faded
        step = pf("step", 0.68f, 0.4f..0.9f), // size ratio between tiers
        rvar = pf("rvar", 0.3f, 0f..0.6f), // how much a tier wanders below its nominal size
        froth = pf("froth", 0.3f, 0f..1f), // roughly the share of the page that fills with small bubbles. past 0.5 the giants stop carrying the page
        fscale = pf("fscale", 0.7f, 0.3f..5f), // froth field periods per page. under 1 is one clean region against one patch
        clean = pf("clean", 0.45f, 0.1f..1f), // smallest bubble allowed where its clean, as a fraction of rbig. lower and the giants walls start to bow
        cap = pf("cap", 0.28f, 0.05f..1f), // biggest bubble allowed where its frothy, same units
        small = pf("small", 0.12f, 0.02f..1f), // and the smallest there. below this only the specks go
        specks = pf("specks", 0.2f, 0f..1f), // how much of the page gets clusters of tiny bubbles in its gaps. noisy, not a true share
        fuse = pf("fuse", 0.08f, 0f..0.4f), // how far neighbours may sink into each other
        dens = pf("dens", 5f, 1f..30f), // packing tries per tier, per disc-area of page

        wall = pf("wall", 4f, 0.5f..20f), // film thickness mid-edge between the biggest bubbles, px
        wvar = pf("wvar", 0.4f, 0f..1f), // per-wall wobble on that
        // plateau borders are long tapered spindles, not round corners - that wants k far
        // bigger than the wall. 50 cuts 8px off the corner and starts swelling 50px out
        knot = pf("knot", 50f, 0f..80f),
        kvar = pf("kvar", 0.5f, 0f..1f),
        core = pf("core", 0.5f, 0f..1f), // how light the film line is in a dry node. 0 is solid ink there, by the shore its white regardless
        cored = pf("cored", 8f, 2f..40f), // how wide the ink has to be before the line shows, px. the films are 4 and stay solid, the fat nodes dont
        film = pf("film", 1.3f, 0.3f..6f), // half-width of the film line, px

        // the drain is small and the wet band wide, so the gradient runs all the way into the
        // corner instead of flattening out over a big disc - a 0.5 disc gave one uniform mesh
        pool = pf("pool", 0.05f, 0f..2f), // radius of the fully wet disc at the drain, page widths. 0 is a dry page
        poolx = pf("poolx", 1.02f, -1f..2f), // its centre, page units. just off the corner
        pooly = pf("pooly", 1.05f, -1f..2f),
        prim = pf("prim", 0.12f, 0f..0.5f), // how much its edge wanders
        wet = pf("wet", 0.85f, 0.05f..1.5f), // how far past the disc the foam still counts as wet, page widths. the whole black-to-white turn happens in here
        sink = pf("sink", 0.05f, 0.01f..1f), // how small the cells get at the drain, as a multiple of their dry size
        flood = pf("flood", 12f, 1f..30f), // how many times fatter the borders are at the drain. 8 still left a white dot in every ring there, under ~5 the cells never go solid

        layers = pi("layers", 4, 1..8),
        blur = pf("blur", 2.2f, 0f..20f), // px of blur per step back. the back nets should still read as lines, just soft ones
        grey = pf("grey", 0.5f, 0f..1f), // ink of the first layer back, 1 is as black as the front
        fade = pf("fade", 0.55f, 0.1f..1f), // and each one behind that keeps this much
        zoom = pf("zoom", 1.1f, 0.5f..2f), // back layers scale up by this per step

        paper = pf("paper", 0.975f, 0.5f..1f),
        haze = pf("haze", 0.04f, 0f..0.5f),
        grain = pf("grain", 0.04f, 0f..0.5f),
        vig = pf("vig", 0.2f, 0f..1.4f),
    )

    require(p.rmin < p.rbig) { "rmin must be under rbig" }
    require(p.small <= p.cap) { "small must be under cap" }

    return p
}
