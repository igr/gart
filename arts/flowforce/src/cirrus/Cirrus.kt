package cirrus

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.pl
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.f
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.noise.curl
import dev.oblac.gart.noise.fbm
import dev.oblac.gart.util.parallelBands
import dev.oblac.gart.vector.MutableVec2
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln1p
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * cirrus - braided ribbons wound into a gyre.
 *
 * the brush is flamebrush's: a little spiral of dots that turns a notch every time you move it,
 * so dragging it leaves a ribbon with a dense spine and a scalloped edge. flamebrush walked it
 * round a circle, I drag it through a field of vortices instead - one big one carrying the
 * composition, a handful of small ones, half of them spinning the other way. that last bit
 * isn't decoration. with a single gyre every path is a streamline of the same field, so nothing
 * ever crosses anything and the page comes out looking like a twirl filter.
 *
 * the other half is where the dots go. not on the canvas - into a float buffer, one bin per
 * subpixel, tone mapped at the end by log density. thats the fractal flame trick and its what
 * makes the whole thing work: overlap stops meaning more paint and starts meaning more light,
 * so the core burns white without me laying down a single white dot.
 */
private const val W = 1200
private const val H = 1600

// palette lookup. 512 is way past what the eye picks out of a gradient
private const val LUTN = 512

// finite-difference step for the curl, in noise-domain units
private const val EPS = 1.1f

private data class Params(
    val seed: Long,
    val out: String,
    val pal: Int,
    val ss: Int,
    // the gyres
    val gx: Float, val gy: Float,
    val gyres: Int, val pull: Float, val reach: Float, val against: Float,
    val core: Float, val mincore: Float,
    val eat: Float, val sateye: Float, val eatmin: Float, val fade: Float,
    val rise: Float,
    // the drift
    val nscale: Float, val wobble: Float, val octaves: Int,
    // where the ribbons are allowed to start
    val spread: Float, val mscale: Float,
    // the ribbons
    val n: Int, val steps: Int, val stepvar: Float, val pitch: Float,
    val spark: Float,
    // the brush
    val k: Int,
    val starts: Int,
    val loops: Float,
    val twist: Float,
    val width: Float,
    val squash: Float,
    val fine: Float,
    val flow: Float,
    val jitter: Float, val slop: Float, val breathe: Float,
    val cscale: Float, val wash: Float,
    // light
    val exposure: Float, val gamma: Float, val whiten: Float, val ceiling: Float,
    val bloom: Float, val bloomr: Float, val thr: Float,
    // the page
    val ink: Float, val glow: Float, val vig: Float, val grain: Float,
)

private val p = resolveParams()
private val rnd = Random(p.seed)

// simplex carries no seed, so shift the sample window instead
private val nzoff = (p.seed and 0xffff) * 0.01f

// the inks /////////

// these are chroma ramps, not value ramps: nothing here is dark. darkness is the log-density
// map's job, and if the ramp darkens too the outskirts get crushed twice over and vanish.
private val inks = arrayOf(
    Palettes.cool174,
    Palettes.cool175,
    Palettes.cool176,
    Palettes.cool177,
)

private val ink = if (p.pal < inks.size) inks[p.pal] else Palettes.coolPalette(p.pal - inks.size + 1)

// ---- the frame ----

private val gx = W * p.gx
private val gy = H * p.gy

// everything about a gyre is a fraction of this
private val R = hypot(W.f(), H.f()) * 0.5f

/**
 * one vortex. [k] is signed, so a satellite can turn against the main one - thats where the
 * ribbons end up crossing instead of sliding past each other. [reach] is where its pull is down
 * to half, [pull] is how hard it winds a ribbon inward as opposed to just carrying it round,
 * and [eye] is the radius it swallows them at.
 */
private class Gyre(
    val x: Float, val y: Float,
    val k: Float, val pull: Float,
    val reach: Float, val eye: Float,
)

