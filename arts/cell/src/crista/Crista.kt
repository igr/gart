package crista

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.colorScale
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.color.lighten
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.pl
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.noise.SimplexNoise
import java.util.stream.IntStream
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The folded inner membrane
 */
private const val W = 1200
private const val H = 1200
private val PIf = PI.toFloat()

// io
private val SEED = pl("seed", 11)
private val OUT = ps("out", "crista")
private val SS = pi("ss", 2)                        // supersample; shading in GW*GH, sim stays coarse
private val GW = W * SS
private val GH = H * SS
private val NZOFF = (SEED and 0xffff) * 0.01f       // SimplexNoise is seedless -> shift its window by the seed

// reaction-diffusion engine
// // sim grid = logical px / SIMDIV; the labyrinth wavelength rides on it
private val SIMDIV = pi("simdiv", 2)                // sim coarseness; 2 -> ridges ~2x wider on canvas
private val SW = W / SIMDIV
private val SH = H / SIMDIV
private val STEPS = pi("steps", 14000)               // integration steps; the labyrinth needs time to close
private val F = pf("f", 0.033f)                     // feed - lower wanders toward stripes, higher toward worms
private val K = pf("k", 0.058f)                     // kill - with F picks the Pearson regime we buckle in
private val KVAR = pf("kvar", 0.0018f)              // regional kill drift: fat ridges here, fine filigree there
private const val KVAR_S = 0.0009f                   // kill-drift frequency (logical px)
private const val DU = 0.16f                         // substrate diffusion (classic Gray-Scott pairing)
private const val DV = 0.08f                         // activator diffusion
private const val DT = 1.0f
private val COMB = pf("comb", 0.95f)                // anisotropy 0..0.95: how hard the current combs the folds
private val FLOW_S = pf("flow", 0.0012f)            // orientation-current frequency (logical px)
private val NSEED = pi("nseed", 200)                // activator dashes sprinkled at t=0, laid along the current
private const val SEED_LEN = 12                      // dash length (sim px)

// mother folds - a few long streamlines of the current, written into the kill field as
// bands of fat, tall ridges
private val NFOLD = pi("folds", 3)
private val FOLD_K = pf("foldk", 0.0008f)           // kill drop along a mother fold (fatter, rounder worms)
private val FOLD_AMP = pf("foldamp", 0.7f)         // extra relief height riding a mother fold
private const val FOLD_W = 26f                       // fold band half-width (logical px)
private const val FOLD_LEN = 1500f                   // trace length (logical px)
private val AURA = pf("aura", 0.002f)               // kill lift hugging each pore -> fine filigree collars

// pores
private val NPORE = pi("pores", 5)
private val PORE_R0 = pf("pore0", 42f)              // radius range, logical px
private val PORE_R1 = pf("pore1", 130f)
private const val PORE_MARGIN = 0.13f                // keep pore centres this fraction of W off the frame
private const val PORE_SEP = 1.7f                    // min centre distance as a multiple of summed radii
private val PWOB = pf("pwob", 0.05f)                // how far each well strays from a perfect circle
private const val EDGE_BINS = 512                    // wobbly-edge radius samples around the rim
private val RIPPLE = pf("ripple", 0.10f)            // still-water rings on the pore floor (h units)
private const val RIPPLE_L = 34f                     // ring wavelength (logical px)
private val BOWL = pf("bowl", 0.9f)                 // how deeply each well is dished
private val GLOW = pf("glow", 0.75f)                 // luminous core resting in each well
private val GLOWOFF = pf("glowoff", 0.18f)          // core nudge off centre (fraction of r), away from the light

// relief
private val RIDGE = pf("ridge", 8f)                 // slope gain: how tall the folds stand
private val SWELL = pf("swell", 2.6f)               // broad membrane undulation (h units, two octaves)
private const val SWELL_S = 0.0012f                  // swell base frequency (logical px)
private val DIM = pf("dim", 0.45f)                  // how deeply the low membrane sinks into shadow
private val AMPVAR = pf("ampvar", 0.3f)            // regional fold damping - some fields lie calmer

