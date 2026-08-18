package turgor

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.darken
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.color.hueShift
import dev.oblac.gart.color.lighten
import dev.oblac.gart.color.space.ColorOKLCH
import dev.oblac.gart.color.space.color4f
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.pl
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.between
import dev.oblac.gart.math.cosDeg
import dev.oblac.gart.math.sinDeg
import dev.oblac.gart.math.toRadians
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.util.farr
import dev.oblac.gart.util.iarr
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.PathEffect
import org.jetbrains.skia.PathFillMode
import org.jetbrains.skia.PathMeasure
import org.jetbrains.skia.PathOp
import org.jetbrains.skia.PathUtils
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Shader
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.random.Random

private const val W = 1620
private const val H = 1080

/** glyphs are built at this size, then the whole piece is scaled to fit */
private const val FONT_SIZE = 420f

private data class Params(
    val seed: Long,
    val text: String,
    val out: String,
    val ss: Int,
    val fill: Float,
    // attitude
    val lean: Float,
    val leanVary: Float,
    val bounce: Float,
    val spin: Float,
    val squash: Float,
    val overlap: Float,
    val drag: Float,
    // warp
    val swell: Float,
    val swellFreq: Float,
    val wobble: Float,
    val wobbleFreq: Float,
    val barbs: Int,
    val barbLen: Float,
    // mass
    val fatten: Float,
    val counter: Float,
    val outline: Float,
    val shine: Float,
    val shineWidth: Float,
    val shineGap: Float,
    // depth
    val extrude: Float,
    val extrudeAngle: Float,
    // halo
    val halo: Float,
    val haloLobe: Float,
    val haloSpots: Int,
    val haloDots: Int,
    val haloMottle: Float,
    val haloMist: Float,
    val haloRough: Float,
    val haloSoft: Float,
    // finish
    val streaks: Float,
    val streakAngle: Float,
    val hue: Float,
    val grain: Float,
    val vignette: Float,
)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val p = resolveParams()
    val gart = Gart.of("turgor", W, H)

    println(gart)
    println(
        "seed=${p.seed} text=${p.text} lean=${p.lean} overlap=${p.overlap} fatten=${p.fatten} " + "swell=${p.swell} wobble=${p.wobble} barbs=${p.barbs} extrude=${p.extrude} hue=${p.hue}",
    )

    val big = Gartvas(Dimension(W * p.ss, H * p.ss))
    big.canvas.scale(p.ss.toFloat(), p.ss.toFloat())
    render(big.canvas, p)

    val g = gart.gartvas()
    val snapshot = big.snapshot()
    g.canvas.drawImageRect(
        snapshot,
        Rect.makeWH((W * p.ss).toFloat(), (H * p.ss).toFloat()),
        Rect.makeWH(W.toFloat(), H.toFloat()),
        SamplingMode.MITCHELL,
        null,
        true,
    )
    snapshot.close()

    if (p.vignette > 0f) g.canvas.drawVignette(g.d, p.vignette, radius = 0.95f, innerStop = 0.55f)
    if (p.grain > 0f) addGrain(g, p.grain, p.seed.toInt())

    val output = p.out.ensureExtension("png")
    gart.saveImage(g, output)
    if (!headless) gart.window().showImage(g)
}

