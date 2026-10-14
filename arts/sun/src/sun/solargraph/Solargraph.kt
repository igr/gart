package sun.solargraph

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palette
import dev.oblac.gart.fx.addScanlines
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.rndGaussian
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.math.wrapPi
import dev.oblac.gart.noise.OpenSimplexNoise
import dev.oblac.gart.pixels.boxBlur
import dev.oblac.gart.pixels.splat
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan
import kotlin.random.Random

/**
 * solargraph - half a year of sun on photo paper in a pinhole can.
 *
 * the can faces south. the exposure starts at the winter solstice and stops at the summer solstice.
 * each day, the sun draws one arc on the paper. in winter, the arc is low and short. in summer, it is high and long.
 * near the solstices, the height of the sun at noon changes slowly. thus the arcs are close together there.
 * clouds cut the arcs into dashes. near the horizon, the sun is weak, so the arcs fade at their ends.
 * the sides of the paper get more light, so the arcs become red there.
 * there is no skyline, only the arcs.
 * the paper stays undeveloped. its inverted scan has the colors of print-out paper.
 * an old CRT shows the scan. the CRT adds static and scanlines.
 */
private val seed = pi("seed", 1)
private val cloud = pf("cloud", 0.45f, 0f..1f)      // the time the sun is behind clouds
private val out = ps("out", "solargraph")

private const val DEG = PIf / 180f
private const val DT = 0.2f                 // minutes a step, keeps the fast summer noon under a px

// facing south
private const val LAT = 50f                 // deg north. under 46 the summer arcs run off the top
private const val FOV = 80f                 // half the paper across, deg of azimuth. a can sees nearly 90
private const val HORIZON = 0.88f           // where the arcs end, down the page
private val sinPhi = sin(LAT * DEG)
private val cosPhi = cos(LAT * DEG)
private val tanPhi = tan(LAT * DEG)
private const val edge = FOV * DEG

private val paper = Palette(
    0xFF100B09, 0xFF22130F, 0xFF3A1E18, 0xFF3E2733, 0xFF2E3558, 0xFF234C80, 0xFF2A6E9C, 0xFF3F93AE,
    0xFF7DBDBA, 0xFFC7DDBA, 0xFFF6EDCC, 0xFFFFFAEC, 0xFFFBE1B8, 0xFFEFAE86, 0xFFCF7460,
)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("solargraph", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    expose(g)

    gart.saveImage(g, out.ensureExtension("png"))
    if (!headless) gart.window().showImage(g)
}

private fun expose(g: Gartvas) {
    val d = g.d
    val w = d.w
    val h = d.h
    val noise = OpenSimplexNoise(seed.toLong())

    val sun = FloatArray(w * h)
    val soft = FloatArray(w * h)        // days under thin high cloud, the sun is a smudge
    trails(sun, soft, d, Random(seed), noise)

    val core = boxBlur(sun, w, h, 1, 1)    // the sun through the pinhole, two or three px wide
    val halo = boxBlur(sun, w, h, 3)       // light spreading in the paper
    val aureole = boxBlur(sun, w, h, 36)   // the bright sky round the sun, wherever it went
    val smudge = boxBlur(soft, w, h, 3)

    val lut = paper.stretchOklab(1300)
    val lo = 0.003f
    val span = ln(1.2f / lo)
    // the paper curls round the back of the can, so its sides sit nearer the pinhole and burn harder.
    // 6 reds the flanks, 3 only warms the feet
    val burnAt = FloatArray(w) { 1f + 6f * (it / (w / 2f) - 1f).pow(2) }
    val m = Gartmap(g)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val i = y * w + x

            var dose = 0.015f + burnAt[x] * (9.2f * core[i] + 2f * halo[i] + 10f * smudge[i]) + 0.4f * aureole[i]

            dose += 0.0225f * (hash01(x, y, seed) - 0.5f)
            val v = ln(max(dose, lo) / lo) / span + 0.03f * noise.random2D(x * 0.004f + 7f, y * 0.004f)
            m.pixels[i] = lut[(v / 1.3f * 1299f).toInt().coerceIn(0, 1299)]
        }
    }
    addScanlines(m, pitch = 4f, depth = 0.3f)
    m.drawToCanvas()
}

private const val START = 355               // day of year
private const val DAYS = 183                // solstice to solstice. 365 drew them all 2x
private const val WINTER = 0.3f             // how much cloudier december is than june

// one line a day, and the weather puts a random opacity on every step of it.
// the front stays for a few days, the haze for a day, cumulus cross the sun in minutes
private fun trails(sun: FloatArray, soft: FloatArray, d: Dimension, rng: Random, noise: OpenSimplexNoise) {
    var front = 0f
    for (day in 0 until DAYS) {
        val season = cos(TAUf * ((START + day) % 365 + 10) / 365f)     // 1 at the winter solstice, -1 at the summer one
        front = 0.8f * front + 0.6f * rng.rndGaussian()
        val cut = 1.5f * (cloud + WINTER * 0.5f * season + 0.35f * front).coerceIn(0f, 1f) - 0.75f
        val haze = rng.rndf(0.5f, 1f)
        val thin = rng.rndf().pow(2)
        arc(-23.44f * DEG * season, d) { t, p, up ->
            val c = 0.65f * noise.random2D(t / 22f, day * 1.7f) + 0.35f * noise.random2D(t / 7f, day * 1.7f + 40f)
            val v = haze * smoothstep(cut - 0.1f, cut + 0.1f, c) * exp(-0.4f / up) * DT
            splat(sun, d.w, p, v * (1f - thin))
            splat(soft, d.w, p, v * thin * 0.7f)
        }
    }
}

// the formula: the sun of one day walks from sunrise to sunset, a step every DT minutes.
// step gets the minute from noon, the spot on the paper and the sine of the height of the sun
private inline fun arc(dec: Float, d: Dimension, step: (t: Float, p: Point, up: Float) -> Unit) {
    val sx = d.wf / 2f / edge       // px per radian across. the can maps azimuth straight onto the paper
    val hy = HORIZON * d.hf
    val sinDec = sin(dec)
    val cosDec = cos(dec)
    val sunset = acos((-tanPhi * tan(dec)).coerceIn(-1f, 1f))    // as an hour angle -- sunrise is at -sunset
    var hr = -sunset
    while (hr < sunset) {
        val up = sinDec * sinPhi + cosDec * cos(hr) * cosPhi        // sine of the height
        val a = wrapPi(atan2(-cosDec * sin(hr), sinDec * cosPhi - cosDec * cos(hr) * sinPhi) - PIf)

        if (up > 0f && abs(a) < edge) {
            step(hr / TAUf * 1440f, Point(d.w / 2f + sx * a, hy - sx * cos(a) * tan(asin(up))), up)
        }
        hr += DT * TAUf / 1440f
    }
}
