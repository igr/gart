package cera

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Radians
import dev.oblac.gart.color.BwColors
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.color.lumOf
import dev.oblac.gart.color.redf
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.gfx.drawLine
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.between
import dev.oblac.gart.math.dist
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.noiseOffset
import dev.oblac.gart.pixels.gaussianBlur
import dev.oblac.gart.spirograph.createSpirograph
import dev.oblac.gart.util.parallelForRows
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.PathDirection
import org.jetbrains.skia.PathMeasure
import org.jetbrains.skia.Point
import org.jetbrains.skia.RRect
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * the cloth under cera, in three parts. the pen (Tjanting) lays the wax, the cracks (Crackle) break
 * it, and the baths (Batik) dye the cloth. cera.kt only says where the rings go and in which bath.
 *
 * a wheel rolls round a core that is never drawn, and the pen sits on its rim. where the wheel
 * touches the core the pen stops, but the tjanting still drips. so every cusp gets a drop of wax,
 * and a fast pen leaves a thin line.
 *
 * there is one bath for each palette color, light to dark. wax keeps the color that the cloth has
 * when the wax goes on, and older wax covers newer wax. at the end the wax cracks, and the dark dye
 * fills the cracks.
 */
internal const val W = 1200
internal const val H = 1200
internal const val M = 300 // we need biigger screen
private val VD = Dimension(W + 2 * M, H + 2 * M)

private const val STEP = 2.5f // px
private const val POW = 0.7f // at 1 all the wax of a slow step went into the line
private const val MOTT = 0.18f
private const val LAST = 0.85f
private const val CLOTH = BwColors.parchment

internal class Trace(val pts: List<Point>, val ws: FloatArray, val stage: Int)

// the core. a rounded triangle, apex up, and a few lumps so it looks drawn by hand. it runs
// clockwise - don't flip it, the other way round the pen slows on the far side of the wheel
internal class Blob(rnd: Random, lumpy: Float, private val tri: Float) {
    init {
        rnd.nextFloat()
    }
    val turn = rnd.nextFloat() * TAUf
    val amp = FloatArray(4) { rnd.between(0f, lumpy / (it + 1)) }
    val ph = FloatArray(4) { rnd.nextFloat() * TAUf }
    private val up = 3f * turn + 3f * TAUf / 4f // the phase

    fun path(cx: Float, cy: Float, r: Float, dent: Float): Path {
        var t = 1f
        var pts = rim(t, r)
        while (tightest(pts) < dent) {
            t *= 0.9f
            pts = rim(t, r)
        }
        val pb = PathBuilder()
        for (i in pts.indices) {
            val x = M + cx + pts[i].x
            val y = M + cy + pts[i].y
            if (i == 0) pb.moveTo(x, y) else pb.lineTo(x, y)
        }
        return pb.closePath().detach()
    }

    // the lumps and the triangle times t, round the middle of the box
    private fun rim(t: Float, r: Float): List<Point> {
        val raw = List(360) {
            val a = it * TAUf / 360
            var rr = 1f
            for (h in amp.indices) rr += t * amp[h] * cos((h + 2) * a + ph[h])
            rr += t * tri * cos(3 * a + up)
            val px = rr * cos(a)
            val py = rr * sin(a)
            Point(px * cos(turn) - py * sin(turn), px * sin(turn) + py * cos(turn))
        }
        val x0 = raw.minOf { it.x }
        val x1 = raw.maxOf { it.x }
        val y0 = raw.minOf { it.y }
        val y1 = raw.maxOf { it.y }
        val s = 2f * r / max(x1 - x0, y1 - y0)
        return raw.map { Point((it.x - (x0 + x1) / 2f) * s, (it.y - (y0 + y1) / 2f) * s) }
    }
}

// radius of the tightest dent, px. the rim runs clockwise, so a dent turns the other way
private fun tightest(pts: List<Point>): Float {
    var tight = Float.MAX_VALUE
    for (i in pts.indices) {
        val a = pts[(i + pts.size - 1) % pts.size]
        val b = pts[i]
        val c = pts[(i + 1) % pts.size]
        val turn = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
        if (turn < 0f) tight = min(tight, dist(a, b) * dist(b, c) * dist(c, a) / (-2f * turn))
    }
    return tight
}


