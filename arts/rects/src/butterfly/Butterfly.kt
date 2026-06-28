package butterfly

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.*
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.roundStroke
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.gfx.toClosedPath
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.hash01 as seededHash01
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.triangulation.Delaunator
import dev.oblac.gart.triangulation.delaunayToVoronoi
import org.jetbrains.skia.*
import org.jetbrains.skia.Shader.Companion.makeRadialGradient
import kotlin.math.*

/**
 * butterfly wing in macro. each scale is a voronoi cell.
 */
private const val W = 1280
private const val H = 1280
private const val TAU = 6.2831855f
private val DIAG = hypot(W.toFloat(), H.toFloat())   // wave span: corner-to-corner

private val OUT = ps("out", "butterfly")
private val SEED = pi("seed", 7)

// layout
private val CELL = pf("cell", 46f)            // base (smallest) scale size
private val WARP = pf("warp", 0.62f)          // warp amplitude as a fraction of a cell (organic flow)
private val WARPFREQ = pf("warpfreq", 1.7f)   // warp field frequency (1 ~ drift over 4 cells)
// cell size varies by a noise field: dense small cells (~CELL) <-> sparse big cells
private val SIZEAMP = pf("sizeamp", 1.1f)     // 0 = uniform CELL; >0 = big regions up to CELL*(1+sizeamp)
private val SIZEFREQ = pf("sizefreq", 2.0f)   // size-field cycles across the width (low = big smooth regions)
private val SIZEBIAS = pf("sizebias", 2.0f)   // >1 keeps most cells small (current); big cells only in peaks

// growth / imbrication
private val GROW = pf("grow", 0.82f)          // bottom-right growth (fraction of vertex radius)
private val DIRDEG = pf("dirdeg", 45f)        // growth direction in degrees (45 = bottom-right, y down)

// per-cell eyespot
private val BORDER = pf("border", 2.4f)       // dark border stroke width (logical px)
private val REDR = pf("redr", 0.22f)          // pupil radius (fraction of cell radius) - the wave's midpoint
private val MIDR = pf("midr", 0.66f)          // middle-colour zone end (fraction)
private val RIMDARK = pf("rimdark", 0.66f)    // inner rim shading: amount of mid pushed toward black (1 = black)
private val BORDERDARK = pf("borderdark", 0.90f) // border stroke darkness: amount toward black (1 = black)
private val EYEOFF = pf("eyeoff", 0.18f)      // pupil offset toward top-left (the lit look)
private val RIM = pf("rim", 0.10f)            // soft sheen highlight strength (top-left)
private val CATCH = pf("catch", 0.0f)         // tiny bright catchlight in the pupil (sells a dark eye)

// the pupil radius is not uniform - it ripples across the image as two crossing sine waves (2D interference)
private val PUPILWAVE = pf("pupilwave", 0.13f) // wave amplitude (0 = constant pupil = old behaviour)
private val PUPILFREQ = pf("pupilfreq", 1.3f)  // first wave: cycles corner-to-corner (low = slow spread)
private val PUPILFREQ2 = pf("pupilfreq2", 1.6f) // second (perpendicular) wave freq -> 2D moiré
private val PUPILANG = pf("pupilang", 45f)      // first wave direction (degrees); second is +90
private val PUPILPHASE = pf("pupilphase", 0f)  // wave phase offset

// smudge mode: instead of the clean radial eyespot, draw each as a painterly smudge -
// pick 4-6 random points on a circle and connect them with thick semi-transparent lines
private val SMUDGE = pf("smudge", 0.0f)       // 0 = off (clean gradient); >0 = smudge strength
private val SMUDGEW = pf("smudgew", 0.4f)     // smudge line thickness as a fraction of cell radius

// extra per-scale detail (composable; set any to 0 to disable)
private val STRIA = pf("stria", 0.55f)        // fine parallel ridge striations across each scale (kept on)
private val STRIAGAP = pf("striagap", 0.16f)  // striation spacing as a fraction of cell radius
private val SERR = pf("serr", 0.0f)           // serrate the exposed bottom-right edge (tooth depth, frac of radius)
private val SERRGAP = pf("serrgap", 0.3f)     // serration tooth spacing as a fraction of cell radius
private val RIMLIGHT = pf("rimlight", 0.0f)   // bright rim-light on the top-left lapping edges

