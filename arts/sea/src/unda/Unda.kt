package unda

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Radians
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.color.rgb
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.gfx.Bokashi
import dev.oblac.gart.gfx.alpha
import dev.oblac.gart.gfx.drawBokashi
import dev.oblac.gart.gfx.drawBokashiScreen
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.lineOrMove
import dev.oblac.gart.gfx.outlineOf
import dev.oblac.gart.gfx.shrink
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.pl
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.between
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.mix
import dev.oblac.gart.noise.fbm
import dev.oblac.gart.noise.noiseOffset
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * unda - a woodblock sea.
 *
 * one rule makes almost all of the page. all cuts on the page come from one family of
 * curves. each cut samples the family at a different depth. the heavy cuts are the crests.
 * the fine broken cuts between the crests are gouge marks.
 *
 * there is no shading. the gouge marks give the water its tone. the tone comes only from
 * the density of the cuts. a woodblock can do nothing else.
 *
 * the swell is an exponential ridge. the ridge flattens as it comes toward you. as a
 * result, everything radiates from one peak.
 */
private const val W = 1200
private const val H = 1800
private const val MARGIN = 46f // the paper around the block

// the sample pitch along a cut, in px. small enough that the taper shows, large enough that the render is fast
private const val STEP = 4f

private val RASTER_ANGLE = Radians(0.48f) // the screen angle. all values are good except 0 and 45. those two angles make moire against a rectangular page

// only the knobs that are still worth a sweep. the rest are settled. they are constants next to
// the code that uses them
private data class Params(
    val seed: Long,
    val out: String,
    val pal: Int,
    // the swell
    val height: Float, val spreadl: Float, val spreadr: Float,
    // depth
    val crests: Int, val fall: Float, val decay: Float,
    // the hand
    val roll: Float, val uneven: Float, val wander: Float,
    // the cut
    val pitch: Float, val nib: Float, val bite: Float,
    // the crests
    val gap: Float, val key: Float, val crown: Float,
    // the sky
    val sky: Float,
    val skyfall: Float,
    val raster: Float,
    val rpitch: Float,
    // paper
    val mottle: Float, val age: Float, val grain: Float,
)

private val p = resolveParams()
private val rnd = Random(p.seed)

private val nzoff = noiseOffset(p.seed)

// the impressions

// sky is the wash from the top edge. it is in the same family as the ink, one shade away from it.
// stain is the color that mottle and age pull the paper toward
private class UndaPalette(val paper: Int, val light: Int, val ink: Int, val sky: Int, val stain: Int)

private val palettes = arrayOf(
    // ai - the indigo that printers usually use for a sea block
    UndaPalette(0xFFE7DEC4.toInt(), 0xFFF4EFE1.toInt(), 0xFF20405F.toInt(), 0xFF3D648A.toInt(), 0xFFBFA477.toInt()),
    // sumi - black ink on gray paper, no color at all
    UndaPalette(0xFFE2DED2.toInt(), 0xFFF3F1E9.toInt(), 0xFF23252A.toInt(), 0xFF474C55.toInt(), 0xFFA8A091.toInt()),
    // beni - a red block on pale paper, the look of a late impression
    UndaPalette(0xFFF0E5D1.toInt(), 0xFFFAF4E7.toInt(), 0xFF7C2C21.toInt(), 0xFF9E4A38.toInt(), 0xFFC9A47C.toInt()),
)

private val pal = palettes[p.pal % palettes.size]

// ---- the block ----

private val panel = Rect.makeLTRB(MARGIN, MARGIN, W - MARGIN, H - MARGIN)
private val pw = panel.width
private val ph = panel.height

// where the peak of the swell sits, as fractions of the panel. it is a little right of center and
// almost on the top edge. everything else hangs from this point
private const val PEAKX = 0.52f
private const val PEAKY = 0.035f

private val peakX = panel.left + pw * PEAKX
private val ridgeH = ph * p.height
private val topY = panel.top + ph * PEAKY + ridgeH
private val botY = panel.bottom + ph * 0.12f // the last crest sits below the frame, so the foreground continues past the edge of the page