// one big one anchoring the composition, plus satellites to rough up the flow. a single gyre
// is laminar no matter what you do to it: every path is a streamline of the same field, so
// nothing ever crosses anything and the whole page comes out looking like a twirl filter.
//
// the eyes are deliberately lopsided. the main one wants a tight eye, since thats the subject
// and all the good detail is packed against it. the satellites want a generous one - theyre
// small, and anything that gets to wind round inside them lays down a bright little ring that
// reads as a dead pixel rather than as a whirl
private val gyres = buildList {
    add(Gyre(gx, gy, 1f, p.pull, R * p.reach, R * p.reach * p.eat))
    repeat(p.gyres - 1) {
        val reach = R * p.reach * (0.28f + 0.32f * rnd.nextFloat())
        add(
            Gyre(
                x = W * (0.1f + 0.8f * rnd.nextFloat()),
                y = H * (0.1f + 0.8f * rnd.nextFloat()),
                k = (0.35f + 0.6f * rnd.nextFloat()) * (if (rnd.nextFloat() < p.against) -1f else 1f),
                pull = p.pull * (0.2f + 1.3f * rnd.nextFloat()),
                reach = reach,
                eye = max(reach * p.sateye, p.eatmin),
            ),
        )
    }
}

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("cirrus", W, H)

    println(gart)
    println(
        "seed=${p.seed} pal=${p.pal} n=${p.n} gyres=${p.gyres} pull=${p.pull} reach=${p.reach} " +
            "twist=${p.twist} width=${p.width} wash=${p.wash} exposure=${p.exposure}",
    )

    val g = gart.gartvas()
    render(g)

    val output = p.out.ensureExtension("png")
    gart.saveImage(g, output)

    if (!headless) gart.window().showImage(g)
}

// the field =================================

// unit velocity at (x, y). every gyre's tangent plus its inward pull, each weighted by a
// lorentzian so it only really governs its own patch, then knocked about by curl noise. bias is
// the ribbon's own multiplier on the pull - low means it sails straight past the middle instead
// of winding in, and thats what makes some of them long sweeps and others tight coils
private fun vel(x: Float, y: Float, bias: Float, out: MutableVec2) {
    var wx = 0f
    var wy = 0f
    for (g in gyres) {
        val dx = x - g.x
        val dy = y - g.y
        val r2 = dx * dx + dy * dy
        val r = max(sqrt(r2), 1e-3f)
        val w = g.k / (1f + r2 / (g.reach * g.reach))
        // ease the pull off near the middle, but never all the way to nothing. at zero a
        // caught ribbon settles into a perfectly stable circular orbit and traces it for the
        // rest of its life - comes out as a hard white ring, like a polo mint dropped on the
        // page. the residual keeps it creeping inward so it lays a spiral and then gets eaten
        val ease = p.mincore + (1f - p.mincore) * smoothstep(0f, g.reach * p.core, r)
        val soft = ease * g.pull * bias
        wx += w * (-dy / r) - abs(w) * (dx / r) * soft
        wy += w * (dx / r) - abs(w) * (dy / r) * soft
    }

    // curl noise, so no two ribbons land on the same curve. EPS is big on purpose - at this
    // nscale it's a ~500px wide difference, which is a smoothed curl rather than a derivative,
    // and the smoothing is what keeps the eddies at the scale of the whole gyre
    curl(x * p.nscale + nzoff, y * p.nscale + nzoff, out, EPS, nz)
    wx += out.x * p.wobble
    wy += out.y * p.wobble

    // and a touch of lift, else its a flat disc
    wy -= p.rise

    val m = max(hypot(wx, wy), 1e-6f)
    out.set(wx / m, wy / m)
}

// where a ribbon is allowed to start: a soft bell round the main gyre times a lump of noise.
// the noise is the half that matters, it opens up holes and clearings so the page keeps
// somewhere dark to breathe. seeding uniformly just fills the frame edge to edge
private fun mask(x: Float, y: Float): Float {
    val rn = hypot(x - gx, y - gy) / (R * p.spread)
    val fall = exp(-rn * rn * 1.15f)
    val n = fbm(
        x * p.mscale + 91f + nzoff, y * p.mscale + 13f,
        octaves = 3, lacunarity = 2.2f, gain = 0.5f,
    )
    return fall * (0.16f + 1.5f * (n + 0.42f).coerceIn(0f, 1f))
}

// the potential the curl is taken of. a val rather than a fun so the reference gets made once -
// as `::nz` it'd be a fresh object on every one of the third of a million calls
private val nz: (Float, Float) -> Float =
    { a, b -> fbm(a, b, octaves = p.octaves, lacunarity = 2.07f, gain = 0.55f) }

// ---- the brush, which is the whole point ----

// every dot the brush lays down. flat arrays, because there are four million of them and each
// splat thread walks the lot. c is an index into the colour LUT
private class Dots(cap: Int) {
    val x = FloatArray(cap)
    val y = FloatArray(cap)
    val r = FloatArray(cap)
    val w = FloatArray(cap)
    val c = IntArray(cap)
    var n = 0