private fun resolveParams(): Params {
    val p = Params(
        seed = pl("seed", 7L),
        text = ps("text", "gart"),
        out = ps("out", "output/turgor"),
        ss = pi("ss", 2),
        fill = pf("fill", 0.84f),
        lean = pf("lean", 20f),
        leanVary = pf("leanvary", 6f),
        bounce = pf("bounce", 0.10f),
        spin = pf("spin", 8f),
        squash = pf("squash", 1.15f),
        overlap = pf("overlap", 0.16f),
        drag = pf("drag", 0.16f),
        swell = pf("swell", 0.026f),
        swellFreq = pf("swellfreq", 2.0f),
        wobble = pf("wobble", 0.016f),
        wobbleFreq = pf("wobblefreq", 3.2f),
        barbs = pi("barbs", 2),
        barbLen = pf("barblen", 0.22f),
        fatten = pf("fatten", 0.052f),
        counter = pf("counter", 1.0f),
        outline = pf("outline", 0.026f),
        shine = pf("shine", 0.022f),
        shineWidth = pf("shinewidth", 0.013f),
        shineGap = pf("shinegap", 0.35f),
        extrude = pf("extrude", 0.075f),
        extrudeAngle = pf("extrudeangle", 62f),
        halo = pf("halo", 0.30f),
        haloLobe = pf("halolobe", 0.45f),
        haloSpots = pi("halospots", 9),
        haloDots = pi("halodots", 16),
        haloMottle = pf("halomottle", 0.55f),
        haloMist = pf("halomist", 0.5f),
        haloRough = pf("halorough", 0.05f),
        haloSoft = pf("halosoft", 0.30f),
        streaks = pf("streaks", 0.55f),
        streakAngle = pf("streakangle", 6f),
        hue = pf("hue", 0f),
        grain = pf("grain", 0.05f),
        vignette = pf("vignette", 0.18f),
    )

    require(p.text.isNotBlank()) { "text must not be blank" }
    require(p.ss in 1..4) { "ss must be between 1 and 4" }
    require(p.fill in 0.3f..0.98f) { "fill must be between 0.3 and 0.98" }
    require(p.lean in -45f..45f) { "lean must be between -45 and 45" }
    require(p.leanVary in 0f..20f) { "leanvary must be between 0 and 20" }
    require(p.bounce in 0f..0.4f) { "bounce must be between 0 and 0.4" }
    require(p.spin in 0f..25f) { "spin must be between 0 and 25" }
    require(p.squash in 0f..2f) { "squash must be between 0 and 2" }
    require(p.overlap in -0.3f..0.6f) { "overlap must be between -0.3 and 0.6" }
    require(p.drag in -0.6f..0.6f) { "drag must be between -0.6 and 0.6" }
    require(p.swell in 0f..0.2f) { "swell must be between 0 and 0.2" }
    require(p.swellFreq in 0.2f..12f) { "swellfreq must be between 0.2 and 12" }
    require(p.wobble in 0f..0.08f) { "wobble must be between 0 and 0.08" }
    require(p.wobbleFreq in 0.5f..40f) { "wobblefreq must be between 0.5 and 40" }
    require(p.barbs in 0..12) { "barbs must be between 0 and 12" }
    require(p.barbLen in 0f..0.8f) { "barblen must be between 0 and 0.8" }
    require(p.fatten in 0.01f..0.30f) { "fatten must be between 0.01 and 0.30" }
    require(p.counter in 0f..1.2f) { "counter must be between 0 and 1.2" }
    require(p.outline in 0f..0.12f) { "outline must be between 0 and 0.12" }
    // clamped against fatten at draw time, so a sweep crossing these axes closes the ring instead of failing
    require(p.shine >= 0f && p.shineWidth >= 0f) { "shine and shinewidth must be >= 0" }
    require(p.shineGap in 0f..1f) { "shinegap must be between 0 and 1" }
    require(p.extrude in 0f..0.4f) { "extrude must be between 0 and 0.4" }
    require(p.halo in 0f..1.2f) { "halo must be between 0 and 1.2" }
    require(p.haloLobe in 0f..0.95f) { "halolobe must be between 0 and 0.95" }
    require(p.haloSpots in 0..60) { "halospots must be between 0 and 60" }
    require(p.haloDots in 0..80) { "halodots must be between 0 and 80" }
    require(p.haloMottle in 0f..1f) { "halomottle must be between 0 and 1" }
    require(p.haloMist in 0f..1f) { "halomist must be between 0 and 1" }
    require(p.haloRough in 0f..0.4f) { "halorough must be between 0 and 0.4" }
    require(p.haloSoft in 0f..1.5f) { "halosoft must be between 0 and 1.5" }
    require(p.streaks in 0f..1f) { "streaks must be between 0 and 1" }
    require(p.hue in -180f..180f) { "hue must be between -180 and 180" }
    require(p.grain in 0f..0.5f) { "grain must be between 0 and 0.5" }
    require(p.vignette in 0f..1.2f) { "vignette must be between 0 and 1.2" }
    return p
}

private const val PINK = 0xFFCE6D7B.toInt()
private const val INK = 0xFF15131F.toInt()
private const val DEEP = 0xFF0A0910.toInt()
private const val SHINE = 0xFFF2E6DA.toInt()
private val GROUND = RetroColors.black01
private const val HALO = 0xFF2F8C82.toInt()
/** how far the pooled blots sit below the cloud they lie on */
private const val HALO_POOL = 0.30f

/** how much of its own half-width a contour may be pushed before it folds through itself */
private const val BUDGET = 0.55f

private class Colors(hue: Float) {
    val pink = hueShift(PINK, hue)
    val shine = hueShift(SHINE, hue * 0.5f)
    val ink = INK
    val deep = DEEP

    // turns with the body, so the backdrop keeps its distance on the colour wheel
    val halo = hueShift(HALO, hue)
    val haloDeep = darken(halo, HALO_POOL)
}

/** rotates in OKLCH, so lightness and chroma survive the turn */
private class Letter(val core: Path, val bounds: Rect)