private val layer = 1f / (p.crests - 1).coerceAtLeast(1) // the depth of one band

// all curves sample the same lattice. the lattice extends a little past the frame, so no curve
// ends on the frame
private val x0 = panel.left - 14f
private val x1 = panel.right + 14f
private val cols = ((x1 - x0) / STEP).toInt() + 1
private val lattice = FloatArray(cols) { x0 + it * STEP }

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("unda", W, H)

    println(gart)
    println(p)

    val g = gart.gartvas()
    val draw = UndaDraw(g)
    patina(g)

    val output = p.out.ensureExtension("png")
    gart.saveImage(g, output)

    if (!headless) gart.window().show(draw)
}

// hot reload needs a real class, not a lambda
private class UndaDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

// the swell -------------------------------------------------------

private const val DRIFT = -0.06f // the peak moves a little to the left as it comes forward, in panel widths

/**
 * how much the swell lifts the water at [x] for depth [s]. the shape is a cusp at the peak with
 * exponential shoulders: sharp on top, with long tails. the cusp flattens as the wave comes
 * toward you. the two spreads are different on purpose. a wide shoulder makes long lazy sweeps.
 * a tight shoulder stacks the crests on each other.
 */
private fun ridge01(x: Float, s: Float): Float {
    val ss = s.coerceIn(0f, 1f)
    // the ridge moves sideways as it comes toward you. without this movement, every crest has its
    // peak at the same x, and the picture gets a seam of chevrons straight down the middle
    val px = peakX + DRIFT * ss * pw +
        fbm(ss * 3.2f + 77f + nzoff, 5f, octaves = 2, lacunarity = 2.1f, gain = 0.5f) * p.wander * pw
    val u = abs(x - px) / pw
    val w = (if (x < px) p.spreadl else p.spreadr).coerceAtLeast(0.02f)
    // a sharp cusp far away, rounder as it comes near. a near wave has no point on it
    return exp(-(u / w).pow(1f + 1.5f * ss)) * (1f - ss).pow(p.decay)
}

// the clean math underneath: a power pushes the depth down the page, then the ridge lifts it
private fun surface(x: Float, s: Float): Float {
    val t = s.coerceAtLeast(0f).pow(p.fall)
    return lerp(topY, botY, t) - ridgeH * ridge01(x, s)
}

private const val ROLL_SCALE = 0.0016f // x scale of the roll noise

// how far the swell moves away from the clean math. the roll applies to the depth, never to y.
// bands cannot swap places that way. also, the same roll moves more where the bands are further
// apart, which is what perspective wants
private fun rollAt(x: Float, s: Float) =
    fbm(x * ROLL_SCALE + nzoff, s * 5.5f + 19f, octaves = 2, lacunarity = 2.1f, gain = 0.45f)

// the paper is not flat. the buckle applies to position only, so all lines on the page bend together
private const val BUCKLE = 2.4f // how far out of flat, in px

private fun buckled(x: Float, y: Float) =
    y + fbm(x * 0.0011f + 301f, y * 0.0011f, octaves = 2, lacunarity = 2.2f, gain = 0.5f) * BUCKLE

// the curve that we actually draw: the clean surface at depth s, pushed by a roll of r, then buckled
private fun rolled(x: Float, s: Float, r: Float) =
    buckled(x, surface(x, (s + r * p.roll * layer).coerceAtLeast(0f)))

private fun waveY(x: Float, s: Float) = rolled(x, s, rollAt(x, s))

// a scallop of foam on a crest. it lifts the key line, and this opens white paper under the line
private class Lobe(val x: Float, val hw: Float, val h: Float) {
    fun at(px: Float): Float {
        val u = (px - x) / hw
        if (u <= -1f || u >= 1f) return 0f
        return h * (cos(u * PIf) * 0.5f + 0.5f).pow(1.15f)
    }
}

