package congestus

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.gfx.sdEllipsoid
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.Grid3
import dev.oblac.gart.math.between
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.henyeyGreenstein
import dev.oblac.gart.math.seededRandom
import dev.oblac.gart.math.smax
import dev.oblac.gart.math.smin
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.worley
import dev.oblac.gart.pixels.boxDownsample
import dev.oblac.gart.pixels.labelRegions
import dev.oblac.gart.util.Stopwatch
import dev.oblac.gart.util.parallelForRows
import dev.oblac.gart.vector.Vec3
import dev.oblac.gart.vector.average
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * congestus - one heaped cloud formation that fills about two thirds of a square sky.
 *
 * a cloud starts as a few soft egg shapes: a wide low body and heads that rise from it. four
 * octaves of worley noise then cut into the shapes. near a noise point, an octave pushes the
 * surface outward. far from a noise point, it pulls the surface inward. as a result, small puffs
 * sit on big puffs. an octave pushes outward only near the surface, not out in the air.
 *
 * a filter removes the small loose bits. fine simplex noise makes the edge rough.
 *
 * the clouds are a volume, not a skin. each ray goes into the cloud and collects light on its
 * way. the light decreases as e^(-density x distance), the beer-lambert law. three copies of this
 * term, each with less loss, represent the light that bounces inside.
 *
 * the light comes from the left. a powder term makes the thin parts that face the light darker.
 * sky light comes from above. the henyey-greenstein phase is also in the sum. the view is
 * orthographic and the light is far away, so the phase is one number for the whole image.
 *
 * the formation has three tiers. the foot is low and near. the body is behind it. the heads are
 * at the back and high. each tier covers the flat bases of the tier behind it. the back tiers are
 * a little hazy and have smaller puffs.
 *
 * the colors are navy and cream. a thick cream frame is on the top and on the two sides. it has
 * no bottom line. its gap from the edge is the same as its width. the clouds go over it.
 */
private const val W = 2000
private const val H = 2000
private const val S = H / 1000f

private val SEED = pi("seed", 81)
private val SS = pi("ss", 1, 1..3)
private val OUT = ps("out", "congestus")

// x desno, y dole, z od mene. ortografski.
// svetlo dolazi sleva, malo odozgo i malo otpozadi, pa desne strane padaju u senku
private val LIGHT = Vec3(-0.85f, -0.45f, 0.15f).normalize()

// oblik
private const val CELL = 95f // px, najveca worley ćelija
private const val OCT = 4
private const val LAC = 2.1f
private const val AMP = 0.6f // koliko duboko seče oktava 0
private const val GAIN = 0.8f
private const val SOFT = 3f // px mekih ivica
private const val FRAY = 4f // za koliko px "razbarušenost" pomera ivicu
private const val FRAY_CELL = 12f // px. na 7 se ivica raspala u mrvice
private const val CRUMB = 2000 // odvojeni komadići manji od ovoliko voksela mreže za svetlo se brišu
private const val LEAN = 0.12f // glave se malo naginju udesno

// svetlo
private const val SIG = 0.3f // gubitak zraka po px pri punoj gustini. na 0.5 je izgledalo kao glina
private const val SIG_L = 0.12f // isto, ka svetlu
private const val SIG_A = 0.025f // isto, za svetlo neba odozgo
private const val K_SUN = 1.4f
private const val K_SKY = 0.28f
private const val HG_G = 0.5f
private const val POWDER = 0.35f
private const val T_MIN = 0.01f // zrak staje kad ostane ovoliko svetla, odatle je oblak pun

private val SKY = Palette(0xFF1E3555, 0xFF2C4867)
private val CLOUD = Palette(0xFF3E4D60, 0xFF56667A, 0xFF8E979C, 0xFFC6C4AE, 0xFFE8E2C8)

private fun skyAt(y: Float) = SKY.sample((y / H).coerceIn(0f, 1f))

private val FRAME = CLOUD.last()
private const val FRAME_W = 38f

// dal je pixel na frame
private fun onFrame(p: Point): Boolean {
    val e = minOf(p.x, W - p.x, p.y)
    return e > FRAME_W * S && e < 2 * FRAME_W * S
}

private val PHASE = henyeyGreenstein(LIGHT.z, HG_G) / henyeyGreenstein(0f, HG_G)

// oblaci