// light, screen coords (y down): +deg => up, 145 = upper-left, kept low to rake the crests
private val LIGHT = pf("light", 145f) * PIf / 180f
private val LZ = pf("lz", 0.46f)
private val AMB = pf("amb", 0.24f)                  // ambient floor under the Lambert term
private val AOMIN = pf("aomin", 0.10f)              // darkest valley shade
private val AOHI = pf("aohi", 0.5f)                 // height where occlusion fully releases
private val SPECK = pf("spec", 0.6f)                // ridgeline sheen strength
private val SPECP = pf("specp", 10f)                // sheen tightness

// colour
private val COOL = pi("cool", 1)
private const val GRAD_STEPS = 512
private val RAMP: Palette = Palettes.coolPalette(COOL).expand(GRAD_STEPS)
private val HGAIN = pf("hgain", 0.66f)              // elevation share of the ramp index (floors cool, crests hot)
private val MGAIN = pf("mgain", 0.26f)              // signed macro drift - broad regions lean warmer or cooler
private val BIAS = pf("bias", 0.10f)
private val NOISE_F = pf("noisef", 0.0016f)         // macro-drift frequency (logical px)
private val GRAIN = pf("grain", 0.02f)
private val VIG = pf("vig", 0.65f)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("crista", W, H)
    println(gart)
    println("seed=$SEED ss=$SS simdiv=$SIMDIV steps=$STEPS f=$F k=$K comb=$COMB pores=$NPORE cool=$COOL")

    val g = gart.gartvas()
    val c = g.canvas

    var t0 = System.currentTimeMillis()
    val sim = Sim()
    sim.run()
    println("sim ${SW}x${SH} x $STEPS in ${System.currentTimeMillis() - t0}ms")

    t0 = System.currentTimeMillis()
    val relief = Relief(sim)
    val map = Gartmap(g.d)
    shade(relief, sim, map)
    map.drawToCanvas(g)
    c.drawVignette(g.d, VIG)
    if (GRAIN > 0f) addGrain(g, GRAIN, SEED.toInt())
    println("render in ${System.currentTimeMillis() - t0}ms")

    gart.saveImage(g, "$OUT.png")
    if (!headless) gart.window().showImage(g)
}

// ENGINE
// anisotropic Gray-Scott on a SW*SH grid. double-buffered, rows in parallel -> deterministic
// for any thread count. zero-flux edges via clamped neighbour indices.

/** a well with a wobbly rim*/
private class Pore(val x: Float, val y: Float, val r: Float) {
    private val lut = FloatArray(EDGE_BINS)
    var rMax = r; private set

    init {
        val ax = x * 0.011f + NZOFF * 7f
        val ay = y * 0.011f
        for (k in 0 until EDGE_BINS) {
            val th = k * 2f * PIf / EDGE_BINS
            val c = cos(th)
            val s = sin(th)
            val n1 = SimplexNoise.noise(ax + c * 1.3f, ay + s * 1.3f)            // 2-3 broad lobes
            val n2 = SimplexNoise.noise(ax + 47f + c * 3.2f, ay + 47f + s * 3.2f) // finer undulation
            val rr = r * (1f + PWOB * (n1 + 0.45f * n2))
            lut[k] = rr
            if (rr > rMax) rMax = rr
        }
    }

    /** rim radius toward (ddx, ddy), linearly interpolated between bins. */
    fun edge(ddx: Float, ddy: Float): Float {
        val u = (atan2(ddy, ddx) / (2f * PIf) + 1f) * EDGE_BINS
        val k = u.toInt()
        val f = u - k
        return lerp(lut[k % EDGE_BINS], lut[(k + 1) % EDGE_BINS], f)
    }
}

