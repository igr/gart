package fimbria

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.colorScale
import dev.oblac.gart.color.darken
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.lighten
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.pl
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.f
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.noise.fbm
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Shader
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * fimbria - a fringe hung on zigzags.
 *
 * Artist is Outi Pieski, the piece is "Beavvit II / Rising Together II (2021)" in Moderna Museet.
 *
 * after a yarn installation I photographed: long strands folded over thin rods, the rods bent
 * into zigzags and stacked in tiers, so every rod reads as a run of diagonals - chevrons of
 * hanging thread, one tier over the next. the strands are only ever vertical; the diagonals
 * are the rods.
 *
 * its a layered image and nothing more. every rod is a zigzag on the page, every strand a
 * vertical line hung off it, and each rod picks a layer to hang in. paint back to front. the
 * back layers go dim and the black shows through the gaps between strands, which is what
 * makes it hang instead of sit.
 *
 * the strands are glass. they're see-through and screen onto each other, so a curtain in front
 * tints the one behind and three colours of rod are plenty - the pinks and oranges are just
 * ruby over amber. glass=0 blend=over pal=2 is the yarn it started as.
 */
private const val W = 1200
private const val H = 1200

private data class Params(
    val seed: Long,
    val out: String,
    val pal: Int,
    val ss: Int,
    // the stack
    val tiers: Int, val rows: Int,
    val top: Float, val bottom: Float,
    val reach: Float, val belly: Float, val waist: Float, val scatter: Float,
    val zig: Float, val pitch: Float, val stagger: Float,
    val drop: Float, val ragged: Float,
    // the yarn
    val gap: Float, val ply: Int, val w: Float,
    val sway: Float, val taper: Float,
    val run: Float, val mood: Float, val jitter: Float,
    val fold: Float, val tail: Float,
    // glass
    val glass: Float, val blend: String, val shine: Float,
    // light
    val fog: Float, val lit: Float,
    val rods: Float, val wall: Float,
)

private val p = resolveParams()
private val rnd = Random(p.seed)

// simplex has no seed of its own, so shift the sample window instead
private val nzoff = (p.seed and 0xffff) * 0.01f

// the yarn box /////////

// colours and how often each one gets picked up. the weights are the whole character of a
// box - the photo is nine parts red and orange to one part everything else
private class Box(val cols: Palette, wts: FloatArray) {
    private val cum = FloatArray(wts.size).also { c -> var s = 0f; for (i in wts.indices) { s += wts[i]; c[i] = s } }
    fun pick(r: Float): Int { val t = r * cum.last(); for (i in cum.indices) if (t < cum[i]) return i; return cum.size - 1 }
}

// the skeins of boxes 1..4 moved to cool.kt as 178..181, the weights stayed here. they go by
// index, so the order has to match on both ends
private val boxes = arrayOf(
    // warm glass: ruby and amber, cobalt for the odd rod. everything else is one over another
    Box(Palette(0xFFE0203C, 0xFFF5A81C, 0xFF2A55D6), floatArrayOf(4f, 3f, 1.5f)),
    // jewel glass: four rods and thats it
    Box(Palettes.cool178, floatArrayOf(3f, 2.5f, 2.5f, 1f)),
    // the yarn in the photo. the cool ones are the odd skein someone ran out of
    Box(Palettes.cool179, floatArrayOf(5f, 4f, 3f, 2.5f, 1f, 2f, 1.2f, 1.5f, 2f, 1f, 1f, 0.8f, 0.7f, 0.5f, 0.4f, 0.4f)),
    // night: the same box after dark. indigo, teal, violet, the odd ember
    Box(Palettes.cool180, floatArrayOf(4f, 4f, 3f, 2.5f, 1.5f, 2.5f, 1.5f, 1f, 1.5f, 0.8f, 0.6f, 0.6f)),
    // ash: bone and grey and one red thread
    Box(Palettes.cool181, floatArrayOf(4f, 3f, 3f, 2.5f, 2f, 1f, 0.35f, 0.5f)),
)