// blob: sredina i tri poluprečnika
private class Blob(val c: Vec3, val r: Vec3)

private class Cloud(private val blobs: List<Blob>, private val base: Float, private val k: Float, scale: Float, val haze: Float, salt: Int) {
    val mid = blobs.map { it.c }.average()
    val r = blobs.maxOf { (it.c - mid).length() + maxOf(it.r.x, it.r.y, it.r.z) }
    private val ox = salt * 101.37f * S
    private val amps = FloatArray(OCT) { AMP * GAIN.pow(it) * CELL * S * scale / LAC.pow(it) }
    private val freq = FloatArray(OCT) { LAC.pow(it) / (CELL * S * scale) }

    private val rest = FloatArray(OCT + 1).also { r ->
        for (q in OCT - 1 downTo 0) r[q] = r[q + 1] + amps[q] * 0.5f
    }
    private val soft = SOFT * S * (1f + haze)
    private val fray = FRAY * S * scale
    private val frayF = 1f / (FRAY_CELL * S * scale)
    val reach = rest[0] + soft + fray

    // blobs stopljena u jedno, ravno odsečena na dnu
    fun sdf(p: Vec3): Float {
        var d = 1e9f
        for (b in blobs) d = smin(d, sdEllipsoid(p - b.c, b.r), k)
        return smax(d, p.y - base, k * 0.5f)
    }

    fun density(p: Vec3, coarse: Float): Float {
        var d = sdf(p)
        val o = Vec3(p.x + ox, p.y, p.z)
        for (q in 0 until OCT) {
            if (d > rest[q] + 0.5f * soft + fray) return 0f
            if (d < -rest[q] - soft - fray) return 1f
            if (q > 0 && amps[q] < coarse) break // presitno za mrežu svetla
            val n = amps[q] * (min(worley(o * freq[q], SEED * 7 + q), 1f) - 0.5f)

            // grudva se naduva samo gde je površina. napolju u vazduhu se to gasi, inače komadići "odlete"
            d += if (n < 0f && d > 0f) n * (1f - d / (amps[q] * 0.5f)).coerceIn(0f, 1f) else n
        }
        if (coarse == 0f) {
            if (d > 0.5f * soft + fray) return 0f
            if (d < -0.5f * soft - fray) return 1f
            val a = o * frayF
            val b = a * 2.3f
            d += fray * (0.65f * SimplexNoise.noise(a.x, a.y, a.z) + 0.35f * SimplexNoise.noise(b.x, b.y, b.z))
        }
        return (0.5f - d / soft).coerceIn(0f, 1f)
    }
}

private fun heap(rnd: Random, base: Vec3, w: Float, h: Float, scale: Float, haze: Float, salt: Int): Cloud {
    val bl = ArrayList<Blob>()
    bl += Blob(base, Vec3(w * 0.5f, h * 0.35f, w * 0.38f)) // telo, široka niska kupola
    repeat(rnd.between(3, 5)) { i ->
        val main = i == 0
        val hx = base.x + rnd.between(-0.3f, 0.3f) * w * (if (main) 0.5f else 1f)
        val top = h * (if (main) 1f else rnd.between(0.45f, 0.8f))
        val r = w * (if (main) rnd.between(0.18f, 0.23f) else rnd.between(0.11f, 0.17f))
        val hz = base.z + rnd.between(-0.15f, 0.15f) * w
        val tx = hx + LEAN * top * 0.3f

        // stub je gomila okruglih grudvi
        val steps = max(1, (top * 0.45f / r).roundToInt())
        for (q in 0 until steps) {
            val t = (q + 0.5f) / steps
            val rr = r * rnd.between(0.95f, 1.2f)
            val c = Vec3(hx + (tx - hx) * t + rnd.between(-0.2f, 0.2f) * r, base.y - top * t * 0.85f, hz + rnd.between(-0.2f, 0.2f) * r)
            bl += Blob(c, Vec3(rr, rr * 0.9f, rr))
        }
        bl += Blob(Vec3(tx, base.y - top + r, hz), Vec3(r * 1.15f, r, r * 1.05f))

        repeat(rnd.between(1, 3)) {
            val a = rnd.between(-2.6f, -0.5f)
            val rr = r * rnd.between(0.5f, 0.75f)
            val c = Vec3(tx + cos(a) * r * 0.9f, base.y - top + r + sin(a) * r * 0.75f, hz + rnd.between(-0.5f, 0.3f) * r)
            bl += Blob(c, Vec3(rr, rr * 0.95f, rr))
        }
    }
    return Cloud(bl, base.y, w * 0.07f, scale, haze, salt)
}