private class Crest(val s: Float, val lobes: List<Lobe>) {
    fun y(x: Float): Float {
        // the tallest scallop wins. the lifts do not sum. summed lifts push the crest up through the
        // wave behind it, and the sea turns into snowdrifts
        var lift = 0f
        for (l in lobes) lift = max(lift, l.at(x))
        return waveY(x, s) - lift
    }

    // the same on the lattice, and the roll there, computed once. the bands, the light and the key
    // line all read these instead of sampling the crest again
    val ys = FloatArray(cols) { y(lattice[it]) }
    val rs = FloatArray(cols) { rollAt(lattice[it], s) }
}

// where each crest sits in depth. the spacing is not even. it is a random walk of gaps, normalized
// back onto 0..1, so some waves come through fat and others come through thin. an even stack
// looks like a contour map, and a contour map is exactly what this piece must not look like
private val crestS: FloatArray = run {
    val gaps = FloatArray(p.crests - 1) { 1f + rnd.between(-p.uneven, p.uneven) }
    val total = gaps.sum()
    val out = FloatArray(p.crests)
    var acc = 0f
    for (i in 1 until p.crests) {
        acc += gaps[i - 1]
        out[i] = acc / total
    }
    out
}

private const val LOBES = 4 // the maximum scallops per crest. twice this number at the front

private fun buildCrests(): List<Crest> = (0 until p.crests).map { i ->
    val s = crestS[i]
    // a rough band thickness out on the flank. it sets the size of the scallops
    val probe = peakX + pw * 0.32f
    val thick = (surface(probe, min(1f, s + layer)) - surface(probe, s)).coerceAtLeast(4f)
    val n = rnd.between(0, LOBES + (s * LOBES).toInt())
    Crest(
        s,
        (0 until n).map {
            Lobe(panel.left + rnd.between(-0.06f, 1.06f) * pw, pw * rnd.between(0.04f, 0.15f), thick * rnd.between(0.15f, 0.55f))
        },
    )
}

// ---- the cut ----

// the run-in and run-out of the gouge. it is a fixed distance, not a fraction. when the tool
// enters the block, it does not know how long the stroke will be
private const val TAPER = 14f

private fun env(i: Int, n: Int): Float {
    val a = i * STEP
    val b = (n - 1 - i) * STEP
    return (min(a, b) / TAPER).coerceIn(0.05f, 1f)
}

// the strip of clean paper under a crest, the light on the back of the wave. it gets wider where
// the swell rises, and this wider strip is the only reason that the peak looks like a snowcap
private fun shoulder(x: Float, s: Float) = p.gap * (1f + p.crown * ridge01(x, s).pow(1.6f))

// how dirty the water is at a point. the blotches are slow, so whole areas of sea go dark
private const val BLOT = 0.22f

private fun blotch(x: Float, y: Float): Float =
    1f + BLOT * fbm(x * 0.0013f + 61f + nzoff, y * 0.0013f, octaves = 2, lacunarity = 2.5f, gain = 0.5f)

private const val TONE0 = 0.72f // tone at the top of a band, relative to its trough
private const val CHATTER = 0.022f // the scale of the break-up noise. a smaller value makes larger patches

/**
 * one cut inside a band, [v] of the way down the band.
 *
 * the cut goes silent in the lit shoulder under the crest above. it also goes silent too close
 * to the crest below, and closer than [Params.pitch] to the last cut that we actually drew. the
 * last test keeps the tone even everywhere. near the peak the band opens wide and every cut
 * survives. out on the flanks the band closes and cuts disappear. this looks like a cleared
 * part of the block.
 */