private val box = boxes[p.pal % boxes.size]

// how two strands combine where they cross. over is paint, screen is light through glass,
// plus is the same but it can burn out to white
private val mix = when (p.blend) {
    "screen" -> BlendMode.SCREEN
    "plus" -> BlendMode.PLUS
    else -> BlendMode.SRC_OVER
}

// ---- the stack ----

private val yTop = H * p.top
private val yBot = H * p.bottom
private val dy = (yBot - yTop) / (p.tiers - 1 + p.drop)
private val drop = dy * p.drop

// one bent rod, as its vertices on the page, and the layer it hangs in - 0 is the front.
// y wobbles a touch, nothing hangs dead level
private class Rod(val tier: Int, val layer: Float, val x: FloatArray, val y: FloatArray)

private val rods = buildList {
    for (i in 0 until p.tiers) {
        val t = if (p.tiers > 1) i / (p.tiers - 1f) else 0f
        // the silhouette: a diamond, widest at the belly, pinched to waist at both ends
        val d = abs(t - p.belly) / max(p.belly, 1f - p.belly).coerceAtLeast(0.01f)
        val prof = 1f - (1f - p.waist) * d.pow(1.15f)
        val hw = W * p.reach * prof * (0.88f + 0.24f * rnd.nextFloat())
        val xc = W / 2f + (rnd.nextFloat() * 2f - 1f) * p.scatter * W
        val y = yTop + i * dy
        for (r in 0 until p.rows) {
            // the rows of a tier hang a bit apart, and each picks its own layer - so a tier can
            // sit in front of the one above it or behind. thats all the depth there is
            val yr = y + (r - (p.rows - 1) * 0.5f) * dy * p.stagger + (rnd.nextFloat() - 0.5f) * dy * 0.2f
            val layer = rnd.nextFloat()
            val flip = if (rnd.nextBoolean()) 1f else -1f
            val n = max(2, (2f * hw / p.pitch).roundToInt() + 1)
            val x0 = xc - (n - 1) * p.pitch * 0.5f
            val xs = FloatArray(n)
            val ys = FloatArray(n)
            for (v in 0 until n) {
                xs[v] = x0 + v * p.pitch + (rnd.nextFloat() - 0.5f) * p.pitch * 0.12f
                ys[v] = yr + p.zig * flip * (if (v % 2 == 0) 1f else -1f) + (rnd.nextFloat() - 0.5f) * p.zig * 0.16f
            }
            add(Rod(i, layer, xs, ys))
        }
    }
}

// everything that gets painted goes in one list so it can be sorted by layer. a strand is
// vertical so one x does it; the rods are the only thing with two
private sealed class El(val layer: Float)
private class Yarn(layer: Float, val x: Float, val y0: Float, val y1: Float, val w: Float, val col: Int, val hi: Int, val lo: Int, val sway: Float) : El(layer)
private class Line(layer: Float, val x0: Float, val y0: Float, val x1: Float, val y1: Float, val w: Float, val col: Int) : El(layer)

private fun alphaOf(c: Int, a: Float) = ((a.coerceIn(0f, 1f) * 255f).roundToInt() shl 24) or (c and 0xFFFFFF)

// the hang =================================