// second scale layer: a smaller "cover scale" offset top-left on top of each cell (ground + cover)
private val LAYER = pf("layer", 0.0f)            // cover-scale opacity / presence (0 = off)
private val LAYERSCALE = pf("layerscale", 0.66f) // cover size as a fraction of the cell
private val LAYEROFF = pf("layeroff", 0.24f)     // cover offset toward top-left (fraction of radius)

// imbrication shadow: each scale casts onto the bottom-right neighbour it laps over
private val SHADOW = pf("shadow", 0.72f)      // cast shadow strength (0 = flat)
private val SHADOWOFF = pf("shadowoff", 5.2f) // shadow offset toward bottom-right (logical px)
private val SHADOWBLUR = pf("shadowblur", 2.5f)

// colour
private val PAL = pi("pal", 2)                // 0 morpho / 1 monarch / 2 peacock / 3 rose (used when cool=0)
private val COOL = pi("cool", 1)             // 0 = built-in pal above; 1..173 = gart Cool palette N
private val COLORFREQ = pf("colorfreq", 1.2f) // band field frequency (cycles across the width)
private val CORE = ps("core", "black")        // pupil colour: crimson|black|ink|wine|self|RRGGBB(hex)
private val REDVAR = pf("redvar", 0.22f)      // per-cell core brightness variation

// post
private val VIG = pf("vig", 0.55f)
private val BLOOM = pf("bloom", 0.0f)
private val GRAIN = pf("grain", 0.03f)
private val SS = pi("ss", 2)

private val FW = W * SS
private val FH = H * SS

private val OFF = SEED * 1000f                 // seed shifts the simplex sample window
private val DR = run {                          // unit growth direction
    val a = Math.toRadians(DIRDEG.toDouble())
    Point(cos(a).toFloat(), sin(a).toFloat())
}
private val CORE_SELF = CORE.equals("self", true)            // pupil = a very dark version of the cell's own mid
private val CORE_RGB = resolveCore(CORE)                      // resolved pupil colour (unused when CORE_SELF)

/** pupil radius fraction for a cell at [p] - rippled across the image by two crossing sine waves (2D). */
private fun pupilFrac(p: Point): Float {
    val base = REDR.coerceIn(0.02f, 0.7f)
    if (PUPILWAVE <= 0f) return base
    val wave = (pupilWaveAt(p, PUPILANG, PUPILFREQ) + pupilWaveAt(p, PUPILANG + 90f, PUPILFREQ2)) * 0.5f
    return (base + PUPILWAVE * wave).coerceIn(0.03f, 0.85f)
}

private fun pupilWaveAt(p: Point, angDeg: Float, freq: Float): Float {
    val a = Math.toRadians(angDeg.toDouble())
    val proj = (p.x * cos(a).toFloat() + p.y * sin(a).toFloat()) / DIAG   // 0..1 along this direction
    return sin(proj * TAU * freq + PUPILPHASE)
}

/** the held mid-colour band for an eyespot whose pupil ends at [pupil]: returns (midStart, midEnd). */
private fun midBand(pupil: Float): Pair<Float, Float> {
    val midStart = (pupil + 0.06f).coerceAtMost(0.85f)
    return midStart to MIDR.coerceIn(midStart + 0.04f, 0.95f)
}

private fun resolveCore(s: String): Int = when (s.lowercase()) {
    "crimson", "red" -> c3(206, 32, 38)
    "black" -> c3(12, 12, 16)
    "ink" -> c3(14, 20, 32)                                   // near-black with a cool tint
    "wine" -> c3(70, 14, 22)                                  // very dark warm "blackish red"
    "self" -> c3(12, 12, 16)
    else -> s.toIntOrNull(16)?.let { (0xFF shl 24) or (it and 0xFFFFFF) } ?: c3(12, 12, 16)
}