    fun add(px: Float, py: Float, pr: Float, pw: Float, pc: Int) {
        if (n >= x.size) return // cap is an upper bound so this never fires. belt and braces
        x[n] = px; y[n] = py; r[n] = pr; w[n] = pw; c[n] = pc; n++
    }
}

// interleaved spiral starts, and how many dots each one gets
private val starts = p.starts.coerceIn(1, p.k)
private val kk = max(1, p.k / starts)
private val lead = TAUf / starts

// walks every ribbon and stamps the brush along it. single threaded on purpose - all the rng
// lives in here and the order of the draws has to stay put or the seed stops meaning anything
private fun sow(ss: Int): Dots {
    val cap = p.n * ((p.steps * (1f + p.stepvar * 0.45f)).toInt() + 2) * p.k
    val dots = Dots(cap)
    val v = MutableVec2()

    for (i in 0 until p.n) {
        // sparks are the same ribbon with the handbrake off - barely any pull, so they sail
        // straight past the middle instead of coiling. thin, hot, and they break the swirl up
        val isSpark = rnd.nextFloat() < p.spark

        // rejection sample against the mask. 60 tries then take whats given, though with a
        // mean mask around 0.3 thats a miss once in 10^31 - it has never actually fired
        var x = 0f
        var y = 0f
        for (t in 0 until 60) {
            x = W * (-0.12f + 1.24f * rnd.nextFloat())
            y = H * (-0.12f + 1.24f * rnd.nextFloat())
            if (rnd.nextFloat() < mask(x, y)) break
        }

        val bias = (if (isSpark) 0.22f else 1f) * (0.55f + 0.9f * rnd.nextFloat())
        val ns = (p.steps * lerp(1f - p.stepvar, 1f + p.stepvar * 0.45f, rnd.nextFloat())).toInt()
        // handedness of the braid, random per ribbon. all-same looks combed
        val twist = p.twist * (0.15f + 2.4f * rnd.nextFloat().pow(2.2f)) * (if (rnd.nextBoolean()) 1f else -1f)
        // and the pitch breathes along the ribbon. a constant twist makes a perfect helix, and
        // at 1:1 a perfect helix looks like a telephone cord - the one thing here that reads
        // as machinery. the period is per ribbon so no two of them beat together
        val breath = 0.018f + 0.055f * rnd.nextFloat()
        val phase0 = rnd.nextFloat() * TAUf
        val arm = TAUf * p.loops * (0.7f + 0.75f * rnd.nextFloat()) / kk
        val wid = p.width * (if (isSpark) 0.34f else 1f) * (0.45f + 1.1f * rnd.nextFloat().pow(1.6f))
        val hue = (rnd.nextFloat() - 0.5f) * 0.2f + (if (isSpark) 0.22f else 0f)
        // some ribbons are ghosts and some are the ones you actually see. thats the depth
        val gain = p.flow * (if (isSpark) 1.15f else 1f) * (0.25f + 1.3f * rnd.nextFloat().pow(1.8f))
        val eat = 0.7f + 0.7f * rnd.nextFloat()

        var off = rnd.nextFloat() * TAUf

        for (s in 0 until ns) {
            if (x < -240f || x > W + 240f || y < -240f || y > H + 240f) break

            // a gyre eats whatever falls into its eye, otherwise a caught ribbon orbits the
            // same square inch for another thousand steps and puts all of it down in one place:
            // a blown white bead, reading as a lens flare. it has to be gradual though - cutting
            // the ribbon dead at the eye leaves a hard circular bite out of the picture, which
            // is worse than the bead. so fade it out across the annulus and stop when nothings left.
            // eat is jittered per ribbon, so they go out at different depths and the middle
            // comes out as an iris rather than a punched hole
            var clear = 1f
            for (g in gyres) {
                val q = smoothstep(1f, p.fade, hypot(x - g.x, y - g.y) / (g.eye * eat))
                if (q < clear) clear = q
            }
            if (clear <= 0f) break

            // rk2. euler visibly cuts the corner on the tight coils
            val h = p.pitch
            vel(x, y, bias, v)
            val mx = x + v.x * h * 0.5f
            val my = y + v.y * h * 0.5f
            vel(mx, my, bias, v)
            val dx = v.x
            val dy = v.y
            x += dx * h
            y += dy * h

            val u = s / (ns - 1f)
            // fat in the belly, nothing at either end. 0.55 rather than 0.5 keeps a bit more
            // meat in the middle - a plain sine tapers so early the strokes look like grains.
            // the max() is not paranoia. sin(PIf) comes back a hair NEGATIVE, a fractional pow
            // of that is NaN, and the bloom blur then smears the NaN over the entire page - I
            // had a black canvas for an hour hunting this. dont drop it
            val taper = max(0f, sin(PIf * u)).pow(0.55f)
            val brush = wid * taper
            if (brush < 0.08f) continue

            val base = paletteAt(x, y, hue)
            val wgt = gain * taper * clear
            val jit = p.jitter * brush

            // the spiral itself. rho grows out from the spine while the angle wraps `loops`
            // times - straight out of SpiralBrush, just placed in the stroke's own frame and
            // run as `starts` interleaved copies. one spiral puts exactly one dot at the
            // outside, and as the rosette turns that dot draws a single clean helix down the
            // ribbon: at 1:1 it looks like a telephone cord. three of them 120 apart share the
            // outer edge between them and it goes back to reading as a braid
            val px = -dy
            val py = dx
            for (j in 0 until p.k) {
                val st = j / starts
                val f = (st + 1) / kk.toFloat()
                val a = off + st * arm + (j % starts) * lead
                val rho = brush * f
                val ca = cos(a)
                val sa = sin(a) * p.squash
                val ox = px * rho * ca + dx * rho * sa
                val oy = py * rho * ca + dy * rho * sa
                // dots swell toward the outside of the rosette: crisp spine, feathered edge.
                // the floor is against the step pitch - a dot smaller than one step leaves a
                // gap before the next one lands, and a whole ribbon of that reads as a zipper
                val dr = max(p.fine * (0.55f + 1.35f * f) * (0.5f + 0.5f * taper), p.pitch * 0.8f)
                // energy per dot is fixed, so a fat dot is a faint haze and a small one is hot.
                // the cube fades the outermost barbs out rather than ending the ribbon on a line
                val e = wgt * (1f - 0.7f * f * f) / (dr * dr + 1f)
                // barbs run a shade cooler than the spine. tiny, but its what makes the
                // ribbons read as seperate things where they cross
                val ci = (base - (26f * f).toInt()).coerceIn(0, LUTN - 1)
                // everything above is in page units; the bins are ss times bigger
                dots.add(
                    (x + ox + (rnd.nextFloat() - 0.5f) * jit) * ss,
                    (y + oy + (rnd.nextFloat() - 0.5f) * jit) * ss,
                    dr * ss, e, ci,
                )
            }
            // a wobble on the phase as well as the position. without both, consecutive
            // rosettes land on top of each other and the ribbon comes out as a set of very
            // tidy dotted threads - reads like a halftone screen, not like a brush
            off += twist * (1f + p.breathe * sin(s * breath + phase0)) +
                (rnd.nextFloat() - 0.5f) * p.slop
        }
    }
    return dots
}

