package sabulum

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.PaletteGenerator
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.color.lighten
import dev.oblac.gart.color.space.ColorOKLCH
import dev.oblac.gart.color.space.color4f
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.pl
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.HALF_PIf
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.between
import dev.oblac.gart.util.farr
import dev.oblac.gart.util.iarr
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Shader
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** sand sea. one ridge curve per dune, back to front, each filling below its own crest. */
private const val W = 1080
private const val H = 1620
private const val HARMONICS = 3

private const val OVERLAP = 4f

private const val SAND = 0
private const val EMBER = 1
private const val SHADE = 2

private data class Params(
    val seed: Long,
    val out: String,
    val ss: Int,
    val dunes: Int,
    val horizon: Float,
    val reach: Float,
    val depth: Float,
    val wave: Float,
    val freq: Float,
    val tilt: Float,
    val skew: Float,
    val arc: Float,
    val drift: Float,
    val stray: Float,
    val accent: Float,
    val pair: Float,
    val face: Float,
    val relief: Float,
    val crest: Float,
    val haze: Float,
    val shadeDrop: Float,
    val shadeWarm: Float,
    val sunX: Float,
    val sunY: Float,
    val sunR: Float,
    val halo: Float,
    val glow: Float,
    val raster: Float,
    val rasterJitter: Float,
    val rasterFloor: Float,
    val grain: Float,
    val vignette: Float,
)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val p = resolveParams()
    val gart = Gart.of("sabulum", W, H)

    println(gart)
    println(
        "seed=${p.seed} dunes=${p.dunes} wave=${p.wave} freq=${p.freq} tilt=${p.tilt} " +
            "skew=${p.skew} arc=${p.arc} depth=${p.depth} accent=${p.accent} relief=${p.relief} " +
            "raster=${p.raster} jitter=${p.rasterJitter} grain=${p.grain}",
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

    if (p.vignette > 0f) g.canvas.drawVignette(g.d, p.vignette, radius = 0.92f, innerStop = 0.58f)
    if (p.grain > 0f) addGrain(g, p.grain, p.seed.toInt())

    val output = p.out.ensureExtension("png")
    gart.saveImage(g, output)
    if (!headless) gart.window().showImage(g)
}