// formacija oblaka

private class Tier(val base: Float, val h: Float, val w: Float, val n: Int, val spread: Float, val shift: Float, val z: Float, val haze: Float, val scale: Float)

private val TIERS = listOf(
    Tier(1.04f, 0.38f, 0.55f, 3, 0.75f, -0.04f, 300f, 0f, 1.05f), // stopa
    Tier(0.79f, 0.38f, 0.58f, 2, 0.38f, 0.02f, 650f, 0.03f, 0.95f), // telo
    Tier(0.54f, 0.42f, 0.52f, 2, 0.26f, 0.08f, 1000f, 0.06f, 0.88f), // glave
)

private fun clouds(rnd: Random): List<Cloud> {
    val all = ArrayList<Cloud>()
    val fx = rnd.between(0.46f, 0.56f) // formacija stoji malo van sredine
    for (t in TIERS) repeat(t.n) { k ->
        val u = if (t.n == 1) 0f else k / (t.n - 1f) - 0.5f
        val cx = (fx + t.shift + u * t.spread + rnd.between(-0.04f, 0.04f)) * W
        val base = (t.base + rnd.between(-0.02f, 0.02f)) * H
        val w = t.w * W * rnd.between(0.9f, 1.15f)
        val h = t.h * H * rnd.between(0.85f, 1.1f)
        val z = (t.z + rnd.between(-60f, 60f)) * S
        all += heap(rnd, Vec3(cx, base, z), w, h, t.scale, t.haze, all.size)
    }
    return all
}

// zapremina prostor

private class Smp {
    var rho = 0f
    var haze = 0f
}

// jedan zrak kroz piksel: odakle dokle ide i šta je sakupio
private class Ray {
    var z = 0f
    var zEnd = 0f
    var t = 1f // koliko svetla još prolazi
    var lum = 0f
    var hz = 0f
}

private class Sky(val clouds: List<Cloud>) {

    // koliko daleko od x/y/z sigurno nema oblaka. 0 ili manje: gledaj bliže
    fun free(p: Vec3): Float {
        var d = 1e9f
        for (c in clouds) {
            val ds = (p - c.mid).length() - c.r - c.reach
            d = min(d, if (ds > 0f) ds else c.sdf(p) - c.reach)
        }
        return d
    }

    // pobeđuje najgušći oblak i donosi svoju izmaglicu
    fun sample(p: Vec3, coarse: Float, s: Smp) {
        s.rho = 0f
        s.haze = 0f
        for (c in clouds) {
            val d = p - c.mid
            val rr = c.r + c.reach
            if (d.dot(d) > rr * rr) continue
            val r = c.density(p, coarse)
            if (r > s.rho) {
                s.rho = r
                s.haze = c.haze
            }
        }
    }

    // odakle dokle zrak kroz px seče sfere oko oblaka. kad ne seče nijednu, z ostaje iza zEnd
    fun span(px: Point, r: Ray) {
        var z = Float.MAX_VALUE
        var zEnd = -Float.MAX_VALUE
        for (c in clouds) {
            val rr = c.r + c.reach
            val dx = px.x - c.mid.x
            val dy = px.y - c.mid.y
            val d2 = dx * dx + dy * dy
            if (d2 >= rr * rr) continue
            val h = sqrt(rr * rr - d2)
            z = min(z, c.mid.z - h)
            zEnd = max(zEnd, c.mid.z + h)
        }
        r.z = z
        r.zEnd = zEnd
    }
}

// mreže za svetlo su vokseli od 4 px. bilinearno u jednom sloju po y, napolju 0
private fun Grid3.layer(j: Int, fi: Float, fk: Float): Float {
    if (fi < 0f || fk < 0f || fi > nx - 1.001f || fk > nz - 1.001f) return 0f
    val i = fi.toInt()
    val k = fk.toInt()
    val tz = fk - k
    val o0 = idx(i, j, k)
    val o1 = idx(i + 1, j, k)
    val a = values[o0] + (values[o0 + 1] - values[o0]) * tz
    val b = values[o1] + (values[o1 + 1] - values[o1]) * tz
    return a + (b - a) * (fi - i)
}