/** band hues per palette; first -> 0, last -> 1 on the ramp. red core is ramp-independent. */
private fun c3(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
private val RAMPS: List<List<Int>> = listOf(
    // 0 morpho - deep indigo -> electric blue -> teal -> cyan-white
    listOf(c3(18, 22, 58), c3(28, 72, 150), c3(40, 150, 205), c3(120, 210, 225), c3(225, 245, 250)),
    // 1 monarch - umber -> burnt orange -> amber -> cream
    listOf(c3(40, 20, 12), c3(150, 60, 18), c3(225, 120, 28), c3(240, 175, 60), c3(250, 225, 150)),
    // 2 peacock - teal -> emerald -> gold -> violet
    listOf(c3(10, 40, 46), c3(18, 95, 90), c3(40, 160, 120), c3(190, 175, 60), c3(150, 90, 175)),
    // 3 rose - plum -> magenta -> rose -> cream
    listOf(c3(50, 18, 42), c3(150, 40, 110), c3(210, 90, 150), c3(225, 160, 200), c3(245, 225, 230)),
)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("butterfly", W, H)
    println(gart)
    val g = gart.gartvas()

    val t0 = System.currentTimeMillis()
    val scales = buildScales()
    println("scales: ${scales.size} in ${System.currentTimeMillis() - t0}ms")

    // render the whole field supersampled, then mitchell-downscale (lots of thin strokes)
    val big = Gartvas(Dimension(FW, FH))
    renderScene(big.canvas, scales)
    val img = big.snapshot()
    g.canvas.drawImageRect(
        img,
        Rect(0f, 0f, FW.toFloat(), FH.toFloat()),
        Rect(0f, 0f, W.toFloat(), H.toFloat()),
        SamplingMode.MITCHELL, null, true
    )

    // post
    if (BLOOM > 0f) drawBloom(g.canvas, g.snapshot())
    if (VIG > 0f) drawVignette(g.canvas)
    if (GRAIN > 0f) addGrain(g)

    gart.saveImage(g, "$OUT.png")
    if (!headless) gart.window().showImage(g)
}

// GEOMETRY

private class Scale(
    val poly: List<Point>,   // grown polygon, logical coords
    val eye: Point,          // gradient + sheen centre
    val radius: Float,       // farthest vertex from eye
    val mid: Int,            // middle colour (from the band field)
    val border: Int,         // dark border stroke colour
    val core: Int,           // pupil colour
    val pupil: Float,        // pupil radius fraction (rippled across the image)
    val order: Float,        // site.x + site.y -> paint order
)

/** desired local scale size at [x],[y]: a low-freq noise field -> dense small cells <-> sparse big cells. */
private fun cellSize(x: Float, y: Float): Float {
    if (SIZEAMP <= 0f) return CELL
    val f01 = ((snf(x, y, SIZEFREQ / W.toFloat(), 333.7f) + 1f) * 0.5f).coerceIn(0f, 1f)
    val biased = f01.pow(SIZEBIAS)     // keep most of the field at the small (current) size; big only in peaks
    return CELL * (1f + SIZEAMP * biased)
}

/**
 * variable-density blue-noise sites: walk a fine candidate grid in fixed order (deterministic),
 * warp each candidate for organic flow, and greedily accept it only if no kept site is within the
 * locally-desired spacing cellSize(). where the size field is small we keep dense points (small
 * cells ~CELL); where it's large we keep sparse points (big cells). a bucket hash keeps it fast.
 */
private fun generateSites(): List<Point> {
    val margin = CELL * 3f
    val step = CELL * 0.45f
    val fw = WARPFREQ / (4f * CELL)
    val pack = 0.9f
    val b = CELL * (1f + max(0f, SIZEAMP))        // bucket size >= max query radius
    val buckets = HashMap<Long, ArrayList<Point>>()
    fun key(bx: Int, by: Int) = (bx.toLong() shl 32) xor (by.toLong() and 0xffffffffL)
    fun tooClose(x: Float, y: Float, r: Float): Boolean {
        val bx = floor(x / b).toInt(); val by = floor(y / b).toInt()
        val r2 = r * r
        for (oy in -1..1) for (ox in -1..1) {
            val lst = buckets[key(bx + ox, by + oy)] ?: continue
            for (q in lst) { val dx = q.x - x; val dy = q.y - y; if (dx * dx + dy * dy < r2) return true }
        }
        return false
    }
    val pts = ArrayList<Point>()
    var row = 0
    var gy = -margin
    while (gy <= H + margin) {
        var col = 0
        var gx = -margin
        while (gx <= W + margin) {
            val cx = gx + (hash01(row, col, 1) - 0.5f) * step
            val cy = gy + (hash01(row, col, 2) - 0.5f) * step
            val x = cx + snf(cx, cy, fw, 0f) * WARP * CELL
            val y = cy + snf(cx, cy, fw, 97.31f) * WARP * CELL
            if (!tooClose(x, y, cellSize(x, y) * pack)) {
                val p = Point(x, y)
                pts.add(p)
                buckets.getOrPut(key(floor(x / b).toInt(), floor(y / b).toInt())) { ArrayList() }.add(p)
            }
            gx += step; col++
        }
        gy += step; row++
    }
    return pts
}