// walks every rod and hangs the yarn off it. single threaded, all the rng is in here
private fun hang(): List<El> {
    val els = ArrayList<El>()

    var mood = 0
    var lastTier = -1
    for (rod in rods) {
        // each tier favours one skein - thats what makes an orange tier, a purple tier
        if (rod.tier != lastTier) { mood = box.pick(rnd.nextFloat()); lastTier = rod.tier }
        // the back layers go dark - its most of the depth in the picture - and the lower tiers
        // get a bit less light
        val tone0 = lerp(1f, p.fog, rod.layer) * (1f - p.lit * rod.tier / (p.tiers - 1f).coerceAtLeast(1f))

        // the rod itself, bend to bend
        if (p.rods > 0f) for (v in 1 until rod.x.size) {
            els.add(Line(rod.layer, rod.x[v - 1], rod.y[v - 1], rod.x[v], rod.y[v], 1.3f, alphaOf(0x2E2723, p.rods)))
        }

        // the yarn, in runs of one colour along the rod
        var left = 0
        var col = 0
        var runLen = 1f
        for (v in 1 until rod.x.size) {
            val ax = rod.x[v - 1]; val ay = rod.y[v - 1]
            val bx = rod.x[v]; val by = rod.y[v]
            val n = max(1, (hypot(bx - ax, by - ay) / p.gap).roundToInt())
            for (j in 0 until n) {
                if (left <= 0) {
                    // a new run. mostly the tiers own colour, sometimes anything in the box
                    val i = if (rnd.nextFloat() < p.mood) mood else box.pick(rnd.nextFloat())
                    col = box.cols[i]
                    left = max(1, (p.run * (0.35f + 1.3f * rnd.nextFloat())).roundToInt())
                    // each bundle was cut on its own, so runs differ in length more than strands do
                    runLen = 1f + (rnd.nextFloat() - 0.5f) * p.ragged
                }
                left--
                val u = (j + 0.5f) / n
                val x = lerp(ax, bx, u)
                val y = lerp(ay, by, u)
                for (q in 0 until p.ply) {
                    val xx = x + (rnd.nextFloat() - 0.5f) * p.gap * 0.7f
                    val l = drop * runLen * (1f + (rnd.nextFloat() - 0.5f) * p.ragged)
                    val tone = tone0 * (1f + (rnd.nextFloat() - 0.5f) * 2f * p.jitter)
                    val c = colorScale(col, tone)
                    // neighbours lean together, thats what makes it read as one curtain and not a comb
                    val sw = p.sway * (
                        2f * fbm(x * 0.006f + nzoff, rod.tier * 3.1f + rod.layer * 5f, octaves = 2, lacunarity = 2.1f, gain = 0.5f) +
                            (rnd.nextFloat() - 0.5f) * 0.6f
                        )
                    els.add(Yarn(rod.layer, xx, y, y + l, p.w, c, lighten(c, p.fold), darken(c, p.tail), sw))
                }
            }
        }
    }
    return els
}

// the paint =================================

private const val SEGS = 8

// the outline of one strand: a sliver that leans a hair at the tail and thins to a tip. scale
// and shift are for the shine, which is the same shape drawn narrower and off centre
private fun sliver(s: Yarn, len: Float, scale: Float, shift: Float): Path {
    val pb = PathBuilder()
    for (i in 0..SEGS) {
        val t = i / SEGS.toFloat()
        val x = s.x + shift + s.sway * t * t
        val hw = s.w * scale * 0.5f * (1f - p.taper * smoothstep(0.7f, 1f, t))
        if (i == 0) pb.moveTo(x - hw, s.y0) else pb.lineTo(x - hw, s.y0 + len * t)
    }
    for (i in SEGS downTo 0) {
        val t = i / SEGS.f()
        val x = s.x + shift + s.sway * t * t
        val hw = s.w * scale * 0.5f * (1f - p.taper * smoothstep(0.7f, 1f, t))
        pb.lineTo(x + hw, s.y0 + len * t)
    }
    return pb.closePath().detach()
}