private class Sim {
    val u = FloatArray(SW * SH) { 1f }
    val v = FloatArray(SW * SH)
    private val u2 = FloatArray(SW * SH)
    private val v2 = FloatArray(SW * SH)

    // per-cell diffusion tensor, unit trace: fast along the current, slow across it
    private val axx = FloatArray(SW * SH)
    private val ayy = FloatArray(SW * SH)
    private val axy = FloatArray(SW * SH)
    private val kk = FloatArray(SW * SH)               // drifting kill rate - the regional fat/fine gradient
    private val fold = FloatArray(SW * SH)             // mother-fold mask, 0..1
    val amp = FloatArray(SW * SH)                      // relief amplitude: calm fields, tall folds
    private val still = BooleanArray(SW * SH)          // pore cells pinned to substrate
    private val xm = IntArray(SW) { max(it - 1, 0) }
    private val xp = IntArray(SW) { min(it + 1, SW - 1) }

    val pores = ArrayList<Pore>()

    init {
        comb()
        val rng = Random(SEED)
        traceFolds(rng)
        digPores(rng)
        settleKill()
        sprinkle(rng)
    }

    /** orientation curr */
    private fun comb() {
        for (y in 0 until SH) {
            for (x in 0 until SW) {
                val lx = x * SIMDIV.toFloat()
                val ly = y * SIMDIV.toFloat()
                val theta = SimplexNoise.noise(lx * FLOW_S + NZOFF, ly * FLOW_S) * PIf
                val dx = cos(theta)
                val dy = sin(theta)
                val i = y * SW + x
                axx[i] = 1f + COMB * (dx * dx - 0.5f)
                ayy[i] = 1f + COMB * (dy * dy - 0.5f)
                axy[i] = COMB * dx * dy
                kk[i] = K + KVAR * SimplexNoise.noise(lx * KVAR_S + NZOFF + 53f, ly * KVAR_S)
            }
        }
    }

    /** walk a few streamlines of the current, both ways, and stamp */
    private fun traceFolds(rng: Random) {
        repeat(NFOLD) {
            val x0 = rng.nextDouble(0.2 * W, 0.8 * W).toFloat()
            val y0 = rng.nextDouble(0.2 * H, 0.8 * H).toFloat()
            for (dir in intArrayOf(1, -1)) {
                var x = x0
                var y = y0
                var walked = 0f
                while (walked < FOLD_LEN / 2 && x in 0f..W.toFloat() && y in 0f..H.toFloat()) {
                    val theta = SimplexNoise.noise(x * FLOW_S + NZOFF, y * FLOW_S) * PIf
                    stampFold(x, y)
                    x += cos(theta) * 4f * dir
                    y += sin(theta) * 4f * dir
                    walked += 4f
                }
            }
        }
    }

    private fun stampFold(px: Float, py: Float) {
        val r = FOLD_W * 2f / SIMDIV                  // stamp to ~2 sigma
        val cx = px / SIMDIV
        val cy = py / SIMDIV
        val sig2 = (FOLD_W / SIMDIV) * (FOLD_W / SIMDIV)
        val x0 = max(0, (cx - r).toInt())
        val x1 = min(SW - 1, (cx + r).toInt() + 1)
        val y0 = max(0, (cy - r).toInt())
        val y1 = min(SH - 1, (cy + r).toInt() + 1)
        for (yy in y0..y1) for (xx in x0..x1) {
            val ddx = xx - cx
            val ddy = yy - cy
            val g = kotlin.math.exp(-(ddx * ddx + ddy * ddy) / sig2)
            val i = yy * SW + xx
            if (g > fold[i]) fold[i] = g
        }
    }