private fun Canvas.gouge(band: Band, v: Float, ink: Paint) {
    val s = lerp(band.a.s, band.b.s, v)
    val rib = Ribbon()

    for (i in 0 until cols) {
        val x = lattice[i]
        // a hair of jitter per cut. without it the cuts stay exactly parallel at exactly the pitch,
        // and the whole picture makes moire where two bands run close together
        val jitter = fbm(x * 0.005f + 401f, v * 37f + s * 3f, octaves = 1) * p.pitch * 0.5f
        // the roll is interpolated between the two crests that bound the band, not sampled fresh.
        // the roll is worth about half a band. a cut that samples its own roll drifts out of its
        // band, and the cull removes it in large smooth areas. with the interpolated roll, the cut
        // lands exactly on the crest above at v=0 and on the crest below at v=1
        val y = rolled(x, s, lerp(band.a.rs[i], band.b.rs[i], v)) + jitter

        val hi = band.a.ys[i]
        val lo = band.b.ys[i]

        var on = y - hi > shoulder(x, s) && lo - y > p.pitch * 0.3f && y - band.last[i] >= p.pitch * 0.62f

        // tone. the trough of a band is darker than its crest, multiplied by the slow blotches
        val tone = (lerp(TONE0, 1f, v) * blotch(x, y) * (1f - p.crown * 0.22f * ridge01(x, s)))
            .coerceIn(0.05f, 1.5f)

        if (on) {
            // the break-up. the noise samples in space, stretched flat along the cut, so the gaps
            // group into patches the way a worn block does. if the noise keys to depth instead, the
            // gaps align between neighboring cuts, and the whole picture gets vertical stripes
            val nn = fbm(x * CHATTER + 211f + nzoff, y * CHATTER * 3.5f, octaves = 2, lacunarity = 2.2f, gain = 0.45f) * 1.1f + 0.5f
            on = nn < p.bite * (0.42f + 0.78f * tone)
        }

        if (on) {
            rib.add(x, y, min(p.nib * (0.25f + 1.2f * tone), p.pitch * 0.92f))
            band.last[i] = y
        } else {
            rib.flush(this, ink)
        }
    }
    rib.flush(this, ink)
}

// a cut stroke: the centerline plus a width at every sample, filled as a ribbon. the ribbon can
// get wider in the middle and end in a point at both ends, the way a gouge enters and leaves the block
private class Ribbon {
    private val xs = FloatArray(cols)
    private val ys = FloatArray(cols)
    private val ws = FloatArray(cols)
    private var k = 0

    val isEmpty get() = k == 0

    fun add(x: Float, y: Float, w: Float) {
        xs[k] = x
        ys[k] = y
        ws[k] = w
        k++
    }

    // draws the stroke so far and starts a new one
    fun flush(c: Canvas, ink: Paint) {
        if (k >= 3) { // a shorter stroke is all taper and looks like a smudge
            val hw = FloatArray(k) { ws[it] * env(it, k) * 0.5f }
            c.drawPath(outlineOf(xs, ys, hw, k), ink)
        }
        k = 0
    }
}

// one band between two crests. last holds the y of the last cut drawn in each column, for the
// crowding test in gouge()
private class Band(val a: Crest, val b: Crest) {
    val last = a.ys.copyOf()
    val thick = max(0f, (0 until cols).maxOf { b.ys[it] - a.ys[it] })
}

// every cut in one band, top to bottom
private fun Canvas.hatchBand(bd: Band, ink: Paint) {
    // the count comes from the thickest part of the band, so the peak never goes bald. everywhere
    // else the crowding test in gouge() thins the cuts again
    val lines = ((bd.thick - p.gap) / p.pitch).roundToInt().coerceIn(1, 120)
    for (j in 0 until lines) gouge(bd, (j + 0.5f) / lines, ink)
}

// the lit shoulder as a solid shape. the scalloped crest bounds it on top, so the lobes show white
private fun Canvas.lightBand(a: Crest, light: Paint) {
    val pb = PathBuilder()
    for (i in 0 until cols) pb.lineOrMove(i == 0, lattice[i], a.ys[i])
    for (i in cols - 1 downTo 0) pb.lineTo(lattice[i], a.ys[i] + shoulder(lattice[i], a.s))
    pb.closePath()
    drawPath(pb.detach(), light)
}