// cool at the rim, hot in the middle - but a good half of the ramp position comes off a slow
// noise field instead, so a ribbon shifts colour as it travels and two of them crossing at the
// same radius arent the same colour. straight radial is a gradient map and it looks like one
private fun paletteAt(x: Float, y: Float, jitter: Float): Int {
    val rn = (hypot(x - gx, y - gy) / (R * p.spread)).coerceIn(0f, 1f)
    val n = fbm(
        x * p.cscale + 311f + nzoff, y * p.cscale - 47f,
        octaves = 2, lacunarity = 2.3f, gain = 0.5f,
    )
    val t = lerp((1f - rn).pow(1.25f), (n + 0.5f).coerceIn(0f, 1f), p.wash) + jitter
    return (t.coerceIn(0f, 1f) * (LUTN - 1)).toInt()
}

// light ///////////////////////////

// ARGB stops -> linear rgb triples. everything downstream adds light, so it has to be linear
private fun buildLut(): FloatArray {
    val lut = FloatArray(LUTN * 3)
    for (i in 0 until LUTN) {
        val c = ink.sample(i / (LUTN - 1f))
        lut[i * 3] = lin(((c shr 16) and 0xFF) / 255f)
        lut[i * 3 + 1] = lin(((c shr 8) and 0xFF) / 255f)
        lut[i * 3 + 2] = lin((c and 0xFF) / 255f)
    }
    return lut
}