    private fun settleKill() {
        for (y in 0 until SH) {
            for (x in 0 until SW) {
                val i = y * SW + x
                val lx = x * SIMDIV.toFloat()
                val ly = y * SIMDIV.toFloat()
                var aura = 0f
                for (p in pores) {
                    val d = hypot(lx - p.x, ly - p.y)
                    if (d > p.rMax * 2.4f) continue
                    val re = p.edge(lx - p.x, ly - p.y)
                    val a = AURA * smoothstep(re * 2.4f, re * 1.05f, d)
                    if (a > aura) aura = a
                }
                kk[i] = (kk[i] - FOLD_K * fold[i] + aura).coerceIn(0.050f, 0.0625f)
                val calmNz = 0.5f + 0.5f * SimplexNoise.noise(lx * NOISE_F * 0.7f + NZOFF + 37f, ly * NOISE_F * 0.7f)
                amp[i] = (1f - AMPVAR * calmNz) * (1f + FOLD_AMP * fold[i])
            }
        }
    }

    private fun digPores(rng: Random) {
        val margin = PORE_MARGIN * W
        var tries = 0
        while (pores.size < NPORE && tries < 600) {
            tries++
            val r = if (pores.isEmpty()) PORE_R1
            else PORE_R0 + (PORE_R1 - PORE_R0) * rng.nextDouble().pow(2.2).toFloat() * 0.7f
            val x = rng.nextDouble(margin.toDouble(), (W - margin).toDouble()).toFloat()
            val y = rng.nextDouble(margin.toDouble(), (H - margin).toDouble()).toFloat()
            if (pores.any { hypot(it.x - x, it.y - y) < (it.r + r) * PORE_SEP }) continue
            pores.add(Pore(x, y, r))
        }
        for (p in pores) {
            val cx = p.x / SIMDIV
            val cy = p.y / SIMDIV
            val cr = p.rMax / SIMDIV
            val x0 = max(0, (cx - cr).toInt())
            val x1 = min(SW - 1, (cx + cr).toInt() + 1)
            val y0 = max(0, (cy - cr).toInt())
            val y1 = min(SH - 1, (cy + cr).toInt() + 1)
            for (yy in y0..y1) for (xx in x0..x1) {
                val ddx = xx - cx
                val ddy = yy - cy
                val re = p.edge(ddx, ddy) / SIMDIV
                if (ddx * ddx + ddy * ddy <= re * re) still[yy * SW + xx] = true
            }
        }
    }

    /** activator dashes laid along  */
    private fun sprinkle(rng: Random) {
        repeat(NSEED) {
            val cx = rng.nextInt(SEED_LEN, SW - SEED_LEN)
            val cy = rng.nextInt(SEED_LEN, SH - SEED_LEN)
            val theta = SimplexNoise.noise(cx * SIMDIV * FLOW_S + NZOFF, cy * SIMDIV * FLOW_S) * PIf
            val dx = cos(theta)
            val dy = sin(theta)
            val len = SEED_LEN / 2 + rng.nextInt(SEED_LEN / 2)
            val uu = 0.50f + rng.nextFloat() * 0.05f
            val vv = 0.25f + rng.nextFloat() * 0.05f
            var s = -len
            while (s <= len) {
                val px = (cx + dx * s).toInt().coerceIn(1, SW - 2)
                val py = (cy + dy * s).toInt().coerceIn(1, SH - 2)
                for (oy in -1..1) for (ox in -1..1) {
                    val i = (py + oy) * SW + px + ox
                    if (still[i]) continue
                    u[i] = uu
                    v[i] = vv
                }
                s++
            }
        }
    }

    fun run() {
        var src = 0
        val t0 = System.currentTimeMillis()
        repeat(STEPS) { step ->
            val us = if (src == 0) u else u2
            val vs = if (src == 0) v else v2
            val ud = if (src == 0) u2 else u
            val vd = if (src == 0) v2 else v
            IntStream.range(0, SH).parallel().forEach { y -> stepRow(y, us, vs, ud, vd) }
            src = 1 - src
            if ((step + 1) % 1000 == 0) println("  rd ${step + 1}/$STEPS  ${System.currentTimeMillis() - t0}ms")
        }
        if (src == 1) {     // results live in u2/v2 - fold them home
            System.arraycopy(u2, 0, u, 0, u.size)
            System.arraycopy(v2, 0, v, 0, v.size)
        }
    }