private class Light(val sun: Grid3, val up: Grid3, private val keep: BooleanArray) {
    fun ok(p: Vec3): Boolean {
        val d = p - sun.origin
        val i = floor(d.x / sun.step).toInt()
        val j = floor(d.y / sun.step).toInt()
        val k = floor(d.z / sun.step).toInt()
        for (c in k..k + 1) for (b in j..j + 1) for (a in i..i + 1) {
            if (a < 0 || b < 0 || c < 0 || a >= sun.nx || b >= sun.ny || c >= sun.nz) continue
            if (keep[sun.idx(a, b, c)]) return true
        }
        return false
    }
}

// 6-povezani komadi oblaka. veliki ostaju
private fun pieces(full: Grid3): BooleanArray {
    val r = labelRegions(BooleanArray(full.values.size) { full.values[it] < 0.5f }, full.nx, full.ny, full.nz)
    val sizes = r.sizes()
    return BooleanArray(r.label.size) { r.label[it] >= 0 && sizes[r.label[it]] >= CRUMB }
}

private fun light(sky: Sky): Light {
    val (dens, full) = densities(sky)
    val keep = pieces(full)
    dropCrumbs(dens, keep)
    blur121(dens)
    val (sun, up) = sweeps(dens)
    return Light(sun, up, keep)
}

private fun Grid3.twin() = Grid3(step, origin, nx, ny, nz) // prazna mreža istog oblika

// gustina u vokselima od 4 px, sa marginom oko slike
private fun densities(sky: Sky): Pair<Grid3, Grid3> {
    val g = 4f * S
    val m = 64f * S
    val zLo = sky.clouds.minOf { it.mid.z - it.r - it.reach }
    val zHi = sky.clouds.maxOf { it.mid.z + it.r + it.reach }
    val nx = ((W + 2 * m) / g).toInt() + 2
    val ny = ((H + 2 * m) / g).toInt() + 2
    val nz = ((zHi - zLo) / g).toInt() + 2
    val dens = Grid3(g, Vec3(-m, -m, zLo), nx, ny, nz)
    val full = dens.twin()
    parallelForRows(ny) { j0, j1 ->
        val s = Smp()
        for (j in j0 until j1) for (i in 0 until nx) for (k in 0 until nz) {
            val p = Vec3(-m + i * g, -m + j * g, zLo + k * g)
            if (sky.free(p) > 0f) continue
            sky.sample(p, 1.2f * g, s)
            dens.values[dens.idx(i, j, k)] = s.rho
            sky.sample(p, 0f, s)
            full.values[full.idx(i, j, k)] = s.rho
        }
    }
    return dens to full
}

private fun dropCrumbs(dens: Grid3, keep: BooleanArray) {
    for (o in dens.values.indices) if (!keep[o] && dens.values[o] >= 0.5f) dens.values[o] = 0f
}

// 1-2-1 blur po sve tri ose, da se ne bi videle pruge
private fun blur121(dens: Grid3) {
    val nx = dens.nx
    val nz = dens.nz
    for (stride in intArrayOf(1, nz, nx * nz)) {
        val src = dens.values.copyOf()
        parallelForRows(dens.ny) { j0, j1 ->
            for (o in j0 * nx * nz until j1 * nx * nz) {
                val a = if (o - stride >= 0) src[o - stride] else src[o]
                val b = if (o + stride < src.size) src[o + stride] else src[o]
                dens.values[o] = 0.25f * a + 0.5f * src[o] + 0.25f * b
            }
        }
    }
}

// dva prolaza niz redove voksela: šta leži između svakog voksela i svetla, i iznad njega
private fun sweeps(dens: Grid3): Pair<Grid3, Grid3> {
    val sun = dens.twin()
    val up = dens.twin()
    val nx = dens.nx
    val nz = dens.nz
    val g = dens.step
    val ax = LIGHT.x / -LIGHT.y
    val az = LIGHT.z / -LIGHT.y
    val step = g / -LIGHT.y // put kroz jedan sloj, ka svetlu
    for (j in 0 until dens.ny) {
        parallelForRows(nx) { i0, i1 ->
            for (i in i0 until i1) for (k in 0 until nz) {
                val o = sun.idx(i, j, k)
                val d = dens.values[o]
                sun.values[o] = (if (j > 0) sun.layer(j - 1, i + ax, k + az) else 0f) + d * step
                var above = 0f
                if (j > 0) {
                    for (a in -1..1) for (b in -1..1) above += up.values[up.idx((i + a).coerceIn(0, nx - 1), j - 1, (k + b).coerceIn(0, nz - 1))]
                    above /= 9f
                }
                up.values[o] = above + d * g
            }
        }
    }
    return sun to up
}