private fun render(c: Canvas, p: Params) {
    c.drawRect(Rect.makeWH(W.toFloat(), H.toFloat()), fillOf(GROUND))

    val f = font(FontFamily.Literata, FONT_SIZE)
    val capH = f.metrics.capHeight
    val letters = buildLetters(f, capH, p)
    if (letters.isEmpty()) return

    val colors = Colors(p.hue)
    val extLen = p.extrude * capH
    // dots are left out of the fit on purpose, better they run off the frame than shrink the piece
    val haloReach = p.halo * capH * (1f + p.haloLobe)
    val reach = (p.fatten + p.outline) * capH + extLen + haloReach

    // fit the finished mass, not the raw outlines, since warp and stroke push past the glyph box
    var l = Float.MAX_VALUE
    var t = Float.MAX_VALUE
    var r = -Float.MAX_VALUE
    var b = -Float.MAX_VALUE
    letters.forEach {
        l = min(l, it.bounds.left); t = min(t, it.bounds.top)
        r = max(r, it.bounds.right); b = max(b, it.bounds.bottom)
    }
    l -= reach
    t -= reach
    r += reach
    b += reach
    val scale = min(W * p.fill / (r - l), H * p.fill / (b - t))

    c.save()
    c.translate(W / 2f, H / 2f)
    c.scale(scale, scale)
    c.translate(-(l + r) / 2f, -(t + b) / 2f)

    if (p.halo > 0f) {
        val piece = outlineOf(letters, 2f * (p.fatten + p.outline) * capH)
        if (piece != null) {
            drawHalo(c, piece, p, capH, colors, Random(p.seed xor 0x4C10D), (p.seed and 0xffff) * 0.01f)
            piece.close()
        }
    }

    // back to front, so each letter's outline cuts into the one behind and makes the black wedges
    val rng = Random(p.seed xor 0x8A17C)
    letters.forEach { drawLetter(c, it, p, capH, colors, rng) }

    c.restore()
}

private fun buildLetters(f: Font, capH: Float, p: Params): List<Letter> {
    val glyphs = f.getStringGlyphs(p.text)
    // getPath per glyph, not getPaths, which drops pathless glyphs so its indices stop matching
    val widths = f.getWidths(glyphs)
    val rng = Random(p.seed)
    val nz = (p.seed and 0xffff) * 0.01f

    val letters = mutableListOf<Letter>()
    var penX = 0f
    for (i in glyphs.indices) {
        val glyph = f.getPath(glyphs[i])
        if (glyph != null && !glyph.isEmpty) {
            val attitude = Attitude(glyph.bounds, penX, capH, p, rng)
            val barbRng = Random(p.seed * 31 + i)
            val contours = contoursOf(glyph, capH * 0.010f)
            if (contours.isNotEmpty()) {
                val shape = Shape(contours)
                contours.forEachIndexed { k, pts ->
                    attitude.place(pts)
                    warp(pts, p, capH, nz, shape.sign, shape.isHole(k))
                    if (!shape.isHole(k)) barb(pts, p.barbs, p.barbLen * capH, shape.radius(k), barbRng)
                }
                val core = pathOf(contours)
                letters.add(Letter(core, core.bounds))
            }
        }
        // a space still advances the pen, just without an overlap pull
        penX += widths[i] * (if (glyph == null || glyph.isEmpty) 1f else 1f - p.overlap)
        glyph?.close()
    }
    return letters
}

/** the per-letter transform, in glyph space where the baseline is y=0 and the ascender negative */
private class Attitude(bounds: Rect, penX: Float, private val capH: Float, p: Params, rng: Random) {
    private val cx = bounds.left + bounds.width / 2f
    private val cy = bounds.top + bounds.height / 2f
    private val sx = 1f + rng.between(-0.06f, 0.15f) * p.squash
    private val sy = 1f + rng.between(-0.05f, 0.20f) * p.squash
    private val lean = tan((p.lean + rng.between(-p.leanVary, p.leanVary)).toRadians())
    private val spin = rng.between(-p.spin, p.spin).toRadians()
    private val cosT = cos(spin)
    private val sinT = sin(spin)
    private val drag = p.drag * capH * rng.between(0.4f, 1.6f)
    private val dy = rng.between(-p.bounce, p.bounce) * capH
    private val dx = penX

    fun place(pts: FloatArray) {
        for (i in 0 until pts.size / 2) {
            // squash then spin, both about the letter's own centre
            val qx = cx + (pts[i * 2] - cx) * sx
            val qy = cy + (pts[i * 2 + 1] - cy) * sy
            val ax = qx - cx
            val ay = qy - cy
            var x = cx + ax * cosT - ay * sinT
            val y = cy + ax * sinT + ay * cosT
            // lean: y is negative above the baseline, so this pushes the top to the right
            x -= lean * y
            // drag grows with height, so the foot lags the shoulder instead of shearing flat
            val h = (-y / capH).coerceIn(0f, 1.6f)
            x += drag * h * h
            pts[i * 2] = x + dx
            pts[i * 2 + 1] = y + dy
        }
    }
}