// the key block: the one heavy line per crest. it gets thinner toward the horizon
private fun Canvas.keyLine(cr: Crest, ink: Paint) {
    val rib = Ribbon()
    for (i in 0 until cols) {
        val x = lattice[i]
        val nn = fbm(x * CHATTER * 0.6f + 907f + nzoff, cr.s * 90f, octaves = 2, lacunarity = 2.5f, gain = 0.5f) * 1.1f + 0.5f
        // a key line mostly holds, but a block does lose it in some places. a break right after a
        // break does not count, that sample goes into the new stroke
        if (nn > 0.96f && !rib.isEmpty) {
            rib.flush(this, ink)
            continue
        }
        rib.add(x, cr.ys[i], p.key * (0.55f + 0.65f * cr.s) * (0.6f + 0.9f * nn) * blotch(x, cr.ys[i]))
    }
    rib.flush(this, ink)
}

// foam ///////////////

private const val CELLS = 9 // light cells per patch, approximately
private const val FOAM = 7 // patches of loose foam out on the open water

// a wobbly circle. nothing in a cut print is round
private fun blob(cx: Float, cy: Float, r: Float): Path {
    val n = 9
    val pb = PathBuilder()
    for (i in 0 until n) {
        val a = TAUf * i / n
        val rr = r * rnd.between(0.7f, 1.3f)
        pb.lineOrMove(i == 0, cx + cos(a) * rr, cy + sin(a) * rr * 0.8f)
    }
    pb.closePath()
    return pb.detach()
}

// foam: light cells punched into the hatching. the cells are close together, so the ink that
// remains between them becomes a net. there are no outlines, because the net is the outline.
// then the dirt that collects in the corners of the net
private fun Canvas.reticule(cx: Float, cy: Float, rx: Float, ry: Float, cells: Int, light: Paint, ink: Paint) {
    repeat(cells) {
        val (a, rr) = disc()
        drawPath(blob(cx + cos(a) * rx * rr, cy + sin(a) * ry * rr, rnd.between(0.1f, 0.24f) * min(rx, ry) + 1.6f), light)
    }
    repeat(cells) {
        val (a, rr) = disc()
        drawCircle(cx + cos(a) * rx * rr * 1.2f, cy + sin(a) * ry * rr * 1.2f, rnd.between(0.5f, 1.5f), ink)
    }
}

// a random point in the unit disc, as an angle and a radius. the sqrt keeps the points even
// instead of crowded in the middle
private fun disc() = rnd.between(0f, TAUf) to sqrt(rnd.nextFloat())

// foam sits where a scallop lifted the crest. this lift is the whole purpose of the scallop
private fun Canvas.foamOn(cr: Crest, light: Paint, ink: Paint) {
    for (l in cr.lobes) {
        if (l.h < p.gap * 2.2f) continue
        val y = cr.y(l.x) + l.h * 0.5f + p.gap * 0.6f
        reticule(l.x, y, l.hw * 0.6f, l.h * 0.5f + 3f, rnd.between(CELLS / 2, CELLS), light, ink)
    }
}

// and some loose foam out on the open water, mostly near the peak
private fun Canvas.spray(crests: List<Crest>, light: Paint, ink: Paint) {
    repeat(FOAM) {
        val x = peakX + (rnd.nextFloat() + rnd.nextFloat() - 1f) * pw * 0.62f
        val cr = crests[(rnd.nextFloat().pow(1.5f) * (crests.size - 1)).toInt()]
        val r = pw * rnd.between(0.012f, 0.038f)
        reticule(x, cr.y(x) + rnd.between(1f, 3.5f) * p.gap, r, r * rnd.between(0.35f, 0.7f), rnd.between(CELLS, CELLS * 2), light, ink)
    }
}

// furniture: sky, snowcap, frame -------------

// everything above the first crest, cut along the crest
private fun skyPath(first: Crest): Path {
    val pb = PathBuilder().moveTo(x0, panel.top - 12f).lineTo(x1, panel.top - 12f)
    for (i in cols - 1 downTo 0) pb.lineTo(lattice[i], first.ys[i])
    pb.closePath()
    return pb.detach()
}