private fun resolveParams(): Params {
    val p = Params(
        seed = pl("seed", 7L),
        out = ps("out", "output/sabulum"),
        ss = pi("ss", 2),
        dunes = pi("dunes", 8),
        horizon = pf("horizon", 0.38f),
        reach = pf("reach", 1.02f),
        depth = pf("depth", 1.60f),
        wave = pf("wave", 150f),
        freq = pf("freq", 0.62f),
        tilt = pf("tilt", 0.10f),
        skew = pf("skew", 0.45f),
        arc = pf("arc", 0.72f),
        drift = pf("drift", 0.11f),
        stray = pf("stray", 0.50f),
        accent = pf("accent", 0.55f),
        pair = pf("pair", 0.35f),
        face = pf("face", 1.35f),
        relief = pf("relief", 0.50f),
        crest = pf("crest", 0.30f),
        haze = pf("haze", 0.12f),
        shadeDrop = pf("shadedrop", 0.50f),
        shadeWarm = pf("shadewarm", -14f),
        sunX = pf("sunx", -1f),
        sunY = pf("suny", -1f),
        sunR = pf("sunr", 0.072f),
        halo = pf("halo", 7.5f),
        glow = pf("glow", 0.28f),
        raster = pf("raster", 4f),
        rasterJitter = pf("rasterjitter", 0.30f),
        rasterFloor = pf("rasterfloor", 0.05f),
        grain = pf("grain", 0.06f),
        vignette = pf("vignette", 0.10f),
    )

    require(p.ss in 1..4) { "ss must be between 1 and 4" }
    require(p.dunes in 3..24) { "dunes must be between 3 and 24" }
    require(p.horizon in 0.05f..0.85f) { "horizon must be between 0.05 and 0.85" }
    require(p.reach in 0.6f..1.4f) { "reach must be between 0.6 and 1.4" }
    require(p.depth in 0f..4f) { "depth must be between 0 and 4" }
    require(p.wave in 0f..420f) { "wave must be between 0 and 420" }
    require(p.freq in 0.2f..4f) { "freq must be between 0.2 and 4" }
    require(p.tilt in -0.6f..0.6f) { "tilt must be between -0.6 and 0.6" }
    require(p.skew in -0.9f..0.9f) { "skew must be between -0.9 and 0.9" }
    require(p.arc in 0f..1f) { "arc must be between 0 and 1" }
    require(p.drift in -0.3f..0.3f) { "drift must be between -0.3 and 0.3" }
    require(p.stray in 0f..1f) { "stray must be between 0 and 1" }
    require(p.accent in 0f..1f) { "accent must be between 0 and 1" }
    require(p.pair in 0f..1f) { "pair must be between 0 and 1" }
    require(p.face in 0.4f..8f) { "face must be between 0.4 and 8" }
    require(p.relief in 0f..0.9f) { "relief must be between 0 and 0.9" }
    require(p.crest in 0f..1f) { "crest must be between 0 and 1" }
    require(p.haze in 0f..0.8f) { "haze must be between 0 and 0.8" }
    require(p.shadeDrop in 0.05f..1f) { "shadedrop must be between 0.05 and 1" }
    require(p.shadeWarm in -180f..180f) { "shadewarm must be between -180 and 180" }
    require(p.sunR in 0f..0.2f) { "sunr must be between 0 and 0.2" }
    require(p.halo in 1f..30f) { "halo must be between 1 and 30" }
    require(p.glow in 0f..1f) { "glow must be between 0 and 1" }
    require(p.raster == 0f || p.raster in 2f..40f) { "raster must be 0 (smooth) or between 2 and 40" }
    require(p.rasterJitter in 0f..1f) { "rasterjitter must be between 0 and 1" }
    require(p.rasterFloor in 0f..1f) { "rasterfloor must be between 0 and 1" }
    require(p.grain in 0f..0.5f) { "grain must be between 0 and 0.5" }
    require(p.vignette in 0f..1.2f) { "vignette must be between 0 and 1.2" }
    return p
}

private const val SKY_STOPS = 6
private const val OPAQUE = 0xFF000000.toInt()

/** resolution of the path a ramp gets sampled out of */
private const val PATH_STOPS = 64

/** one colour -> ramp. far end in OKLCH (L/C/H move independently there), generator walks between. */
private fun oklchRamp(anchor: Int, endL: Float, endC: Float, endH: Float, stops: Int, curve: Float): Palette {
    val far = ColorOKLCH(
        l = endL.coerceIn(0f, 1f),
        c = endC.coerceAtLeast(0f),
        h = endH,
    ).toColor4f().toColor() or OPAQUE
    val path = PaletteGenerator.sequential(listOf(anchor, far), PATH_STOPS)
    // curve > 1 holds the change back for the far end - haze piles up at the horizon, not evenly
    return Palette(IntArray(stops) { i -> path.sample((i / (stops - 1f)).pow(curve)) })
}

/** the burn overhead, everything else grown down from it */
private val SKY_TOP = NipponColors.col029_GINSYU
private const val SKY_WARM = 25.8f
private const val SKY_FADE = 0.36f
private const val SKY_LIFT = 0.174f
private const val SKY_CURVE = 1.6f

private val SUN_CORE = NipponColors.col106_USUKI

/** nothing about it varies, so build it once */
private val sky = run {
    val top = ColorOKLCH.of(SKY_TOP.color4f())
    oklchRamp(
        anchor = SKY_TOP,
        endL = top.l + SKY_LIFT,
        endC = top.c * (1f - SKY_FADE),
        endH = top.h + SKY_WARM,
        stops = SKY_STOPS,
        curve = SKY_CURVE,
    )
}

private const val RAMP_STOPS = 6

/** lit ends: open sand, lit slip face, lee shadow */
private val rampTops = intArrayOf(
    NipponColors.col105_TORINOKO,// 0xFFEDD5B8 ????
    NipponColors.col073_SHAREGAKI,
    NipponColors.col172_OMESHICHA, //0xFF406C78?
)