/** even arc-length walk over every contour, which flattens the curves for free */
private fun contoursOf(path: Path, step: Float): List<FloatArray> {
    val out = mutableListOf<FloatArray>()
    val pm = PathMeasure(path, false)
    do {
        val len = pm.length
        if (len > step * 8) {
            val n = (len / step).toInt().coerceIn(16, 4096)
            val pts = FloatArray(n * 2)
            var ok = true
            for (i in 0 until n) {
                val pos = pm.getPosition(len * i / n)
                if (pos == null) {
                    ok = false
                    break
                }
                pts[i * 2] = pos.x
                pts[i * 2 + 1] = pos.y
            }
            if (ok) out.add(pts)
        }
    } while (pm.nextContour())
    pm.close()
    return out
}

/** Splits counters from outer contours by nesting, and takes the normal's sign from the biggest one. */
private class Shape(contours: List<FloatArray>) {
    private val areas = FloatArray(contours.size) { signedArea(contours[it]) }
    private val outer = areas.indices.maxBy { abs(areas[it]) }
    val sign = if (areas[outer] > 0f) 1f else -1f

    // nesting, not winding: font glyphs wind counters the opposite way but the path ops hand back
    // even-odd paths with every contour wound the same, where the sign says nothing about holes
    private val hole = BooleanArray(contours.size) { i ->
        var enclosing = 0
        for (j in contours.indices) {
            if (j == i || abs(areas[j]) <= abs(areas[i])) continue
            if (encloses(contours[j], contours[i][0], contours[i][1])) enclosing++
        }
        enclosing % 2 == 1
    }

    fun isHole(i: Int) = hole[i]
    fun radius(i: Int) = sqrt(abs(areas[i]) / PIf)
}

/** Width of the contour at its thinnest; an equal-area radius will not do, as a stem is long and thin. */
private fun narrowest(pts: FloatArray): Float {
    val n = pts.size / 2
    val gap = max(6, n / 20)
    if (n < 2 * gap + 2) return Float.MAX_VALUE
    var best = Float.MAX_VALUE
    for (i in 0 until n) {
        for (j in i + gap..i + n - gap) {
            val k = (j % n) * 2
            val dx = pts[k] - pts[i * 2]
            val dy = pts[k + 1] - pts[i * 2 + 1]
            val d = dx * dx + dy * dy
            if (d < best) best = d
        }
    }
    return sqrt(best)
}

/** ray crossing count: is (x, y) inside this closed polyline */
private fun encloses(poly: FloatArray, x: Float, y: Float): Boolean {
    val n = poly.size / 2
    var inside = false
    var j = n - 1
    for (i in 0 until n) {
        val yi = poly[i * 2 + 1]
        val yj = poly[j * 2 + 1]
        if ((yi > y) != (yj > y) && x < poly[i * 2] + (poly[j * 2] - poly[i * 2]) * (y - yi) / (yj - yi)) inside = !inside
        j = i
    }
    return inside
}

private fun signedArea(pts: FloatArray): Float {
    val n = pts.size / 2
    var a = 0f
    for (i in 0 until n) {
        val j = (i + 1) % n
        a += pts[i * 2] * pts[j * 2 + 1] - pts[j * 2] * pts[i * 2 + 1]
    }
    return a / 2f
}

/** Pushes points along the contour normal so stems swell and pinch, and opens counters back up to survive the fattening. */
private fun warp(pts: FloatArray, p: Params, capH: Float, nz: Float, sign: Float, hole: Boolean) {
    val n = pts.size / 2
    if (n < 8) return
    val src = pts.copyOf()
    val sf = p.swellFreq / capH
    val wf = p.wobbleFreq / capH
    var swell = p.swell * capH
    var wobble = p.wobble * capH
    var open = if (hole) p.counter * p.fatten * capH else 0f

    // budget the whole contour at once; push the flanks past the half-width and they cross and cancel to holes
    val budget = BUDGET * narrowest(src) / 2f
    val want = swell + wobble + open
    if (want > budget) {
        val k = budget / want
        swell *= k
        wobble *= k
        open *= k
    }

    for (i in 0 until n) {
        val x = src[i * 2]
        val y = src[i * 2 + 1]
        val pi2 = ((i - 1 + n) % n) * 2
        val ni2 = ((i + 1) % n) * 2
        var tx = src[ni2] - src[pi2]
        var ty = src[ni2 + 1] - src[pi2 + 1]
        val tl = hypot(tx, ty)
        if (tl < 1e-5f) continue
        tx /= tl
        ty /= tl
        val nx = sign * ty
        val ny = -sign * tx
        val d = SimplexNoise.noise(x * sf + nz, y * sf + nz) * swell + open
        pts[i * 2] = x + nx * d + SimplexNoise.noise(x * wf + nz + 31.7f, y * wf + nz) * wobble
        pts[i * 2 + 1] = y + ny * d + SimplexNoise.noise(x * wf + nz, y * wf + nz + 57.3f) * wobble
    }
}