// the tjanting on the rim of the wheel. it makes the traces and says how wide the wax is
internal class Tjanting(seed: Int, private val wax: Float, private val wmax: Float) {
    private val nz = noiseOffset(seed.toLong())
    private fun noise(x: Float, y: Float) = SimplexNoise.noise(x + nz, y)

    private fun trace(guide: Path, r: Float, arches: Int, laps: Int, stage: Int, rnd: Random): Trace {
        val len = PathMeasure(guide).length
        val samples = ((len + TAUf * r) / STEP).toInt()
        val da = TAUf * (arches + 1f / laps) / (samples - 1)
        val pts = createSpirograph(VD, guide, r, Radians.of(da), samples, laps).points.map { Point(it.x - M, it.y - M) }
        val ds = FloatArray(pts.size - 1) { hypot(pts[it + 1].x - pts[it].x, pts[it + 1].y - pts[it].y) }
        val ref = ds.sorted()[ds.size / 2] // the median step
        val off = rnd.nextFloat() * 100f
        var s = 0f
        var since = 0f
        var dip = rnd.between(250f, 700f) // px of line
        val ws = FloatArray(ds.size) {
            s += ds[it]
            since += ds[it]
            if (since > dip) {
                since = 0f
                dip = rnd.between(250f, 700f)
            }

            val flow = (1.25f - 0.45f * since / dip) * (1f + 0.2f * noise(s * 0.004f + off, stage * 3.1f))
            (wax * flow * (ref / max(ds[it], 0.05f)).pow(POW)).coerceIn(0.6f, wmax)
        }
        return Trace(pts, ws, stage)
    }

    fun rolled(guide: Path, r: Float, rho: Float, gap: Float, stage: Int, rnd: Random): Trace {
        val len = PathMeasure(guide).length
        val arches = max(1f, floor(rho * len / (TAUf * r))).toInt()
        val laps = (len / (arches * gap)).toInt().coerceIn(2, 60)
        return trace(guide, r, arches, laps, stage, rnd)
    }

    fun hem(border: Float, stage: Int, rnd: Random): Trace {
        val pb = PathBuilder()
        pb.addRRect(RRect.makeLTRB(M + border, M + border, M + W - border, M + H - border, border * 0.8f), PathDirection.CLOCKWISE)
        return rolled(pb.detach(), border * 0.42f, 1.05f, 13f, stage, rnd)
    }
}

internal class Crackle(seed: Int) {
    private val nz = noiseOffset(seed.toLong())
    private fun noise(x: Float, y: Float) = SimplexNoise.noise(x + nz, y)
    private val cracka = Random(seed * 7919L + 6).nextFloat() * 3.1416f // crumpled

    private fun vein(p: Point, a: Float, sx: Float, sy: Float, salt: Float): Float {
        val ca = cos(a)
        val sa = sin(a)
        fun f(px: Float, py: Float) = noise((px * ca - py * sa) * sx + salt, (px * sa + py * ca) * sy - salt)
        val n = f(p.x, p.y)
        val gx = (f(p.x + 0.7f, p.y) - n) / 0.7f
        val gy = (f(p.x, p.y + 0.7f) - n) / 0.7f
        val d = abs(n) / max(sqrt(gx * gx + gy * gy), 1e-5f)
        val fade = smoothstep(-0.35f, 0.35f, noise(p.x * 0.009f + salt * 3f, p.y * 0.009f))
        return ((1f - smoothstep(0.25f, 1.1f, d)) + 0.2f * (1f - smoothstep(0f, 3.5f, d))) * fade
    }