/** shade each ramp down from its lit end. reversed out - these index dark to light. */
private fun buildRamps(p: Params): Array<Palette> = Array(rampTops.size) { i ->
    val top = ColorOKLCH.of(rampTops[i].color4f())
    val darkL = (top.l * p.shadeDrop).coerceIn(0.03f, 1f)
    oklchRamp(
        anchor = rampTops[i],
        endL = darkL,
        // chroma peaks mid-lightness, so light colours deepen as they shade and dark ones just grey
        endC = top.c * chromaPeak(darkL) / chromaPeak(top.l),
        endH = top.h + p.shadeWarm,
        stops = RAMP_STOPS,
        curve = 1f,
    ).reversed()
}

/** chroma a pigment holds at l - peaks mid, zero at both ends */
private fun chromaPeak(l: Float) = sqrt((l * (1f - l)).coerceAtLeast(0f))

/** slice of its ramp each family may walk. lee face stays low or it stops reading as shadow. */
private val windows = arrayOf(
    farr(0.34f, 0.86f),
    farr(0.40f, 0.85f),
    farr(0.04f, 0.46f),
)

private fun render(c: Canvas, p: Params) {
    val rng = Random(p.seed)
    val dunes = Dunes(p, rng)

    val ramps = buildRamps(p)
    val sunX = (if (p.sunX >= 0f) p.sunX else rng.between(0.56f, 0.84f)) * W
    val sunY = (if (p.sunY >= 0f) p.sunY else rng.between(0.15f, 0.30f)) * H
    drawSky(c, p, sunX, sunY)
    drawSun(c, p, sunX, sunY)

    // own stream, so reshaping the dunes leaves the colours alone
    val tintRng = Random(p.seed xor 0x5A17D)
    val family = families(p, tintRng)
    val tone = toneWalk(family, tintRng)
    val horizonColor = sky.last()

    for (i in 0 until p.dunes) {
        val t = dunes.depthOf(i)
        val ramp = ramps[family[i]]

        // distance washes the far dunes toward the sky behind them
        val wash = (1f - t) * p.haze
        val lit = lerpColor(ramp.sample(tone[i] + p.relief * 0.55f), horizonColor, wash)
        val dark = lerpColor(ramp.sample(tone[i] - p.relief * 0.80f), horizonColor, wash * 0.6f)

        val ridge = dunes.ridges[i]
        if (p.raster > 0f) {
            drawRasterFace(c, dunes, ridge, dunes.faceHeight[i], lit, dark, p, i)
        } else {
            drawFace(c, dunes, ridge, dunes.faceHeight[i], lit, dark)
        }
        if (p.crest > 0f) drawCrest(c, dunes, ridge, lit, p.crest, 1.2f + 2.2f * t)
    }
}

/** sand is the ground state, accents take turns. front dune always sand - it carries the frame. */
private fun families(p: Params, rng: Random): IntArray {
    val family = IntArray(p.dunes)
    var next = if (rng.nextBoolean()) SHADE else EMBER
    var previous = SAND
    for (i in 0 until p.dunes) {
        val f = when {
            previous != SAND && rng.nextFloat() >= p.pair -> SAND
            rng.nextFloat() < p.accent -> next.also { next = if (it == SHADE) EMBER else SHADE }
            else -> SAND
        }
        family[i] = f
        previous = f
    }
    family[p.dunes - 1] = SAND
    return family
}

/** reflected walk into each family's window. same-family neighbours forced apart or they merge. */
private fun toneWalk(family: IntArray, rng: Random): FloatArray {
    val raw = FloatArray(family.size)
    val tone = FloatArray(family.size)
    var t = rng.between(0.35f, 0.70f)
    for (i in family.indices) {
        t = reflect(t + rng.between(-0.34f, 0.34f))
        if (i > 0 && family[i] == family[i - 1] && abs(t - raw[i - 1]) < SEPARATION) {
            t = reflect(raw[i - 1] + if (t >= raw[i - 1]) SEPARATION else -SEPARATION)
        }
        raw[i] = t
        val (lo, hi) = windows[family[i]]
        tone[i] = lo + t * (hi - lo)
    }
    return tone
}