/** Grows a spike out of the contour by dragging its furthest vertex outward, so the base can never come apart. */
private fun barb(pts: FloatArray, count: Int, len: Float, radius: Float, rng: Random) {
    val n = pts.size / 2
    if (count <= 0 || n < 32) return
    @Suppress("NAME_SHADOWING") val len = min(len, radius)
    // tight span and steep falloff, or the body dilation just rounds the tip away
    val span = max(3, n / 40)
    val turn = rng.nextFloat() * TAUf
    repeat(count) { k ->
        // one direction per sector, so the spikes spread round the letter instead of clustering
        val a = turn + TAUf * (k + rng.between(0.15f, 0.85f)) / count
        val dx = cos(a)
        val dy = sin(a)
        var best = 0
        var bestD = -Float.MAX_VALUE
        for (i in 0 until n) {
            val d = pts[i * 2] * dx + pts[i * 2 + 1] * dy
            if (d > bestD) {
                bestD = d
                best = i
            }
        }
        val l = len * rng.between(0.5f, 1f)
        for (j in -span..span) {
            val i = ((best + j) % n + n) % n
            val w = (1f - abs(j) / (span + 1f)).pow(2.2f)
            pts[i * 2] += dx * l * w
            pts[i * 2 + 1] += dy * l * w
        }
    }
}

private fun pathOf(contours: List<FloatArray>): Path {
    val pb = PathBuilder()
    pb.setFillType(PathFillMode.WINDING)
    contours.forEach { pb.addPoly(it, true) }
    // warping can fold a contour over itself, and winding keeps that overlap solid
    return pb.detach().also { it.fillMode = PathFillMode.WINDING }
}

private fun drawLetter(c: Canvas, letter: Letter, p: Params, capH: Float, colors: Colors, rng: Random) {
    val core = letter.core
    val body = 2f * p.fatten * capH
    val outer = body + 2f * p.outline * capH
    // the ring is cut out of the body, so it closes up when there is no room left
    val shine = min(p.shine, p.fatten)
    val ringOuter = 2f * (p.fatten - shine) * capH
    val ringInner = ringOuter - 2f * min(p.shineWidth, p.fatten - shine) * capH

    // 3d block, stepped so it stays solid however far it is pushed
    val extLen = p.extrude * capH
    if (extLen > 0.5f) {
        val ex = cosDeg(p.extrudeAngle)
        val ey = sinDeg(p.extrudeAngle)
        val steps = max(1, (extLen / 1.1f).toInt())
        for (s in steps downTo 1) {
            val d = extLen * s / steps
            c.save()
            c.translate(ex * d, ey * d)
            stroked(c, core, outer, colors.deep)
            c.restore()
        }
    }

    stroked(c, core, outer, colors.ink)
    stroked(c, core, body, colors.pink)

    val silhouette = dilate(core, body)
    if (silhouette != null) {
        drawStreaks(c, silhouette, p, capH, colors, rng)
        drawShine(c, core, silhouette, ringOuter, ringInner, p, capH, colors, rng)
        if (silhouette !== core) silhouette.close()
    }
}

/** the whole word as one shape at outline width, which is what the backdrop wraps */
private fun outlineOf(letters: List<Letter>, width: Float): Path? {
    var piece = Path()
    letters.forEach { letter ->
        val body = dilate(letter.core, width) ?: letter.core
        val merged = Path.makeCombining(piece, body, PathOp.UNION)
        if (body !== letter.core) body.close()
        if (merged != null) {
            piece.close()
            piece = merged
        }
    }
    return if (piece.isEmpty) null else piece
}