    private fun stepRow(y: Int, us: FloatArray, vs: FloatArray, ud: FloatArray, vd: FloatArray) {
        val row = y * SW
        val rowN = max(y - 1, 0) * SW
        val rowS = min(y + 1, SH - 1) * SW
        for (x in 0 until SW) {
            val i = row + x
            val xl = xm[x]
            val xr = xp[x]
            val uc = us[i]
            val vc = vs[i]
            // anisotropic laplacian: axx*Fxx + ayy*Fyy + 2*axy*Fxy, cross term from the diagonals
            val lu = axx[i] * (us[row + xl] + us[row + xr] - 2f * uc) + ayy[i] * (us[rowN + x] + us[rowS + x] - 2f * uc) + axy[i] * 0.5f * (us[rowN + xr] + us[rowS + xl] - us[rowN + xl] - us[rowS + xr])
            val lv = axx[i] * (vs[row + xl] + vs[row + xr] - 2f * vc) + ayy[i] * (vs[rowN + x] + vs[rowS + x] - 2f * vc) + axy[i] * 0.5f * (vs[rowN + xr] + vs[rowS + xl] - vs[rowN + xl] - vs[rowS + xr])
            val uvv = uc * vc * vc
            var un = uc + DT * (DU * lu - uvv + F * (1f - uc))
            var vn = vc + DT * (DV * lv + uvv - (F + kk[i]) * vc)
            if (still[i]) {
                un = 1f
                vn = 0f
            }
            ud[i] = un
            vd[i] = vn
        }
    }
}

// RELIEF

private class Relief(sim: Sim) {
    val h = FloatArray(SW * SH)

    init {
        for (i in h.indices) h[i] = 1f - sim.u[i]
        blur()
        blur()
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        for (x in h) {
            if (x < lo) lo = x
            if (x > hi) hi = x
        }
        val inv = if (hi > lo) 1f / (hi - lo) else 1f
        for (i in h.indices) h[i] = (h[i] - lo) * inv
    }

    /** radius-1 box blur, separable, clamped edges. */
    private fun blur() {
        val t = FloatArray(SW * SH)
        for (y in 0 until SH) {
            val row = y * SW
            for (x in 0 until SW) {
                t[row + x] = (h[row + max(x - 1, 0)] + h[row + x] + h[row + min(x + 1, SW - 1)]) / 3f
            }
        }
        for (y in 0 until SH) {
            val rowN = max(y - 1, 0) * SW
            val row = y * SW
            val rowS = min(y + 1, SH - 1) * SW
            for (x in 0 until SW) {
                h[row + x] = (t[rowN + x] + t[row + x] + t[rowS + x]) / 3f
            }
        }
    }

    fun at(sx: Float, sy: Float) = bilin(h, sx, sy)
}

/** bilinear sample of a SW*SH field at sim-grid coords. */
private fun bilin(f: FloatArray, sx: Float, sy: Float): Float {
    val x = sx.coerceIn(0f, SW - 1.001f)
    val y = sy.coerceIn(0f, SH - 1.001f)
    val x0 = x.toInt()
    val y0 = y.toInt()
    val fx = x - x0
    val fy = y - y0
    val i = y0 * SW + x0
    val a = f[i] + (f[i + 1] - f[i]) * fx
    val b = f[i + SW] + (f[i + SW + 1] - f[i + SW]) * fx
    return a + (b - a) * fy
}

// RENDER