// the sky is bokashi: one graded wash from the top edge. the printer wipes it onto the block and
// does not cut it. the wash fades before it gets to the water. skyfall is how fast the wash
// fades. a low value keeps color most of the way down. a high value keeps it near the top edge.
//
// then a dot screen goes over the wash, so the grade looks printed and not airbrushed. the
// printer wipes the wash by hand, so the line where it ends moves across the page. you read the
// fade from the screen, so it is enough to move only the screen
private fun Canvas.drawSky(sky: Path) {
    val wash = Bokashi(pal.sky, panel.top, sky.bounds.bottom, p.sky, p.skyfall)
    drawBokashi(sky, wash)
    drawBokashiScreen(sky, wash, fillOf(pal.sky), p.rpitch, RASTER_ANGLE, p.raster, Point(peakX, panel.top)) { x ->
        1f + fbm(x * 0.0016f + 811f + nzoff, 2f, octaves = 2, lacunarity = 2.2f, gain = 0.5f) * 0.3f
    }
}

// the speckle on the snowcap. the peak is the one part of the sea that is mostly paper
private const val STIPPLE = 320

private fun Canvas.crownStipple(crests: List<Crest>, ink: Paint) {
    repeat(STIPPLE) {
        // approximately gaussian around the peak, with a bias to the crests near the top
        val x = peakX + (rnd.nextFloat() + rnd.nextFloat() + rnd.nextFloat() - 1.5f) * pw * 0.34f
        val i = (rnd.nextFloat().pow(2.2f) * (crests.size - 1)).toInt()
        val cr = crests[i]
        val y = cr.y(x) + rnd.between(0.15f, 1.0f) * shoulder(x, cr.s)
        drawCircle(x, y, rnd.between(0.6f, 1.9f), ink)
    }
}

// the edge of the block: a heavy rule, plus the thin rule that always sits just inside it
private fun Canvas.frame(d: Dimension, paper: Paint) {
    // the paper outside the block stays clean, so we wipe it before we draw the edge rule. this is
    // four fills, not drawBorder. a stroked border lands one level off on the inner seam at
    // fractional margins
    val m = MARGIN
    drawRect(Rect.makeLTRB(0f, 0f, d.wf, m), paper)
    drawRect(Rect.makeLTRB(0f, d.hf - m, d.wf, d.hf), paper)
    drawRect(Rect.makeLTRB(0f, 0f, m, d.hf), paper)
    drawRect(Rect.makeLTRB(d.wf - m, 0f, d.wf, d.hf), paper)

    drawRect(panel, strokeOf(p.key * 1.3f, pal.ink))
    drawRect(panel.shrink(5f), strokeOf(0.9f, pal.ink).alpha(90))
}

// the print ======

private fun draw(c: Canvas, d: Dimension) {
    c.clear(pal.paper)

    val ink = fillOf(pal.ink)
    val light = fillOf(pal.light)
    val paper = fillOf(pal.paper)

    val crests = buildCrests()

    c.save()
    c.clipRect(panel)

    val sky = skyPath(crests.first())
    c.drawSky(sky)

    // 1. the tone. every band gets its hatch first. the other layers then go on top of it
    for (i in 0 until crests.size - 1) c.hatchBand(Band(crests[i], crests[i + 1]), ink)

    // 2. the light. the lit shoulder goes on top. where a scallop lifted the crest, the shoulder cuts
    //    into the band above. this is exactly what foam does to the water behind it
    for (i in 0 until crests.size - 1) c.lightBand(crests[i], light)

    // 3. the key block. it goes last, so nothing draws over it
    for (cr in crests) c.keyLine(cr, ink)

    for (cr in crests) c.foamOn(cr, light, ink)
    c.spray(crests, light, ink)
    c.crownStipple(crests, ink)

    c.restore()

    c.frame(d, paper)
}

// the paper: a slow warm stain, dirt at the edges, then film grain over all of it. the grain puts
// the ink under the same skin as everything else, so the ink does not look wet
private fun patina(g: Gartvas) {
    if (p.mottle > 0f || p.age > 0f) stain(g)
    if (p.grain > 0f) addGrain(g, p.grain, p.seed.toInt())
}