private const val SEPARATION = 0.30f

private operator fun FloatArray.component1() = this[0]
private operator fun FloatArray.component2() = this[1]

private fun drawSky(c: Canvas, p: Params, sunX: Float, sunY: Float) {
    // fill the whole frame - a ridge dipping below the horizon would show bare canvas
    val bottom = p.horizon * H + p.wave
    val frame = Rect.makeLTRB(-OVERLAP, -OVERLAP, W + OVERLAP, H + OVERLAP)
    val paint = paint().apply {
        shader = Shader.makeLinearGradient(0f, -OVERLAP, 0f, bottom, gradientOf(sky.toIntArray()))
    }
    c.drawRect(frame, paint)
    paint.close()

    // lift around the sun, else the sky bands flat
    if (p.glow > 0f) {
        val lift = paint().apply {
            shader = Shader.makeRadialGradient(
                sunX, sunY, W * 0.62f,
                gradientOf(
                    // fade to the horizon colour so the lift doesn't tint on its way out
                    iarr(
                        alpha(lighten(sky.last(), 0.18f), (p.glow * 150f).toInt()),
                        alpha(sky.last(), 0),
                    ),
                    farr(0f, 1f),
                ),
            )
        }
        c.drawRect(frame, lift)
        lift.close()
    }
}

private fun drawSun(c: Canvas, p: Params, sunX: Float, sunY: Float) {
    if (p.sunR <= 0f) return
    val r = p.sunR * W

    if (p.glow > 0f) drawHalo(c, p, sunX, sunY, r)

    // flat disc, just enough rim falloff that it isn't cut out
    val disc = paint().apply {
        isDither = true
        shader = Shader.makeRadialGradient(
            sunX, sunY, r,
            gradientOf(iarr(SUN_CORE, SUN_CORE, alpha(SUN_CORE, 0)), farr(0f, 0.90f, 1f)),
        )
    }
    c.drawCircle(sunX, sunY, r, disc)
    disc.close()
}

/** glow on the same screen as the sand - one ink, falloff all in dot size. gradient if raster=0. */
private fun drawHalo(c: Canvas, p: Params, sunX: Float, sunY: Float, r: Float) {
    val outer = r * p.halo
    if (outer <= r) return

    if (p.raster <= 0f) {
        val halo = paint().apply {
            isDither = true
            shader = Shader.makeRadialGradient(
                sunX, sunY, outer,
                gradientOf(
                    iarr(alpha(SUN_CORE, (p.glow * 90f).toInt()), alpha(SUN_CORE, 0)),
                    farr(0f, 1f),
                ),
            )
        }
        c.drawCircle(sunX, sunY, outer, halo)
        halo.close()
        return
    }

    val ink = paint().apply {
        color = alpha(SUN_CORE, (p.glow * 255f).toInt().coerceIn(0, 255))
    }
    // squared, else the glow spreads into an even wash across a third of the sky
    rasterScreen(
        c, p,
        seed = p.seed * 31 + 977,
        left = sunX - outer, top = sunY - outer,
        right = sunX + outer, bottom = sunY + outer,
        ink = ink,
    ) { x, y ->
        val t = ((hypot(x - sunX, y - sunY) - r) / (outer - r)).coerceIn(0f, 1f)
        (1f - t) * (1f - t)
    }
    ink.close()
}

/** halftone: jittered hex grid, dot area tracks coverage. under rasterFloor draws nothing. */
private inline fun rasterScreen(
    c: Canvas,
    p: Params,
    seed: Long,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    ink: Paint,
    coverage: (Float, Float) -> Float,
) {
    val cell = p.raster
    // circles only close up at the cell half-diagonal
    val maxRadius = cell * 0.72f
    val rowStep = cell * 0.866f // hex packing: rows sit in the previous row's gaps
    val jitter = p.rasterJitter * cell * 0.5f
    val rng = Random(seed)

    var y = top
    var row = 0
    while (y < bottom) {
        val offset = if (row % 2 == 1) cell * 0.5f else 0f
        var x = left + offset
        while (x < right) {
            val cov = coverage(x, y)
            if (cov > p.rasterFloor) {
                val dotRadius = maxRadius * sqrt((cov - p.rasterFloor) / (1f - p.rasterFloor))
                c.drawCircle(
                    x + rng.between(-jitter, jitter),
                    y + rng.between(-jitter, jitter),
                    dotRadius,
                    ink,
                )
            }
            x += cell
        }
        y += rowStep
        row++
    }
}