private fun shade(relief: Relief, sim: Sim, map: Gartmap) {
    val pores = sim.pores
    val rgb = IntArray(GW * GH)
    val llen = sqrt(1f + LZ * LZ)
    val lx = cos(LIGHT) / llen
    val ly = -sin(LIGHT) / llen
    val lz = LZ / llen
    val hvz = lz + 1f                                   // half vector of light and the straight-down view
    val hlen = sqrt(lx * lx + ly * ly + hvz * hvz)
    val hx = lx / hlen
    val hy = ly / hlen
    val hz = hvz / hlen
    val poreArr = pores.toTypedArray()

    // each core drifts off centre, roughly away from the light (where a dished well pools it),
    // with a per-pore twist so the wells don't look stamped
    val coreX = FloatArray(poreArr.size)
    val coreY = FloatArray(poreArr.size)
    for (pi in poreArr.indices) {
        val p = poreArr[pi]
        val twist = (hash01(pi, 3, SEED.toInt()) - 0.5f) * 0.9f
        val off = GLOWOFF * p.r * (0.85f + 0.3f * hash01(pi, 5, SEED.toInt()))
        coreX[pi] = p.x - cos(LIGHT + twist) * off
        coreY[pi] = p.y + sin(LIGHT + twist) * off
    }

    val ringK = 2f * PIf / RIPPLE_L
    IntStream.range(0, GH).parallel().forEach { py ->
        val yL = py.toFloat() / SS                       // logical coords
        val sy = yL / SIMDIV                             // sim coords
        var i = py * GW
        for (px in 0 until GW) {
            val xL = px.toFloat() / SS
            val sx = xL / SIMDIV

            // regional amplitude: calm fields where the light can rest, tall mother folds
            val calm = bilin(sim.amp, sx, sy)
            var hc = relief.at(sx, sy) * calm

            // fold slope (per sim px)
            var dhx = (relief.at(sx + 1f, sy) - relief.at(sx - 1f, sy)) * 0.5f * calm
            var dhy = (relief.at(sx, sy + 1f) - relief.at(sx, sy - 1f)) * 0.5f * calm
            var glow = 0f

            // pore wells: dished floor, still-water rings, the folds fade out at the wobbly rim
            for (pi in poreArr.indices) {
                val p = poreArr[pi]
                val ddx = xL - p.x
                val ddy = yL - p.y
                val d2 = ddx * ddx + ddy * ddy
                if (d2 >= p.rMax * p.rMax) continue
                val d = sqrt(d2)
                val re = p.edge(ddx, ddy)
                if (d < re) {
                    val q = d / re
                    val dN = lerp(d, d / re * p.r, q)                          // circular rings mid-pool, echoing the rim near the wall
                    val inPore = smoothstep(re, re * 0.9f, d)
                    val damp = (1f - q).pow(1.5f)
                    val wob = 1.7f * SimplexNoise.noise(xL * 0.02f + NZOFF + 5f, yL * 0.02f)
                    val ring = RIPPLE * damp * sin(dN * ringK + wob)
                    val radial = RIPPLE * damp * cos(dN * ringK + wob) * ringK * lerp(1f, p.r / re, q) + // ring slope, per logical px
                        2f * BOWL * d / (re * re)                              // bowl wall rising to the rim
                    val invD = if (d > 1e-3f) 1f / d else 0f
                    dhx = lerp(dhx, radial * ddx * invD * SIMDIV, inPore)
                    dhy = lerp(dhy, radial * ddy * invD * SIMDIV, inPore)
                    hc = lerp(hc, (0.05f + ring + BOWL * 0.1f * (d / re) * (d / re)).coerceAtLeast(0f), inPore)
                    val dg = hypot(xL - coreX[pi], yL - coreY[pi])
                    glow = GLOW * smoothstep(p.r * 0.38f, p.r * 0.02f, dg).pow(1.4f) * inPore
                    break
                }
            }

            // broad membrane swell under everything, two octaves; its height is the wide light
            val swellH = 0.5f + 0.5f * (SimplexNoise.noise(xL * SWELL_S + NZOFF + 11f, yL * SWELL_S) + 0.5f * SimplexNoise.noise(xL * SWELL_S * 3.1f + NZOFF + 71f, yL * SWELL_S * 3.1f)) / 1.5f
            dhx += ((SimplexNoise.noise((xL + 8f) * SWELL_S + NZOFF + 11f, yL * SWELL_S) - SimplexNoise.noise((xL - 8f) * SWELL_S + NZOFF + 11f, yL * SWELL_S)) + 0.5f * (SimplexNoise.noise((xL + 8f) * SWELL_S * 3.1f + NZOFF + 71f, yL * SWELL_S * 3.1f) - SimplexNoise.noise((xL - 8f) * SWELL_S * 3.1f + NZOFF + 71f, yL * SWELL_S * 3.1f))) * SWELL * SIMDIV * 0.0625f
            dhy += ((SimplexNoise.noise(xL * SWELL_S + NZOFF + 11f, (yL + 8f) * SWELL_S) - SimplexNoise.noise(xL * SWELL_S + NZOFF + 11f, (yL - 8f) * SWELL_S)) + 0.5f * (SimplexNoise.noise(xL * SWELL_S * 3.1f + NZOFF + 71f, (yL + 8f) * SWELL_S * 3.1f) - SimplexNoise.noise(xL * SWELL_S * 3.1f + NZOFF + 71f, (yL - 8f) * SWELL_S * 3.1f))) * SWELL * SIMDIV * 0.0625f

            var nx = -dhx * RIDGE
            var ny = -dhy * RIDGE
            var nz = 1f
            val nl = sqrt(nx * nx + ny * ny + nz * nz)
            nx /= nl
            ny /= nl
            nz /= nl

            val lambert = (nx * lx + ny * ly + nz * lz).coerceIn(0f, 1f)
            val ao = smoothstep(0.02f, AOHI, hc)
            val shade = lerp(AOMIN, 1f, ao) * (AMB + (1f - AMB) * lambert) * lerp(1f - DIM, 1f, swellH)

            // elevation + signed macro drift pick the tone: teal floors, ember crests
            val macro = SimplexNoise.noise(xL * NOISE_F + NZOFF, yL * NOISE_F)
            val t = (BIAS + HGAIN * min(hc, 1f).pow(1.2f) + MGAIN * macro).coerceIn(0f, 1f)
            var col = colorScale(RAMP.bound((t * (GRAD_STEPS - 1)).toInt()), shade)

            // sheen riding the ridgelines
            val s = (nx * hx + ny * hy + nz * hz).coerceAtLeast(0f).pow(SPECP) * SPECK * smoothstep(0.45f, 0.9f, hc)
            if (s > 0.003f) col = lerpColor(col, 0xFFFFF1D6.toInt(), s.coerceIn(0f, 1f))

            // a luminous core resting in the still water
            if (glow > 0.003f) col = lerpColor(col, lighten(RAMP.bound((0.58f * GRAD_STEPS).toInt()), 0.45f), glow.coerceIn(0f, 1f))

            rgb[i] = col
            i++
        }
    }
    downsample(rgb, map)
}

/** box-average the SS grid down to W*H, writing opaque pixels. */
private fun downsample(rgb: IntArray, map: Gartmap) {
    val px = map.pixels
    val inv = 1f / (SS * SS)
    for (y in 0 until H) {
        val by = y * SS
        for (x in 0 until W) {
            val bx = x * SS
            var r = 0
            var gg = 0
            var b = 0
            var yy = 0
            while (yy < SS) {
                var row = (by + yy) * GW + bx
                var xx = 0
                while (xx < SS) {
                    val col = rgb[row]
                    r += (col ushr 16) and 0xFF
                    gg += (col ushr 8) and 0xFF
                    b += col and 0xFF
                    row++
                    xx++
                }
                yy++
            }
            px[y * W + x] = (0xFF shl 24) or ((r * inv).toInt() shl 16) or ((gg * inv).toInt() shl 8) or (b * inv).toInt()
        }
    }
}