/** Lays the backdrop down like a fat cap: circles walked round the piece, sized by a noise field so the edge lobes. */
private fun drawHalo(c: Canvas, piece: Path, p: Params, capH: Float, colors: Colors, rng: Random, nz: Float) {
    val reach = p.halo * capH
    if (reach <= 0f) return

    val cloud = cloudAt(piece, p, capH, nz, 1f)

    // knocks the arcs off the circles the cloud is stamped from, so the edge is chewed rather than drawn
    val rough = roughen(reach * 0.22f, reach * p.haloRough, reach * 0.11f, p.seed.toInt())

    // a can lays more paint in the middle than at the rim, so the edge is a stack of oversized
    // passes at low alpha under the solid core rather than one hard boundary
    if (p.haloSoft > 0f) {
        for (k in HALO_FADE downTo 1) {
            val wide = cloudAt(piece, p, capH, nz, 1f + p.haloSoft * k / HALO_FADE)
            // own seed per pass, else every pass is chewed identically and the fade shows as rings
            val edge = roughen(reach * 0.22f, reach * p.haloRough, reach * 0.11f, p.seed.toInt() + k * 977)
            val fade = fillOf(alpha(colors.halo, 16 + 11 * (HALO_FADE - k))).apply { pathEffect = edge }
            c.drawPath(wide, fade)
            fade.close()
            edge?.close()
            wide.close()
        }
    }

    val body = fillOf(colors.halo).apply { pathEffect = rough }
    c.drawPath(cloud, body)
    body.close()

    drawMottle(c, cloud, p, capH, colors, rng, nz)
    cloud.close()
    drawMist(c, piece, p, capH, colors, rng, nz)

    // blots inside the cloud and dots off it, both hung on the piece outline so they follow the letters
    if (p.haloSpots <= 0 && p.haloDots <= 0) {
        rough?.close()
        return
    }
    val stops = mutableListOf<FloatArray>()
    walk(piece, capH * 0.02f) { x, y, tx, ty -> stops.add(floatArrayOf(x, y, tx, ty)) }
    if (stops.isEmpty()) {
        rough?.close()
        return
    }

    // a dot is a fraction of the cloud's size, so it gets its own milder roughening
    val speck = roughen(reach * 0.09f, reach * p.haloRough * 0.35f, reach * 0.05f, p.seed.toInt() * 31 + 7)

    if (p.haloSpots > 0) {
        val deep = fillOf(colors.haloDeep).apply { pathEffect = speck }
        scatter(c, piece, stops, p.haloSpots, rng, deep) { reach * rng.between(0.15f, 0.75f) to reach * rng.between(0.20f, 0.42f) }
        deep.close()
    }
    if (p.haloDots > 0) {
        val dot = fillOf(colors.halo).apply { pathEffect = speck }
        scatter(c, piece, stops, p.haloDots, rng, dot) { reach * rng.between(1.25f, 2.9f) to reach * rng.between(0.10f, 0.36f) }
        dot.close()
    }
    speck?.close()
    rough?.close()
}

/** roughen, then round the facets it leaves, so the edge undulates instead of coming out spiky */
private fun roughen(seg: Float, dev: Float, round: Float, seed: Int): PathEffect? {
    if (dev <= 0f) return null
    val jag = PathEffect.makeDiscrete(seg, dev, seed)
    if (round <= 0f) return jag
    val corner = PathEffect.makeCorner(round)
    val soft = corner.makeCompose(jag)
    jag.close()
    corner.close()
    return soft
}

/** how many oversized passes fade the cloud out under its solid core */
private const val HALO_FADE = 5

/** the cloud stamped at [grow] times its reach, as one winding path so the circles read as solid */
private fun cloudAt(piece: Path, p: Params, capH: Float, nz: Float, grow: Float): Path {
    val pb = PathBuilder()
    pb.setFillType(PathFillMode.WINDING)
    walk(piece, p.halo * capH * 0.30f) { x, y, _, _ ->
        val r = lobe(x, y, p, capH, nz) * grow
        if (r > 0f) pb.addCircle(x, y, r)
    }
    return pb.detach().also { it.fillMode = PathFillMode.WINDING }
}

/** how far the cloud reaches at this point, the same field the stamps use */
private fun lobe(x: Float, y: Float, p: Params, capH: Float, nz: Float): Float {
    val f = 1.5f / capH
    return p.halo * capH * (1f + p.haloLobe * SimplexNoise.noise(x * f + nz, y * f + nz))
}

/** paint pools where the can lingered and goes thin where it passed, in patches set by a noise field. */
private fun drawMottle(c: Canvas, cloud: Path, p: Params, capH: Float, colors: Colors, rng: Random, nz: Float) {
    if (p.haloMottle <= 0f) return
    val reach = p.halo * capH
    val b = cloud.bounds
    val step = reach * 0.62f
    if (step <= 0f) return
    val freq = 2.4f / capH

    c.save()
    c.clipPath(cloud, ClipMode.INTERSECT, true)
    val pt = paint()
    var y = b.top
    while (y < b.bottom) {
        var x = b.left
        while (x < b.right) {
            val px = x + rng.between(-step * 0.5f, step * 0.5f)
            val py = y + rng.between(-step * 0.5f, step * 0.5f)
            val n = SimplexNoise.noise(px * freq + nz + 11.3f, py * freq + nz - 7.1f)
            val a = (abs(n) * p.haloMottle * 105f).toInt()
            if (a > 3) {
                val col = if (n > 0f) colors.haloDeep else GROUND
                val r = reach * rng.between(0.45f, 1.05f)
                // falls off to nothing at the rim, or it just reads as a disc
                val shader = Shader.makeRadialGradient(
                    px, py, r,
                    gradientOf(iarr(alpha(col, a), alpha(col, (a * 0.45f).toInt()), alpha(col, 0)), farr(0f, 0.5f, 1f)),
                )
                pt.shader = shader
                c.drawCircle(px, py, r, pt)
                shader.close()
            }
            x += step
        }
        y += step
    }
    pt.shader = null
    pt.close()
    c.restore()
}