/** one strip per column, each hung off that column's own crest so the shading follows the curve. */
private fun drawFace(c: Canvas, d: Dunes, ridge: FloatArray, faceHeight: Float, lit: Int, dark: Int) {
    val shader = Shader.makeLinearGradient(0f, 0f, 0f, 1f, gradientOf(iarr(lit, dark)))
    val paint = paint().apply {
        isDither = true
        this.shader = shader
    }

    c.save()
    c.clipPath(capPath(d, ridge, below = true), ClipMode.INTERSECT, true)
    for (j in 0 until d.cols - 1) {
        val y0 = ridge[j]
        if (y0 > H + OVERLAP) continue
        c.save()
        c.translate(0f, y0)
        c.scale(1f, faceHeight)
        c.drawRect(Rect.makeLTRB(d.x(j), 0f, d.x(j + 1), (H + OVERLAP - y0) / faceHeight), paint)
        c.restore()
    }
    c.restore()

    paint.close()
    shader.close()
}

/** same face screen-printed: flat bed, raster of lit dots. geometry not pixels, so the downsample keeps them. */
private fun drawRasterFace(
    c: Canvas,
    d: Dunes,
    ridge: FloatArray,
    faceHeight: Float,
    lit: Int,
    dark: Int,
    p: Params,
    index: Int,
) {
    val cell = p.raster

    c.save()
    c.clipPath(capPath(d, ridge, below = true), ClipMode.INTERSECT, true)

    // the bed. without it the dots sit on bare sky and the sand is a stencil
    val bed = paint().apply { color = dark }
    c.drawRect(Rect.makeLTRB(-OVERLAP, -OVERLAP, W + OVERLAP, H + OVERLAP), bed)
    bed.close()

    val ink = paint().apply { color = lit }
    var top = Float.MAX_VALUE
    for (j in 0 until d.cols) top = min(top, ridge[j])

    // own grid per dune, else the dots line up into a screen door over the whole frame
    rasterScreen(
        c, p,
        seed = p.seed * 31 + index,
        left = -cell, top = top - cell,
        right = W + cell, bottom = H + cell,
        ink = ink,
    ) { x, y ->
        val j = (x * d.ssf).toInt().coerceIn(0, d.cols - 1)
        // 1 at the crest, 0 once the face has run its depth
        1f - ((y - ridge[j]) / faceHeight).coerceIn(0f, 1f)
    }
    ink.close()
    c.restore()
}

/** lip of light along a crest. clipped to the far side, so it fades up and stays sharp below. */
private fun drawCrest(c: Canvas, d: Dunes, ridge: FloatArray, lit: Int, strength: Float, blur: Float) {
    val paint = paint().apply {
        mode = PaintMode.STROKE
        strokeWidth = 3f + 7f * strength
        color = alpha(lighten(lit, 0.55f), (60f + strength * 195f).toInt().coerceAtMost(255))
        if (blur > 0f) imageFilter = ImageFilter.makeBlur(blur, blur, FilterTileMode.DECAL)
    }

    c.save()
    c.clipPath(capPath(d, ridge, below = false), ClipMode.INTERSECT, true)
    c.drawPath(ridgeLine(d, ridge), paint)
    c.restore()
    paint.close()
}

/** one side of the ridge, closed off at the far edge */
private fun capPath(d: Dunes, ridge: FloatArray, below: Boolean): Path {
    val edge = if (below) H + OVERLAP else -OVERLAP
    val pb = PathBuilder()
    pb.moveTo(d.x(0), edge)
    pb.lineTo(d.x(d.cols - 1), edge)
    var j = d.cols - 1
    while (j >= 0) {
        pb.lineTo(d.x(j), ridge[j])
        j -= d.ss
    }
    pb.lineTo(d.x(0), ridge[0])
    return pb.closePath().detach()
}