// one strand, filled top to bottom with the bright fold over the rod, the body, then a dimmer
// tail. and if its glass, a shine down one side - a rod is round, and thats the whole tell
private fun yarn(c: Canvas, s: Yarn, paint: Paint, shine: Paint) {
    val len = s.y1 - s.y0
    if (len <= 0.5f) return
    val path = sliver(s, len, 1f, 0f)
    // the fold is a few px, not a fraction - a long strand doesnt get a longer highlight
    val ft = (3f / len).coerceIn(0.01f, 0.25f)
    val sh = Shader.makeLinearGradient(
        0f, s.y0, 0f, s.y1,
        gradientOf(intArrayOf(s.hi, s.col, s.col, s.lo), floatArrayOf(0f, ft, 0.72f, 1f)),
    )
    paint.shader = sh
    c.drawPath(path, paint)
    path.close()
    sh.close()
    if (p.shine > 0f) {
        // a third of the width, a fifth off centre. the light is up and to the left, say
        val core = sliver(s, len, 0.32f, -s.w * 0.2f)
        shine.color = alphaOf(lighten(s.col, 0.7f), p.shine)
        c.drawPath(core, shine)
        core.close()
    }
}

private fun paint(c: Canvas, els: List<El>) {
    // the wall. black, with a faint spot where a light would land - a room, not a void
    c.clear(0xFF050406.toInt())
    if (p.wall > 0f) {
        val spot = lerpColor(0xFF050406.toInt(), 0xFF2A221C.toInt(), p.wall)
        c.drawPaint(
            Paint().apply {
                shader = Shader.makeRadialGradient(
                    W * 0.5f, H * 0.42f, W * 0.85f,
                    gradientOf(intArrayOf(spot, 0xFF050406.toInt()), floatArrayOf(0f, 1f)),
                )
            },
        )
    }

    val fill = paint().apply { blendMode = mix; setAlphaf(1f - p.glass) }
    val shine = paint().apply { blendMode = mix }
    val stroke = paint().apply { mode = PaintMode.STROKE; strokeCap = PaintStrokeCap.ROUND }
    for (e in els) when (e) {
        is Line -> { stroke.color = e.col; stroke.strokeWidth = e.w; c.drawLine(e.x0, e.y0, e.x1, e.y1, stroke) }
        is Yarn -> yarn(c, e, fill, shine)
    }
}

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("fimbria", W, H)

    println(gart)
    println(
        "seed=${p.seed} pal=${p.pal} tiers=${p.tiers} rows=${p.rows} zig=${p.zig} pitch=${p.pitch} " +
            "gap=${p.gap} ply=${p.ply} drop=${p.drop} fog=${p.fog}",
    )

    // back first
    val els = hang().sortedByDescending { it.layer }
    println("${els.size} things to hang")

    val big = Gartvas(Dimension(W * p.ss, H * p.ss))
    big.canvas.scale(p.ss.toFloat(), p.ss.toFloat())
    paint(big.canvas, els)

    val g = gart.gartvas()
    val snap = big.snapshot()
    g.canvas.drawImageRect(
        snap,
        Rect.makeWH((W * p.ss).toFloat(), (H * p.ss).toFloat()),
        Rect.makeWH(W.toFloat(), H.toFloat()),
        SamplingMode.MITCHELL,
        null,
        true,
    )
    snap.close()

    val output = p.out.ensureExtension("png")
    gart.saveImage(g, output)

    if (!headless) gart.window().showImage(g)
}

// plumbing