/** Overspray: speckle that thins out fast, thrown from the piece outline at the cloud's own reach. */
private fun drawMist(c: Canvas, piece: Path, p: Params, capH: Float, colors: Colors, rng: Random, nz: Float) {
    if (p.haloMist <= 0f) return
    val reach = p.halo * capH
    val spread = reach * 0.9f
    val perStop = 1 + (p.haloMist * 5f).toInt()
    val pt = paint()

    walk(piece, capH * 0.03f) { x, y, tx, ty ->
        var nx = ty
        var ny = -tx
        if (piece.contains(x + nx * PROBE, y + ny * PROBE)) {
            nx = -nx
            ny = -ny
        }
        val edge = lobe(x, y, p, capH, nz)
        repeat(perStop) {
            val t = rng.nextFloat()
            // starts just inside the edge so the speckle softens the rim rather than ringing it
            val d = edge - spread * 0.15f + spread * t * t
            val side = rng.between(-reach * 0.3f, reach * 0.3f)
            pt.color = alpha(colors.halo, (25f + (1f - t) * (1f - t) * 205f).toInt().coerceIn(0, 255))
            c.drawCircle(x + nx * d - ny * side, y + ny * d + nx * side, capH * rng.between(0.004f, 0.017f), pt)
        }
    }
    pt.close()
}

/** walks every contour of [path] at [step] intervals, handing back position and unit tangent */
private inline fun walk(path: Path, step: Float, action: (Float, Float, Float, Float) -> Unit) {
    val pm = PathMeasure(path, false)
    do {
        val len = pm.length
        var d = 0f
        while (d < len) {
            val pos = pm.getPosition(d)
            val tan = pm.getTangent(d)
            if (pos != null && tan != null) action(pos.x, pos.y, tan.x, tan.y)
            d += step
        }
    } while (pm.nextContour())
    pm.close()
}

/** Blobs hung off the outside of [piece]; which way is out gets probed, since the path ops pick the winding. */
private inline fun scatter(
    c: Canvas,
    piece: Path,
    stops: List<FloatArray>,
    count: Int,
    rng: Random,
    paint: Paint,
    place: () -> Pair<Float, Float>,
) {
    repeat(count) {
        val s = stops[rng.nextInt(stops.size)]
        var nx = s[3]
        var ny = -s[2]
        if (piece.contains(s[0] + nx * PROBE, s[1] + ny * PROBE)) {
            nx = -nx
            ny = -ny
        }
        val (out, size) = place()
        val cx = s[0] + nx * out
        val cy = s[1] + ny * out
        val squash = rng.between(0.62f, 1f)
        c.save()
        c.translate(cx, cy)
        c.rotate(rng.nextFloat() * 360f)
        c.drawOval(Rect.makeLTRB(-size, -size * squash, size, size * squash), paint)
        c.restore()
    }
}

private const val PROBE = 2f

/** one path and one width: a centred stroke plus fill grows the outside and tightens counters by w/2 */
private fun stroked(c: Canvas, path: Path, width: Float, color: Int) {
    val pt = paint().apply {
        this.color = color
        mode = if (width > 0f) PaintMode.STROKE_AND_FILL else PaintMode.FILL
        strokeWidth = max(0f, width)
        strokeJoin = PaintStrokeJoin.ROUND
        strokeCap = PaintStrokeCap.ROUND
    }
    c.drawPath(path, pt)
    pt.close()
}

/** the same shape [stroked] draws, but as a path, for clipping and for the shine ring */
private fun dilate(core: Path, width: Float): Path? {
    if (width <= 0f) return core
    val sp = Paint().apply {
        mode = PaintMode.STROKE
        strokeWidth = width
        strokeJoin = PaintStrokeJoin.ROUND
        strokeCap = PaintStrokeCap.ROUND
    }
    val band = PathUtils.fillPathWithPaint(core, sp)
    sp.close()
    val res = Path.makeCombining(band, core, PathOp.UNION)
    band.close()
    return res
}