private fun stain(g: Gartvas) {
    val m = Gartmap(g)
    val px = m.pixels
    val sr = red(pal.stain).toFloat()
    val sg = green(pal.stain).toFloat()
    val sb = blue(pal.stain).toFloat()
    val edge = W * 0.36f

    for (i in px.indices) {
        val x = i % W
        val y = i / W

        val n = fbm(x * 0.0018f + nzoff, y * 0.0018f, octaves = 3, lacunarity = 2.4f, gain = 0.5f)
        val e = (1f - min(min(x, W - 1 - x), min(y, H - 1 - y)) / edge).coerceAtLeast(0f)
        val t = ((n * 0.5f + 0.5f) * p.mottle + e.pow(2.4f) * p.age).coerceIn(0f, 1f)

        val col = px[i]
        px[i] = rgb(
            mix(red(col).toFloat(), sr, t).toInt().coerceIn(0, 255),
            mix(green(col).toFloat(), sg, t).toInt().coerceIn(0, 255),
            mix(blue(col).toFloat(), sb, t).toInt().coerceIn(0, 255),
        )
    }
    m.drawToCanvas(g)
}

// knobs -----------------------------------------

private fun resolveParams() = Params(
    seed = pl("seed", 9L),
    out = ps("out", "unda"),
    pal = pi("pal", 0, 0..2), // 0 ai, 1 sumi, 2 beni

    height = pf("height", 0.22f, 0f..0.7f),
    // the two shoulders. a wide shoulder makes long lazy sweeps off the edge. a tight shoulder stacks
    // the crests. the two must not be equal. a symmetrical swell looks like a mountain, not like water
    spreadl = pf("spreadl", 0.42f, 0.02f..2f),
    spreadr = pf("spreadr", 0.17f, 0.02f..2f),

    crests = pi("crests", 28, 4..80),
    fall = pf("fall", 1.9f, 0.4f..4f), // perspective. 1 stacks the bands evenly. 2.6 gives the foreground to three huge waves
    decay = pf("decay", 1.35f, 0f..6f), // how fast the peak loses effect as it comes near. near 0 the picture is one giant chevron

    roll = pf("roll", 0.85f, 0f..2f), // 0 is a machined ripple tank
    uneven = pf("uneven", 0.5f, 0f..0.95f), // jitter in the band spacing. 0 is a contour map
    wander = pf("wander", 0.1f, 0f..0.5f), // 0 puts every peak on the same x, and you get a seam

    // the cut. pitch and nib together are the whole tonal range
    pitch = pf("pitch", 4.6f, 2f..40f), // the gouge spacing, px. at less than about 3.5 with a fat nib, the water fills in solid
    nib = pf("nib", 2.2f, 0.2f..12f),
    bite = pf("bite", 1.08f, 0f..1.4f), // ink survival. at more than 1 the cuts run unbroken. at less than about 0.8 the block looks worn through

    gap = pf("gap", 7f, 0f..60f), // the paper left under each key line, px. it is the only white in the water
    key = pf("key", 4.2f, 0.2f..20f),
    crown = pf("crown", 1.9f, 0f..12f), // makes the gap wider where the swell rises. this gap is the snowcap

    sky = pf("sky", 0.32f, 0f..1f),
    skyfall = pf("skyfall", 1.7f, 0.2f..8f), // how fast the wash fades from the top edge
    raster = pf("raster", 0.46f, 0f..1.4f), // the dot screen over the wash. 0 leaves the wash smooth
    rpitch = pf("rpitch", 6f, 2f..60f), // the screen ruling, px. 3 is too fine to see. 14 is pop art

    mottle = pf("mottle", 0.11f, 0f..1f), // the stain that blooms through the sheet
    age = pf("age", 0.16f, 0f..1f), // the dirt that collects at the edges
    grain = pf("grain", 0.07f, 0f..1f),
)