private fun lin(v: Float) = v.pow(2.2f)
private fun enc(v: Float) = v.coerceIn(0f, 1f).pow(1f / 2.2f)

// density and summed colour, one bin per supersampled pixel
private class Bins(val w: Int, val h: Int) {
    val den = FloatArray(w * h)
    val acc = FloatArray(w * h * 3)
}

// splats dots into rows [y0, y1) and nowhere else. every output row belongs to exactly one
// thread and every thread walks the dots in the same order, so the float sums land the same way
// however many cores turn up. thats what keeps it seeded - render twice, shasum, they match
private fun splat(dots: Dots, lut: FloatArray, b: Bins, y0: Int, y1: Int) {
    for (i in 0 until dots.n) {
        val cx = dots.x[i]
        val cy = dots.y[i]
        val r = dots.r[i]
        val top = max(ceil(cy - r).toInt(), y0)
        val bot = min(floor(cy + r).toInt(), y1 - 1)
        if (top > bot) continue
        val left = max(ceil(cx - r).toInt(), 0)
        val right = min(floor(cx + r).toInt(), b.w - 1)
        if (left > right) continue

        val wgt = dots.w[i]
        val ci = dots.c[i] * 3
        val lr = lut[ci]
        val lg = lut[ci + 1]
        val lb = lut[ci + 2]
        val inv = 1f / (r * r)

        for (py in top..bot) {
            val dy = py - cy
            val dy2 = dy * dy
            val row = py * b.w
            for (px in left..right) {
                val dx = px - cx
                val q = 1f - (dx * dx + dy2) * inv
                if (q <= 0f) continue
                val a = q * q * wgt // squared, so it meets zero flat at the edge
                val o = row + px
                b.den[o] += a
                val o3 = o * 3
                b.acc[o3] += a * lr
                b.acc[o3 + 1] += a * lg
                b.acc[o3 + 2] += a * lb
            }
        }
    }
}

// white point off a histogram of log density rather than off the max. the max is one pixel
// somewhere in the core, and letting that set the scale drags the whole picture dark
private fun whitePoint(b: Bins): Float {
    var hi = 0f
    var nz = 0L
    for (d in b.den) {
        if (d <= 0f) continue
        nz++
        val l = ln1p(d * p.exposure)
        if (l > hi) hi = l
    }
    if (nz == 0L || hi <= 0f) return 1f

    val nb = 4096
    val hist = IntArray(nb)
    val sc = (nb - 1) / hi
    for (d in b.den) {
        if (d <= 0f) continue
        hist[(ln1p(d * p.exposure) * sc).toInt().coerceIn(0, nb - 1)]++
    }
    val want = (nz * p.ceiling).toLong()
    var run = 0L
    for (i in 0 until nb) {
        run += hist[i]
        if (run >= want) return max((i + 0.5f) / sc, 1e-4f)
    }
    return hi
}

// tone map and box-downsample in one pass. log density is the fractal flame curve: a bin's
// first few hits buy a lot of brightness and its thousandth buys almost none, so a stroke fades
// off smoothly instead of piling up against a hard alpha ceiling
private fun develop(b: Bins, ss: Int): FloatArray {
    val denom = whitePoint(b)
    val invG = 1f / p.gamma
    val norm = 1f / (ss * ss)
    val out = FloatArray(W * H * 3)

    for (oy in 0 until H) {
        for (ox in 0 until W) {
            var sr = 0f
            var sg = 0f
            var sb = 0f
            for (jy in 0 until ss) {
                val row = (oy * ss + jy) * b.w + ox * ss
                for (jx in 0 until ss) {
                    val o = row + jx
                    val d = b.den[o]
                    if (d <= 0f) continue
                    val id = 1f / d
                    val o3 = o * 3
                    var cr = b.acc[o3] * id
                    var cg = b.acc[o3 + 1] * id
                    var cb = b.acc[o3 + 2] * id

                    val raw = ln1p(d * p.exposure) / denom
                    val v = min(raw, 1f).pow(invG)
                    // past the white point theres nowhere left to go but white, which is
                    // exactly how a hot core ought to behave
                    val wt = (p.whiten * smoothstep(0.55f, 1.05f, raw)).coerceIn(0f, 1f)
                    cr = lerp(cr, 1f, wt) * v
                    cg = lerp(cg, 1f, wt) * v
                    cb = lerp(cb, 1f, wt) * v

                    sr += cr; sg += cg; sb += cb
                }
            }
            val o3 = (oy * W + ox) * 3
            out[o3] = sr * norm
            out[o3 + 1] = sg * norm
            out[o3 + 2] = sb * norm
        }
    }
    return out
}