private fun resolveParams(): Params {
    val p = Params(
        seed = pl("seed", 4L),
        out = ps("out", "fimbria"),
        pal = pi("pal", 5),
        ss = pi("ss", 2),
        tiers = pi("tiers", 8),
        rows = pi("rows", 2),
        top = pf("top", 0.13f),
        bottom = pf("bottom", 0.89f),
        reach = pf("reach", 0.42f), // half width of the widest tier, fraction of W
        belly = pf("belly", 0.45f), // where down the stack the widest tier is
        waist = pf("waist", 0.32f), // how wide the ends are next to the belly. 1 is a box
        scatter = pf("scatter", 0.05f),
        zig = pf("zig", 50f), // half height of a bend, px. with pitch its the angle of the chevrons
        pitch = pf("pitch", 150f), // x run between bends
        stagger = pf("stagger", 0.4f), // rows of one tier sit this far apart, in tier spacings
        drop = pf("drop", 1.5f), // strand length in tier spacings. under 1 the tiers dont overlap
        ragged = pf("ragged", 0.14f),
        gap = pf("gap", 3.5f), // strand spacing along the rod, px
        ply = pi("ply", 1), // strands per spot on the rod
        w = pf("w", 4f), // rod width in px. under ~2 it reads as thread
        sway = pf("sway", 2.5f),
        taper = pf("taper", 0.6f),
        run = pf("run", 9f), // mean length of a colour run, in strands
        mood = pf("mood", 0.55f), // how much of a tier is its own colour
        jitter = pf("jitter", 0.08f),
        fold = pf("fold", 0.45f),
        tail = pf("tail", 0.3f),
        glass = pf("glass", 0.35f), // how see-through a strand is. 0 is yarn
        blend = ps("blend", "screen"),
        shine = pf("shine", 0f), // the highlight down each rod. 0 is matte
        fog = pf("fog", 0.35f), // brightness at the very back
        lit = pf("lit", 0.12f), // and a bit less light on the lower tiers
        rods = pf("rods", 0.5f),
        wall = pf("wall", 0.5f),
    )

    require(p.pal >= 0) { "pal must be >= 0" }
    require(p.ss in 1..4) { "ss must be 1..4" }
    require(p.tiers in 1..40) { "tiers must be between 1 and 40" }
    require(p.rows in 1..6) { "rows must be between 1 and 6" }
    require(p.top in -0.5f..1f) { "top must be between -0.5 and 1" }
    require(p.bottom in 0f..1.5f && p.bottom > p.top) { "bottom must be between 0 and 1.5 and below top" }
    require(p.reach in 0.05f..1f) { "reach must be between 0.05 and 1" }
    require(p.belly in 0f..1f) { "belly must be between 0 and 1" }
    require(p.waist in 0f..1f) { "waist must be between 0 and 1" }
    require(p.scatter in 0f..0.5f) { "scatter must be between 0 and 0.5" }
    require(p.zig in 0f..400f) { "zig must be between 0 and 400" }
    require(p.pitch in 10f..1200f) { "pitch must be between 10 and 1200" }
    require(p.stagger in 0f..3f) { "stagger must be between 0 and 3" }
    require(p.drop in 0.1f..6f) { "drop must be between 0.1 and 6" }
    require(p.ragged in 0f..1f) { "ragged must be between 0 and 1" }
    require(p.gap in 0.5f..40f) { "gap must be between 0.5 and 40" }
    require(p.ply in 1..6) { "ply must be between 1 and 6" }
    require(p.w in 0.3f..12f) { "w must be between 0.3 and 12" }
    require(p.sway in 0f..40f) { "sway must be between 0 and 40" }
    require(p.taper in 0f..1f) { "taper must be between 0 and 1" }
    require(p.run in 1f..200f) { "run must be between 1 and 200" }
    require(p.mood in 0f..1f) { "mood must be between 0 and 1" }
    require(p.jitter in 0f..0.5f) { "jitter must be between 0 and 0.5" }
    require(p.fold in 0f..1f) { "fold must be between 0 and 1" }
    require(p.tail in 0f..1f) { "tail must be between 0 and 1" }
    require(p.glass in 0f..1f) { "glass must be between 0 and 1" }
    require(p.blend in setOf("over", "screen", "plus")) { "blend must be over, screen or plus" }
    require(p.shine in 0f..1f) { "shine must be between 0 and 1" }
    require(p.fog in 0f..1f) { "fog must be between 0 and 1" }
    require(p.lit in 0f..1f) { "lit must be between 0 and 1" }
    require(p.rods in 0f..1f) { "rods must be between 0 and 1" }
    require(p.wall in 0f..1f) { "wall must be between 0 and 1" }
    return p
}