    fun at(p0: Point): Float {
        val p = Point(
            p0.x + 14f * noise(p0.x * 0.006f + 300f, p0.y * 0.006f),
            p0.y + 14f * noise(p0.x * 0.006f, p0.y * 0.006f + 300f),
        )
        val a = vein(p, cracka, 0.012f, 0.0025f, 11f)
        val b = vein(p, cracka + 1.1f, 0.016f, 0.0035f, 23f)
        val c = vein(p, cracka + 2.2f, 0.02f, 0.005f, 37f)
        val e = vein(p, cracka + 0.5f, 0.035f, 0.03f, 51f)
        return maxOf(a, b, 0.8f * c, 0.5f * e)
    }
}

// the cloth

internal class Batik(private val seed: Int, pal: Int, private val ss: Int, private val crack: Float) {
    private val nz = noiseOffset(seed.toLong())
    private fun noise(x: Float, y: Float) = SimplexNoise.noise(x + nz, y)
    private val cracks = Crackle(seed)

    val pal = Palettes.coolPalette(pal).sortedBy { -lumOf(it) }
    val K = this.pal.size
    private val ramp = (Palette.of(CLOTH) + this.pal).stretchOklab(1024)

    // ---- wax ----

    private fun waxMask(traces: List<Trace>, stage: Int): FloatArray {
        val g = Gartvas(Dimension(W * ss, H * ss))
        val c = g.canvas
        c.clear(0xFF000000.toInt())
        c.save()
        c.scale(ss.toFloat(), ss.toFloat())
        for (t in traces) if (t.stage == stage) {
            for (i in t.ws.indices) {
                val p = strokeOf(0xFFFFFFFF.toInt(), t.ws[i]).apply { strokeCap = PaintStrokeCap.ROUND }
                c.drawLine(t.pts[i], t.pts[i + 1], p)
            }
        }
        c.restore()
        val f = Gartmap(g).use { m -> FloatArray(m.pixels.size) { redf(m.pixels[it]) } }
        gaussianBlur(f, W * ss, H * ss, 1f)
        return f
    }

    // the baths

    fun dye(gart: Gart, traces: List<Trace>): Gartvas {
        val masks = List(K) { waxMask(traces, it) }
        val big = gart.supersampled(ss)
        Gartmap(big).use { m ->
            val bw = W * ss
            parallelForRows(H * ss) { y0, y1 ->
                for (y in y0 until y1) for (x in 0 until bw) {
                    val i = y * bw + x
                    val fx = (x + 0.5f) / ss
                    val fy = (y + 0.5f) / ss
                    val fine = noise(fx * 0.7f, fy * 0.7f) * 0.45f + (hash01(x, y, 7, seed) - 0.5f) * 0.5f
                    val seal = 0.97f + 0.03f * noise(fx * 0.05f + 70f, fy * 0.05f)
                    // rows and columns of thread take the dye a bit unevenl
                    val thread = 1f + 0.03f * (hash01(fy.toInt(), 0, 5, seed) - 0.5f) + 0.025f * (hash01(fx.toInt(), 0, 6, seed) - 0.5f)
                    var cr = 0f
                    var acc = 0f
                    var dose = 0f
                    for (k in 0 until K) {
                        val res = smoothstep(0.25f, 0.75f, masks[k][i] + 0.45f * fine) * seal
                        acc = max(acc, res)
                        val mott = 1f + MOTT * (0.6f * noise(fx * 0.005f + k * 31f, fy * 0.005f) + 0.5f * noise(fx * 0.016f + 0.4f * fy * 0.016f, fy * 0.016f + k * 17f))
                        dose += (1f - acc) * mott * thread * (if (k == K - 1) LAST else 1f)
                    }

                    if (acc > 0.01f) cr = min(1f, acc * crack * cracks.at(Point(fx, fy)))
                    val base = ramp[(dose / K * 1023f).toInt().coerceIn(0, 1023)]
                    m.pixels[i] = if (cr > 0f) lerpColor(base, ramp[((dose + (K - dose) * 0.9f) / K * 1023f).toInt().coerceIn(0, 1023)], cr) else base
                }
            }
            m.drawToCanvas()
        }
        return big.downsample(ss)
    }
}