private val NEAR = floatArrayOf(1.5f * S, 3f * S, 4.5f * S)

private fun lit(sky: Sky, l: Light, p: Vec3, s: Smp): Float {
    // prvih par px ka svetlu sa punim detaljem,
    var near = 0f
    var t = 0f
    for (st in NEAR) {
        val q = p + LIGHT * (t + st * 0.5f)
        if (l.ok(q)) {
            sky.sample(q, 0f, s)
            near += s.rho * st
        }
        t += st
    }
    val tau = (near + l.sun.at(p + LIGHT * (t + l.sun.step))) * SIG_L / S
    // beer-lambert x3. svaka kopija gubi manje i vredi upola, kao svetlo koje se odbilo
    val beer = (exp(-tau) + 0.5f * exp(-0.3f * tau) + 0.25f * exp(-0.09f * tau)) / 1.75f
    val powder = 1f - exp(-2f * near * SIG_L / S)
    val sun = K_SUN * PHASE * beer * (1f - POWDER + POWDER * powder)
    return sun + K_SKY * exp(-SIG_A / S * l.up.at(p.copy(y = p.y - 1.5f * l.sun.step)))
}

private fun render(sky: Sky, l: Light): IntArray {
    val ws = W * SS
    val big = IntArray(ws * H * SS)
    parallelForRows(H * SS) { y0, y1 ->
        val r = Ray()
        val s = Smp()
        val s2 = Smp()
        for (by in y0 until y1) for (bx in 0 until ws) {
            val px = Point((bx + 0.5f) / SS, (by + 0.5f) / SS)
            sky.span(px, r)
            march(sky, l, px, hash01(bx, by, 0, 99), r, s, s2)
            big[by * ws + bx] = shade(px, r)
        }
    }
    return big
}

// zrak ide kroz oblake i sakuplja svetlo i izmaglicu, dok skoro sve svetlo ne nestane
private fun march(sky: Sky, l: Light, px: Point, jitter: Float, r: Ray, s: Smp, s2: Smp) {
    var t = 1f
    var lum = 0f
    var hz = 0f
    val dz = 1f * S
    var z = r.z + dz * jitter
    while (z < r.zEnd && t > T_MIN) {
        val p = Vec3(px.x, px.y, z)
        val e = sky.free(p)
        if (e > 0f) {
            z += max(dz, e * 0.8f)
            continue
        }
        if (l.ok(p)) sky.sample(p, 0f, s) else s.rho = 0f
        if (s.rho > 0f) {
            val a = 1f - exp(-SIG / S * s.rho * dz)
            hz += t * a * s.haze
            lum += t * a * lit(sky, l, p, s2)
            t *= 1f - a
        }
        z += dz
    }
    r.t = t
    r.lum = lum
    r.hz = hz
}

// boja piksela: postavi oblak preko neba i rama
private fun shade(px: Point, r: Ray): Int {
    val alpha = 1f - r.t
    val air = skyAt(px.y)
    val back = if (r.t > T_MIN && onFrame(px)) FRAME else air // pun oblak sakriva sve
    if (alpha < 1e-4f) return back

    val cloud = lerpColor(CLOUD.sample((r.lum / alpha).coerceIn(0f, 1f)), air, r.hz / alpha)
    return lerpColor(back, cloud, alpha)
}

fun main(args: Array<String>) {
    val sw = Stopwatch()
    val gart = Gart.of("congestus", W, H)
    val sky = Sky(clouds(seededRandom(SEED)))
    val light = light(sky)
    val big = render(sky, light)
    val g = gart.gartvas()
    Gartmap(g).use { m ->
        boxDownsample(big, SS, m)
        m.drawToCanvas()
    }
    gart.saveImage(g, OUT.ensureExtension())
    println("congestus: seed=$SEED, ${sky.clouds.size} clouds, ${sw.ms}ms")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