private fun buildScales(): List<Scale> {
    val pts = generateSites()
    val voronoi = delaunayToVoronoi(Delaunator(pts).triangles())
    val ramp = ramp()
    val fc = COLORFREQ / W.toFloat()

    val scales = ArrayList<Scale>(voronoi.size)
    for (cell in voronoi) {
        val s = cell.site
        if (s.x < -CELL || s.x > W + CELL || s.y < -CELL || s.y > H + CELL) continue
        val raw = cell.toPathPoints()
        if (raw.size < 3) continue

        // directional grow: push only the bottom-right flank outward from the site
        val grown = raw.map { v ->
            val dx = v.x - s.x; val dy = v.y - s.y
            val len = hypot(dx, dy)
            if (len < 1e-3f) v else {
                val ux = dx / len; val uy = dy / len
                val w = max(0f, ux * DR.x + uy * DR.y)
                val push = GROW * len * w
                Point(s.x + ux * (len + push), s.y + uy * (len + push))
            }
        }
        val rmax = grown.maxOf { hypot(it.x - s.x, it.y - s.y) }
        val eye = Point(s.x - DR.x * EYEOFF * rmax, s.y - DR.y * EYEOFF * rmax)
        val radius = grown.maxOf { hypot(it.x - eye.x, it.y - eye.y) }

        val t = ((snf(s.x, s.y, fc, 211.7f) + 1f) * 0.5f).coerceIn(0f, 1f)
        val mid = ramp.colorAt(t)
        scales.add(Scale(grown, eye, radius, mid, darken(mid, BORDERDARK), varyCore(mid, s), pupilFrac(s), s.x + s.y))
    }
    // bottom-right first, top-left painted last (on top) -> imbrication
    scales.sortByDescending { it.order }
    return scales
}

// RENDER

private fun renderScene(c: Canvas, scales: List<Scale>) {
    c.scale(SS.toFloat(), SS.toFloat())          // draw in logical W x H coords
    c.clear(c3(8, 8, 12))                          // dark ground fills any sliver, matches borders

    for (sc in scales) {
        val poly = if (SERR > 0f) serrate(sc.poly, sc.eye, sc.radius) else sc.poly
        val path = poly.toClosedPath()
        // cast shadow onto the bottom-right neighbour this scale laps over -> shingle depth
        if (SHADOW > 0f) {
            val sp = poly.map { Point(it.x + DR.x * SHADOWOFF, it.y + DR.y * SHADOWOFF) }.toClosedPath()
            c.drawPath(sp, Paint().apply {
                isAntiAlias = true
                color = (SHADOW.coerceIn(0f, 1f) * 170).toInt() shl 24    // black, alpha only
                if (SHADOWBLUR > 0f) imageFilter = ImageFilter.makeBlur(SHADOWBLUR, SHADOWBLUR, FilterTileMode.CLAMP)
            })
        }
        if (SMUDGE > 0f) {
            drawSmudge(c, sc, path)
        } else {
            val rimCol = darken(sc.mid, RIMDARK)
            val (midStart, midEnd) = midBand(sc.pupil)
            c.drawPath(path, Paint().apply {
                isAntiAlias = true
                shader = makeRadialGradient(
                    sc.eye.x, sc.eye.y, max(1f, sc.radius),
                    gradientOf(
                        intArrayOf(sc.core, sc.core, sc.mid, sc.mid, rimCol),
                        floatArrayOf(0f, sc.pupil, midStart, midEnd, 1f)
                    )
                )
            })
            // soft sheen toward the top-left (light from upper-left), clipped to the scale
            if (RIM > 0f) {
                val hr = max(1f, sc.radius * 0.55f)
                val hx = sc.eye.x - DR.x * 0.22f * sc.radius
                val hy = sc.eye.y - DR.y * 0.22f * sc.radius
                val tint = lighten(sc.mid, 0.6f) and 0xFFFFFF
                val hi = ((RIM.coerceIn(0f, 1f) * 150).toInt() shl 24) or tint
                c.save()
                c.clipPath(path)
                c.drawCircle(hx, hy, hr, Paint().apply {
                    isAntiAlias = true
                    shader = makeRadialGradient(hx, hy, hr, gradientOf(intArrayOf(hi, tint), floatArrayOf(0f, 1f)))
                })
                c.restore()
            }
            // tiny catchlight glint in the pupil - makes a dark eye read as an eye
            if (CATCH > 0f) {
                val cr = max(0.6f, sc.radius * REDR * 0.5f)
                val cx = sc.eye.x - DR.x * sc.radius * REDR * 0.35f
                val cy = sc.eye.y - DR.y * sc.radius * REDR * 0.35f
                val hi = ((CATCH.coerceIn(0f, 1f) * 235).toInt() shl 24) or 0xEFF3FF
                c.drawCircle(cx, cy, cr, Paint().apply {
                    isAntiAlias = true
                    shader = makeRadialGradient(cx, cy, cr, gradientOf(intArrayOf(hi, 0x00EFF3FF), floatArrayOf(0f, 1f)))
                })
            }
        }
        if (STRIA > 0f) drawStriations(c, sc.eye, sc.radius, sc.mid, path)
        // crisp dark border - this is what separates the overlapping scales
        c.drawPath(path, strokeOf(sc.border, BORDER).apply { isAntiAlias = true })
        if (RIMLIGHT > 0f) drawRimLight(c, sc, poly)
        // upper cover scale on top (ground + cover layering)
        if (LAYER > 0f) drawCoverScale(c, sc)
    }
}