/** roller texture: broken hatch running across the strokes, clipped to the body */
private fun drawStreaks(c: Canvas, silhouette: Path, p: Params, capH: Float, colors: Colors, rng: Random) {
    if (p.streaks <= 0f) return
    val b = silhouette.bounds
    val diag = hypot(b.width, b.height)
    if (diag < 1f) return

    val a = p.streakAngle.toRadians()
    val dx = cos(a)
    val dy = sin(a)
    val cx = b.left + b.width / 2f
    val cy = b.top + b.height / 2f
    val gap = capH / (16f + p.streaks * 60f)
    val light = lighten(colors.pink, 0.42f)

    c.save()
    c.clipPath(silhouette, ClipMode.INTERSECT, true)
    val pt = paint().apply {
        mode = PaintMode.STROKE
        strokeCap = PaintStrokeCap.ROUND
        strokeJoin = PaintStrokeJoin.ROUND
    }
    var t = -diag / 2f
    while (t <= diag / 2f) {
        // perpendicular offset of this hatch line from the centre
        val ox = cx - dy * t
        val oy = cy + dx * t
        // a pass either lays one long stroke or breaks into short dashes, the way a hand would
        val long = rng.nextFloat() < 0.45f
        // width holds across a pass, since one stroke keeps its weight
        val width = capH * (if (long) rng.between(0.013f, 0.028f) else rng.between(0.008f, 0.019f))
        var u = -diag / 2f
        while (u < diag / 2f) {
            val seg = capH * (if (long) rng.between(0.30f, 1.00f) else rng.between(0.06f, 0.26f))
            if (rng.nextFloat() < (if (long) 0.86f else 0.60f)) {
                pt.color = alpha(light, (28f + rng.nextFloat() * 76f).toInt())
                pt.strokeWidth = width * rng.between(0.85f, 1.15f)
                drawByHand(c, ox + dx * u, oy + dy * u, dx, dy, seg, capH, rng, pt)
            }
            u += seg + capH * (if (long) rng.between(0.02f, 0.10f) else rng.between(0.05f, 0.22f))
        }
        t += gap * rng.between(0.7f, 1.4f)
    }
    pt.close()
    c.restore()
}

/** one stroke, bowed through the middle and unsteady along it, so it does not come out ruled */
private fun drawByHand(
    c: Canvas,
    x: Float,
    y: Float,
    dx: Float,
    dy: Float,
    len: Float,
    capH: Float,
    rng: Random,
    pt: Paint,
) {
    val bow = capH * rng.between(-0.022f, 0.022f)
    val shake = capH * rng.between(0.0015f, 0.006f)
    val steps = (len / (capH * 0.09f)).toInt().coerceIn(2, 10)
    val pb = PathBuilder()
    for (k in 0..steps) {
        val f = k / steps.toFloat()
        // the bow arcs the whole stroke, the shake is the hand on top of it
        val off = bow * sin(PIf * f) + rng.between(-shake, shake)
        val px = x + dx * len * f - dy * off
        val py = y + dy * len * f + dx * off
        if (k == 0) pb.moveTo(px, py) else pb.lineTo(px, py)
    }
    val path = pb.detach()
    c.drawPath(path, pt)
    path.close()
}

/** The shine as a real ring path */
private fun drawShine(
    c: Canvas,
    core: Path,
    silhouette: Path,
    ringOuter: Float,
    ringInner: Float,
    p: Params,
    capH: Float,
    colors: Colors,
    rng: Random,
) {
    if (ringOuter <= 0f || ringOuter <= ringInner) return
    val outer = dilate(core, ringOuter) ?: return
    val inner = dilate(core, ringInner) ?: return
    val ring = Path.makeCombining(outer, inner, PathOp.DIFFERENCE)
    if (outer !== core) outer.close()
    if (inner !== core) inner.close()
    if (ring == null) return

    c.save()
    c.clipPath(silhouette, ClipMode.INTERSECT, true)
    val pt = fillOf(colors.shine)
    c.drawPath(ring, pt)
    pt.close()

    // gaps land on the ring itself, so they always cut it instead of missing into the body
    if (p.shineGap > 0f) {
        val pm = PathMeasure(ring, false)
        val gapPaint = fillOf(colors.pink)
        do {
            val len = pm.length
            if (len > capH * 0.2f) {
                val n = (p.shineGap * len / (capH * 0.55f)).toInt()
                repeat(n) {
                    val pos = pm.getPosition(rng.nextFloat() * len)
                    if (pos != null) c.drawCircle(pos.x, pos.y, capH * rng.between(0.035f, 0.11f), gapPaint)
                }
            }
        } while (pm.nextContour())
        gapPaint.close()
        pm.close()
    }
    c.restore()
    ring.close()
}