// bloom

// three box passes is near enough a gaussian, and the running sum makes each one O(n) in r
private fun blur(src: FloatArray, tmp: FloatArray, r: Int) {
    repeat(3) { boxH(src, tmp, r); boxV(tmp, src, r) }
}

private fun boxH(src: FloatArray, dst: FloatArray, r: Int) {
    val d = 2 * r + 1
    for (y in 0 until H) {
        val row = y * W
        for (ch in 0 until 3) {
            var sum = 0f
            for (i in -r..r) sum += src[(row + i.coerceIn(0, W - 1)) * 3 + ch]
            for (x in 0 until W) {
                dst[(row + x) * 3 + ch] = sum / d
                sum += src[(row + (x + r + 1).coerceIn(0, W - 1)) * 3 + ch]
                sum -= src[(row + (x - r).coerceIn(0, W - 1)) * 3 + ch]
            }
        }
    }
}

private fun boxV(src: FloatArray, dst: FloatArray, r: Int) {
    val d = 2 * r + 1
    for (x in 0 until W) {
        for (ch in 0 until 3) {
            var sum = 0f
            for (i in -r..r) sum += src[(i.coerceIn(0, H - 1) * W + x) * 3 + ch]
            for (y in 0 until H) {
                dst[(y * W + x) * 3 + ch] = sum / d
                sum += src[((y + r + 1).coerceIn(0, H - 1) * W + x) * 3 + ch]
                sum -= src[((y - r).coerceIn(0, H - 1) * W + x) * 3 + ch]
            }
        }
    }
}

// two scales - a tight one for the sheen on the ribbons, a wide one for the air around them
private fun bloom(img: FloatArray) {
    val bright = FloatArray(img.size)
    for (i in 0 until W * H) {
        val o = i * 3
        val l = 0.2126f * img[o] + 0.7152f * img[o + 1] + 0.0722f * img[o + 2]
        val k = smoothstep(p.thr, p.thr + 0.25f, l)
        bright[o] = img[o] * k
        bright[o + 1] = img[o + 1] * k
        bright[o + 2] = img[o + 2] * k
    }
    val wide = bright.copyOf()
    val tmp = FloatArray(img.size)

    blur(bright, tmp, max(1, (p.bloomr * min(W, H)).toInt()))
    blur(wide, tmp, max(2, (p.bloomr * 4.2f * min(W, H)).toInt()))

    for (i in img.indices) {
        img[i] += bright[i] * p.bloom + wide[i] * p.bloom * 0.55f
    }
}

// the page ======

// the ground the light sits on. cold ink, warmed a little where the gyre is
private fun page(img: FloatArray): IntArray {
    val out = IntArray(W * H)
    val glowC = ink.sample(0.82f)
    val gr = lin(((glowC shr 16) and 0xFF) / 255f)
    val gg = lin(((glowC shr 8) and 0xFF) / 255f)
    val gb = lin((glowC and 0xFF) / 255f)
    val fall = 1f / (0.62f * R * 0.62f * R)

    for (y in 0 until H) {
        for (x in 0 until W) {
            val i = y * W + x
            val o = i * 3
            // a bit more ink at the bottom of the page. keeps the thing sitting on something
            val v = p.ink * (0.72f + 0.5f * (1f - y / H.f()))
            val dx = x - gx
            val dy = y - gy
            val q = p.glow * exp(-(dx * dx + dy * dy) * fall)

            // soft shoulder, so bloom can pile up without clipping to a flat white blob
            val r = 1f - exp(-(img[o] + v * 0.42f + q * gr))
            val g = 1f - exp(-(img[o + 1] + v * 0.5f + q * gg))
            val b = 1f - exp(-(img[o + 2] + v * 1.0f + q * gb))

            out[i] = (0xFF shl 24) or
                ((enc(r) * 255f).toInt() shl 16) or
                ((enc(g) * 255f).toInt() shl 8) or
                (enc(b) * 255f).toInt()
        }
    }
    return out
}

// plumbing