// smudge

/**
 * paint the eyespot as a smudge instead of a clean gradient: pick 4-6 random points on a circle
 * around the eye and connect them with thick, semi-transparent round strokes. the overlapping
 * translucent lines build up unevenly into a smeared dab of pigment. two passes: the mid colour
 * outer smear, then a darker pupil smear on top. clipped to the cell so it reads as one scale.
 */
private fun drawSmudge(c: Canvas, sc: Scale, path: Path) {
    val s = SMUDGE.coerceIn(0f, 1f)
    val sx = sc.eye.x.toInt(); val sy = sc.eye.y.toInt()
    val n = 4 + (hash01(sx, sy, 11) * 3f).toInt()        // 4..6 points
    c.save()
    c.clipPath(path)
    c.drawPath(path, fillOf(sc.mid))                                 // keep the bright band colour as the scale
    // darker halo smear, then a dark pupil smear on top - the eyespot as a smudge of thick lines
    smudgeBlob(c, sc.eye, sc.radius * 0.95f, n, sx, sy, 20, darken(sc.mid, 0.5f), (s * 95).toInt(), sc.radius * SMUDGEW)
    smudgeBlob(c, sc.eye, sc.radius * 0.50f, n, sx, sy, 60, sc.core, (s * 175).toInt(), sc.radius * SMUDGEW * 0.8f)
    c.restore()
}

private fun smudgeBlob(c: Canvas, center: Point, rad: Float, n: Int, sx: Int, sy: Int, salt: Int, color: Int, alpha: Int, width: Float) {
    val pts = ArrayList<Point>(n)
    for (i in 0 until n) {
        val ang = (i / n.toFloat()) * TAU + (hash01(sx, sy, salt + i) - 0.5f) * 1.3f
        val rr = rad * (0.55f + hash01(sx, sy, salt + 30 + i) * 0.55f)
        pts.add(Point(center.x + cos(ang) * rr, center.y + sin(ang) * rr))
    }
    val col = (alpha.coerceIn(0, 255) shl 24) or (color and 0xFFFFFF)
    c.drawPath(pts.toClosedPath(), strokeOf(col, max(1f, width)).roundStroke())
}

// EXTRA SCALE DETAIL

/** fine parallel ridge lines along the scale's length - the signature macro-butterfly microstructure. */
private fun drawStriations(c: Canvas, eye: Point, radius: Float, mid: Int, path: Path) {
    val sx = eye.x.toInt(); val sy = eye.y.toInt()
    // each scale tilts its ridges a touch off the growth axis so the field isn't mechanically parallel
    val tilt = (hash01(sx, sy, 71) - 0.5f) * 0.7f
    val ca = cos(tilt); val sa = sin(tilt)
    val dx = DR.x * ca - DR.y * sa; val dy = DR.x * sa + DR.y * ca    // along-scale
    val nx = -dy; val ny = dx                                          // across-scale
    val gap = max(1.2f, radius * STRIAGAP)
    val half = radius * 1.25f
    val k = (radius / gap).toInt()
    val col = ((STRIA.coerceIn(0f, 1f) * 70).toInt() shl 24) or (darken(mid, 0.45f) and 0xFFFFFF)
    val paint = strokeOf(col, max(0.6f, radius * 0.018f)).apply { isAntiAlias = true }
    c.save()
    c.clipPath(path)
    for (i in -k..k) {
        val off = i * gap + (hash01(sx, sy, 80 + i + k) - 0.5f) * gap * 0.3f
        val px = eye.x + nx * off; val py = eye.y + ny * off
        c.drawLine(px - dx * half, py - dy * half, px + dx * half, py + dy * half, paint)
    }
    c.restore()
}