private fun ridgeLine(d: Dunes, ridge: FloatArray): Path {
    val pb = PathBuilder()
    pb.moveTo(d.x(0), ridge[0])
    var j = d.ss
    while (j < d.cols) {
        pb.lineTo(d.x(j), ridge[j])
        j += d.ss
    }
    return pb.detach()
}

/** crest curves, one sample per supersampled column. size grows toward the viewer - the only depth cue. */
private class Dunes(p: Params, rng: Random) {
    val ss = p.ss
    val ssf = p.ss.toFloat()
    val cols = W * p.ss + 1
    val n = p.dunes

    val ridges: Array<FloatArray>
    val faceHeight: FloatArray

    fun depthOf(i: Int) = if (n > 1) i / (n - 1f) else 1f

    init {
        // lead harmonic carries it, the rest just hide the sine. more and the crests ripple like water
        val amplitude = farr(
            p.wave * rng.between(0.70f, 0.92f),
            p.wave * rng.between(0.09f, 0.17f),
            p.wave * rng.between(0.03f, 0.06f),
        )
        val frequency = farr(
            p.freq * rng.between(0.80f, 1.30f),
            p.freq * rng.between(1.80f, 2.80f),
            p.freq * rng.between(3.20f, 4.60f),
        )
        val phase = FloatArray(HARMONICS) { rng.nextFloat() }
        val drift = FloatArray(HARMONICS) { p.drift * rng.between(0.6f, 1.5f) * (it + 1) }

        // thicken toward the viewer, normalised to land at reach. jitter so the spacing doesn't march
        val growth = FloatArray(n) { i ->
            val t = depthOf(i)
            val g = 1f + p.depth * t
            g * g * rng.between(1f - 0.45f * p.stray, 1f + 0.45f * p.stray)
        }
        val base = FloatArray(n)
        base[0] = p.horizon * H
        if (n > 1) {
            val span = p.reach * H - base[0]
            val scale = span / (growth.sum() - growth[0])
            for (i in 1 until n) base[i] = base[i - 1] + growth[i] * scale
        }

        val strayRng = Random(p.seed xor 0x51A7L)

        fun scaled(v: FloatArray, spread: Float) =
            FloatArray(v.size) { v[it] * (1f + p.stray * strayRng.between(-spread, spread)) }

        fun shifted(v: FloatArray, spread: Float) =
            FloatArray(v.size) { v[it] + p.stray * strayRng.between(-spread, spread) }

        ridges = Array(n) { i ->
            val t = depthOf(i)
            val swell = 0.55f + 0.45f * t
            val amp = scaled(amplitude, 0.70f)
            val frq = scaled(frequency, 0.40f)
            val phs = shifted(phase, 0.60f)
            // own lean per dune. different slopes have to cross, and the crossings make the crescents
            val lean = p.tilt * H * strayRng.between(1f - 1.5f * p.stray, 1f + 2.2f * p.stray)
            FloatArray(cols) { j ->
                val u = j / ssf / W
                var y = base[i] + lean * (u - 0.5f)
                for (k in 0 until HARMONICS) {
                    val theta = TAUf * (frq[k] * u + phs[k]) + i * drift[k]
                    // skew slides the crest off centre, arc rounds it over - dune vs sine wave
                    val shaped = if (k == 0) theta + p.skew * sin(theta) else theta
                    val w = sin(shaped)
                    val curved = if (k == 0) w + p.arc * (sin(HALF_PIf * w) - w) else w
                    y += amp[k] * swell * curved
                }
                y
            }
        }

        faceHeight = FloatArray(n) { i ->
            val thickness = if (i < n - 1) base[i + 1] - base[i] else base[i] - base[max(0, i - 1)]
            max(70f, thickness * p.face)
        }
    }

    fun x(j: Int) = j / ssf
}

/** fold the walk back inside 0.12..0.88 so it can't stick at the ends */
private fun reflect(v: Float): Float {
    var x = v
    if (x < 0.12f) x = 0.24f - x
    if (x > 0.88f) x = 1.76f - x
    return x.coerceIn(0.12f, 0.88f)
}