private fun render(g: Gartvas) {
    val ss = p.ss.coerceIn(1, 3)
    val t0 = System.currentTimeMillis()

    val dots = sow(ss)
    println("  ${dots.n} dots  (${System.currentTimeMillis() - t0}ms)")

    val bins = Bins(W * ss, H * ss)
    val lut = buildLut()
    parallelBands(bins.h) { y0, y1 -> splat(dots, lut, bins, y0, y1) }
    println("  splat done (${System.currentTimeMillis() - t0}ms)")

    val light = develop(bins, ss)
    if (p.bloom > 0f) bloom(light)

    // the unbound Gartmap, since every pixel gets overwritten - no point in the bound one
    // pulling the (empty) canvas back on the way in
    val m = Gartmap(g.d)
    page(light).copyInto(m.pixels)
    m.drawToCanvas(g)

    g.canvas.drawVignette(g.d, strength = p.vig, radius = 0.82f, innerStop = 0.42f)
    if (p.grain > 0f) addGrain(g, p.grain, p.seed.toInt())
    println("  done (${System.currentTimeMillis() - t0}ms)")
}

// knobs -------------------------------------

private fun resolveParams() = Params(
    seed = pl("seed", 3L),
    out = ps("out", "cirrus.png"),
    pal = pi("pal", 0),
    ss = pi("ss", 2),

    // main gyre sits high and right of centre - the long sweep into it comes up from the lower
    // left, which is the diagonal the eye already wants to walk
    gx = pf("gx", 0.575f),
    gy = pf("gy", 0.415f),
    gyres = pi("gyres", 8),
    pull = pf("pull", 0.30f), // how fast it winds in. 0 is a ring, 1 is a drain
    reach = pf("reach", 0.35f),
    against = pf("against", 0.45f), // odds a satellite turns the other way
    core = pf("core", 0.28f),
    mincore = pf("mincore", 0.6f), // dont drop this much lower, see the note in vel()
    // the main gyre's eye, as a fraction of its reach. small: this is the subject and all the
    // best detail is packed right up against it
    eat = pf("eat", 0.11f),
    sateye = pf("sateye", 0.55f), // a satellite's, as a fraction of its own reach
    eatmin = pf("eatmin", 70f), // ...but never under this in px, or it makes specks
    fade = pf("fade", 2.4f),
    rise = pf("rise", 0.10f),

    nscale = pf("nscale", 0.0021f),
    wobble = pf("wobble", 0.42f),
    octaves = pi("octaves", 3),

    spread = pf("spread", 0.85f),
    mscale = pf("mscale", 0.0026f),

    n = pi("n", 330),
    steps = pi("steps", 1700),
    stepvar = pf("stepvar", 0.6f),
    // step along the ribbon. it has to stay under the dot size or consecutive stamps stop
    // overlapping and every ribbon comes out as a little zipper - so dont raise this on its
    // own, raise fine with it. cost me an evening of thinking the twist was to blame
    pitch = pf("pitch", 1.6f),
    spark = pf("spark", 0.06f),

    k = pi("k", 24),
    starts = pi("starts", 3), // 1 gives a telephone cord, 6 gives a plain tube. 3 is the braid
    loops = pf("loops", 4.2f),
    // 0.03 by eye. it wants to be slow: much over 0.1 and the scallop period drops under a
    // dozen steps, so the ribbon edge stops being a wave and turns into sawtooth
    twist = pf("twist", 0.03f),
    width = pf("width", 26f),
    squash = pf("squash", 0.34f), // rosette flattened along the stroke -> ribbon, not a rope
    fine = pf("fine", 1.6f),
    flow = pf("flow", 1.0f),
    jitter = pf("jitter", 0.09f),
    slop = pf("slop", 0.22f),
    breathe = pf("breathe", 0.85f),
    cscale = pf("cscale", 0.0016f),
    wash = pf("wash", 0.62f),

    exposure = pf("exposure", 2.6f),
    gamma = pf("gamma", 1.28f), // past ~1.6 the whole thing goes pastel and stops reading as light
    whiten = pf("whiten", 0.62f),
    ceiling = pf("ceiling", 0.997f), // density percentile that maps to white
    bloom = pf("bloom", 0.5f),
    bloomr = pf("bloomr", 0.006f),
    thr = pf("thr", 0.24f),

    ink = pf("ink", 0.022f),
    glow = pf("glow", 0.05f),
    vig = pf("vig", 0.62f),
    grain = pf("grain", 0.05f),
)