/**
 * the upper "cover scale": a smaller scale offset toward the top-left, sitting on the ground scale
 * with its own drop shadow, a brighter mid colour, its own pupil, ridges and dark border. butterfly
 * wings stack two scale layers (ground + cover) - this is what gives the surface its depth.
 */
private fun drawCoverScale(c: Canvas, sc: Scale) {
    val cs = LAYERSCALE.coerceIn(0.3f, 0.95f)
    val cc = Point(sc.eye.x - DR.x * LAYEROFF * sc.radius, sc.eye.y - DR.y * LAYEROFF * sc.radius)
    val cover = sc.poly.map { Point(cc.x + (it.x - cc.x) * cs, cc.y + (it.y - cc.y) * cs) }
    val path = cover.toClosedPath()
    val cr = max(1f, sc.radius * cs)
    val a = (LAYER.coerceIn(0f, 1f) * 255).toInt()
    val coverMid = lighten(sc.mid, 0.18f)
    // drop shadow toward bottom-right so the cover floats above the ground scale
    if (SHADOW > 0f) {
        val sp = cover.map { Point(it.x + DR.x * SHADOWOFF * 0.7f, it.y + DR.y * SHADOWOFF * 0.7f) }.toClosedPath()
        c.drawPath(sp, Paint().apply {
            isAntiAlias = true
            color = (SHADOW.coerceIn(0f, 1f) * 130).toInt() shl 24
            if (SHADOWBLUR > 0f) imageFilter = ImageFilter.makeBlur(SHADOWBLUR, SHADOWBLUR, FilterTileMode.CLAMP)
        })
    }
    // cover fill: same pupil -> brighter mid band -> dark rim, slightly translucent
    val (midStart, midEnd) = midBand(sc.pupil)
    c.drawPath(path, Paint().apply {
        isAntiAlias = true
        alpha = a
        shader = makeRadialGradient(
            cc.x, cc.y, cr,
            gradientOf(
                intArrayOf(sc.core, sc.core, coverMid, coverMid, darken(coverMid, RIMDARK)),
                floatArrayOf(0f, sc.pupil, midStart, midEnd, 1f)
            )
        )
    })
    if (STRIA > 0f) drawStriations(c, cc, cr, coverMid, path)
    c.drawPath(path, strokeOf(darken(coverMid, BORDERDARK), BORDER).apply { isAntiAlias = true })
}

/** bright catch on the top-left lapping edges - light from upper-left grazing the raised shingle. */
private fun drawRimLight(c: Canvas, sc: Scale, poly: List<Point>) {
    val col = ((RIMLIGHT.coerceIn(0f, 1f) * 200).toInt() shl 24) or (lighten(sc.mid, 0.6f) and 0xFFFFFF)
    val paint = strokeOf(col, max(1f, BORDER * 0.8f)).roundStroke()
    val n = poly.size
    for (i in 0 until n) {
        val a = poly[i]; val b = poly[(i + 1) % n]
        val mx = (a.x + b.x) * 0.5f - sc.eye.x; val my = (a.y + b.y) * 0.5f - sc.eye.y
        val len = hypot(mx, my)
        if (len < 1e-3f) continue
        if ((mx / len) * DR.x + (my / len) * DR.y < -0.15f) c.drawLine(a.x, a.y, b.x, b.y, paint)
    }
}

/** notch the exposed (bottom-right facing) edges into a comb of little teeth, like a real scale tip. */
private fun serrate(poly: List<Point>, eye: Point, radius: Float): List<Point> {
    val depth = radius * SERR
    val gap = max(2f, radius * SERRGAP)
    val n = poly.size
    val out = ArrayList<Point>(n * 3)
    for (i in 0 until n) {
        val a = poly[i]; val b = poly[(i + 1) % n]
        out.add(a)
        val ex = b.x - a.x; val ey = b.y - a.y
        val len = hypot(ex, ey)
        val mx = (a.x + b.x) * 0.5f - eye.x; val my = (a.y + b.y) * 0.5f - eye.y
        val ml = hypot(mx, my)
        val faces = ml > 1e-3f && ((mx / ml) * DR.x + (my / ml) * DR.y) > 0.2f
        if (!faces || len < gap * 1.3f) continue
        val teeth = (len / gap).toInt().coerceIn(1, 12)
        var nrmx = -ey / len; var nrmy = ex / len
        if (nrmx * mx + nrmy * my < 0f) { nrmx = -nrmx; nrmy = -nrmy }   // outward
        for (t in 0 until teeth) {
            val tip = (t + 0.5f) / teeth
            out.add(Point(a.x + ex * tip + nrmx * depth, a.y + ey * tip + nrmy * depth))
            if (t < teeth - 1) {
                val v = (t + 1f) / teeth
                out.add(Point(a.x + ex * v, a.y + ey * v))
            }
        }
    }
    return out
}

// COLOUR HELPERS

private fun ramp(): ColorRamp {
    if (COOL in 1..173) return ColorRamp.of(Palettes.coolPalette(COOL))
    val cols = RAMPS[PAL.coerceIn(0, RAMPS.size - 1)]
    val denom = (cols.size - 1).toFloat()
    return ColorRamp(cols.mapIndexed { i, col -> ColorStop(col, i / denom) })
}

private fun varyCore(mid: Int, s: Point): Int {
    val base = if (CORE_SELF) darken(mid, 0.88f) else CORE_RGB
    val f = 1f + (hash01(s.x.toInt(), s.y.toInt(), 7) - 0.5f) * REDVAR
    val r = ((base ushr 16 and 0xFF) * f).toInt().coerceIn(0, 255)
    val g = ((base ushr 8 and 0xFF) * f).toInt().coerceIn(0, 255)
    val b = ((base and 0xFF) * f).toInt().coerceIn(0, 255)
    return c3(r, g, b)
}

// NOISE / HASH`

/** deterministic simplex sample at frequency [freq]; [tag] picks an independent field. */
private fun snf(x: Float, y: Float, freq: Float, tag: Float): Float {
    val o = OFF + tag
    return SimplexNoise.noise((x * freq + o).toDouble(), (y * freq + o).toDouble()).toFloat()
}

/** integer hash keyed by SEED; stable per-cell pseudo-random in [0,1). */
private fun hash01(a: Int, b: Int, k: Int): Float = seededHash01(a, b, k, SEED)

// POST ----------------------------------------------------------------------

private fun drawBloom(c: Canvas, img: Image) {
    bloomOctave(c, img, 4f, 0.5f * BLOOM, BlendMode.SCREEN)
    bloomOctave(c, img, 10f, 0.3f * BLOOM, BlendMode.PLUS)
    bloomOctave(c, img, 22f, 0.18f * BLOOM, BlendMode.PLUS)
}

private fun bloomOctave(c: Canvas, img: Image, sigma: Float, strength: Float, blend: BlendMode) {
    c.drawImage(img, 0f, 0f, Paint().apply {
        imageFilter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP)
        blendMode = blend
        alpha = (strength.coerceIn(0f, 1f) * 255).toInt()
    })
}

private fun drawVignette(c: Canvas) {
    c.drawPaint(Paint().apply {
        shader = makeRadialGradient(
            W * 0.5f, H * 0.5f, W * 0.72f,
            gradientOf(
                intArrayOf(0x00000000, 0x00000000, ((VIG.coerceIn(0f, 1f) * 210).toInt() shl 24) or 0x05060A),
                floatArrayOf(0f, 0.55f, 1f)
            )
        )
    })
}

private fun addGrain(g: Gartvas) {
    val map = Gartmap(g)
    val px = map.pixels
    val amp = GRAIN.coerceIn(0f, 1f) * 40f
    val w = g.d.w
    for (idx in px.indices) {
        val n = (hash01(idx % w, idx / w, 9) - 0.5f) * 2f * amp
        val col = px[idx]
        val r = ((col ushr 16 and 0xFF) + n).toInt().coerceIn(0, 255)
        val gg = ((col ushr 8 and 0xFF) + n).toInt().coerceIn(0, 255)
        val b = ((col and 0xFF) + n).toInt().coerceIn(0, 255)
        px[idx] = c3(r, gg, b)
    }
    map.drawToCanvas(g)
}
