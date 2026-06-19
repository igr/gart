package work.areola

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.gfx.closedPathOf
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.map
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.PoissonDiskSamplingNoise
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.triangulation.Delaunator
import dev.oblac.gart.triangulation.VoronoiCell
import dev.oblac.gart.triangulation.delaunayToVoronoi
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point
import org.jetbrains.skia.Shader.Companion.makeRadialGradient
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/**
 * AREOLA
 *
 * The membrane cells between wing veins - basically the inverse of nervure (that
 * drew the veins, this draws the gaps inbetween). Throw down a load of poisson
 * points, thin them out with a noise field so cell sizes vary, then lloyd relax
 * a couple times into a foam. Each cell gets a pale pearly fill + a thin dark
 * outline. delicate iridescent biology stuff.
 *
 * family: rugae, nervure, corona, areola.
 */

private const val W = 1200
private const val H = 1200
private val OUT = System.getProperty("out") ?: "work/areola.png"
private val DEFAULT_SEED = 5L                    // locked: richest iridescent hue split, fine-centre composition

private val GROUND = 0xFF080A12.toInt()          // deep iridescent near-black
private val GROUND_WASH = 0xFF161A2C.toInt()     // subtle blue-violet centre lift

// seeds
private const val POISSON_MINDIST = 15.0         // dense base spacing (smaller -> more, tinier cells)
private const val REJECTION = 20
private const val KEEP_MIN = 0.09f               // keep-probability where the density field is low (large cells)
private const val KEEP_MAX = 1.0f                // keep-probability where high (tiny cells)
private const val DENSITY_SCALE = 0.0024         // simplex frequency for the density field
private const val GHOST_MARGIN = 130f            // ghost ring sits this far outside the frame
private const val GHOST_STEP = 55f               // spacing of ghost seeds along the ring

private const val LLOYD_ITERS = 2                // centroidal Voronoi relaxation passes (low = keep organic size contrast)

// render
private const val GRAD_STEPS = 512
private const val HUE_SCALE = 0.0011             // simplex frequency for the colour field
private const val GRADIENT_TILT = 0.35f          // slow positional hue drift across the frame
private const val JITTER = 0.06f                 // per-cell lightness shimmer
private const val VEIN_MIN_W = 0.5f
private const val VEIN_MAX_W = 2.2f
private const val AREA_LO = 60f                  // cell area (px^2) mapping to VEIN_MIN_W
private const val AREA_HI = 1400f                // cell area mapping to VEIN_MAX_W
private val VEIN_COLOR = 0xFF0A0D18.toInt()      // deep iridescent dark (wing membrane)

/** pale mother of pearl colours, ice-blue thru to pink. */
private val NACRE = Palette(
    0xFFBFE3F2L,
    0xFF8FD3D6L,
    0xFFA9DCC9L,
    0xFFB9C4F0L,
    0xFFCBB6E8L,
    0xFFE4C9E6L,
)

// finish
private val SHEEN_COLOR = 0x4CEAF2FFL.toInt()    // pale cool highlight (alpha in high byte)
private const val BLOOM_SIGMA = 7f
private val VIGNETTE_EDGE = 0x99060810.toInt()

fun main() {
    val headless = System.getProperty("headless") != null

    val gart = Gart.of("areola", W, H)
    println(gart)

    val g = gart.gartvas()
    val c = g.canvas

    val seed = System.getProperty("seed")?.toLong() ?: DEFAULT_SEED
    val rng = Random(seed)
    val seeds = seedPoints(rng)
    val ghosts = ghostRing()
    println("seeds: ${seeds.size}, ghosts: ${ghosts.size}")

    val t0 = System.currentTimeMillis()
    val sites = relax(seeds, ghosts)
    val cells = voronoiCells(sites + ghosts)
    val drawn = buildCells(sites, cells)
    println("cells: ${drawn.size}/${sites.size} in ${System.currentTimeMillis() - t0}ms")

    drawGround(c)
    drawFills(c, drawn)
    drawSheen(c)
    drawBloom(c, g)
    drawVeins(c, drawn)
    drawVignette(c)

    gart.saveImage(g, OUT)
    if (!headless) gart.window().showImage(g)
}

// SEEDS

private fun seedPoints(rng: Random): List<Point> {
    val poisson = PoissonDiskSamplingNoise(rng.nextLong())
    val pts = poisson.generate(0.0, 0.0, W.toDouble(), H.toDouble(), POISSON_MINDIST, REJECTION)
    val kept = ArrayList<Point>(pts.size)
    for (p in pts) {
        val n = map(SimplexNoise.noise(p.x * DENSITY_SCALE, p.y * DENSITY_SCALE), -1, 1, 0, 1)
        if (rng.rndf() < lerp(KEEP_MIN, KEEP_MAX, n)) kept.add(p)
    }
    return kept
}

/** ring of fake seeds outside the frame so the real cells dont run off the edge. */
private fun ghostRing(): List<Point> {
    val ring = ArrayList<Point>()
    var x = -GHOST_MARGIN
    while (x <= W + GHOST_MARGIN) {
        ring.add(Point(x, -GHOST_MARGIN)); ring.add(Point(x, H + GHOST_MARGIN)); x += GHOST_STEP
    }
    var y = -GHOST_MARGIN
    while (y <= H + GHOST_MARGIN) {
        ring.add(Point(-GHOST_MARGIN, y)); ring.add(Point(W + GHOST_MARGIN, y)); y += GHOST_STEP
    }
    return ring
}

// TESSELLATION

/** drop the dup last point if the poly closes on itself. */
private fun cleanPoly(poly: List<Point>): List<Point> =
    if (poly.size >= 2 && poly.first() == poly.last()) poly.subList(0, poly.size - 1) else poly

private fun polygonArea(poly: List<Point>): Float {
    var a = 0f
    for (i in poly.indices) {
        val p = poly[i]; val q = poly[(i + 1) % poly.size]
        a += p.x * q.y - q.x * p.y
    }
    return abs(a) * 0.5f
}

private fun polygonCentroid(poly: List<Point>): Point {
    var a = 0f; var cx = 0f; var cy = 0f
    for (i in poly.indices) {
        val p = poly[i]; val q = poly[(i + 1) % poly.size]
        val cross = p.x * q.y - q.x * p.y
        a += cross; cx += (p.x + q.x) * cross; cy += (p.y + q.y) * cross
    }
    a *= 0.5f
    if (abs(a) < 1e-4f) {                         // degenerate -> vertex average
        var sx = 0f; var sy = 0f
        for (p in poly) { sx += p.x; sy += p.y }
        return Point(sx / poly.size, sy / poly.size)
    }
    return Point(cx / (6f * a), cy / (6f * a))
}

private fun voronoiCells(all: List<Point>): Map<Point, VoronoiCell> {
    val tris = Delaunator(all).triangles()
    val cells = delaunayToVoronoi(tris)
    val bySite = HashMap<Point, VoronoiCell>(cells.size * 2)
    for (cell in cells) bySite[cell.site] = cell
    return bySite
}

/** lloyd relax - shove each seed to its cell centroid a few times. ghosts dont move. */
private fun relax(sites: List<Point>, ghosts: List<Point>): List<Point> {
    var current = sites
    repeat(LLOYD_ITERS) {
        val cells = voronoiCells(current + ghosts)
        val moved = ArrayList<Point>(current.size)
        for (s in current) {
            val cell = cells[s]
            val poly = cell?.let { runCatching { cleanPoly(it.toPathPoints()) }.getOrNull() }
            if (poly == null || poly.size < 3) { moved.add(s); continue }
            val cen = polygonCentroid(poly)
            moved.add(Point(cen.x.coerceIn(0f, W.toFloat()), cen.y.coerceIn(0f, H.toFloat())))
        }
        current = moved
    }
    return current
}

private class Cell(val poly: List<Point>, val centroid: Point, val area: Float)

private fun buildCells(sites: List<Point>, cells: Map<Point, VoronoiCell>): List<Cell> {
    val out = ArrayList<Cell>(sites.size)
    for (s in sites) {
        val poly = cells[s]?.let { runCatching { cleanPoly(it.toPathPoints()) }.getOrNull() } ?: continue
        if (poly.size < 3) continue
        out.add(Cell(poly, polygonCentroid(poly), polygonArea(poly)))
    }
    return out
}

/** cheap deterministic rng from a points x/y. */
private fun hashFloat(p: Point): Float {
    val s = sin(p.x * 127.1f + p.y * 311.7f) * 43758.5453f
    return s - floor(s)
}

// RENDER

private fun drawGround(c: Canvas) {
    c.clear(GROUND)
    c.drawPaint(Paint().apply {
        shader = makeRadialGradient(
            W * 0.42f, H * 0.38f, W * 0.85f,
            gradientOf(
                intArrayOf(GROUND_WASH, GROUND, GROUND),
                floatArrayOf(0f, 0.6f, 1f)
            )
        )
    })
}

private fun drawFills(c: Canvas, cells: List<Cell>) {
    val ramp = NACRE.expand(GRAD_STEPS)
    for (cell in cells) {
        val cen = cell.centroid
        val hue = map(SimplexNoise.noise(cen.x * HUE_SCALE, cen.y * HUE_SCALE), -1, 1, 0, 1)
        val tilt = ((cen.x / W + cen.y / H) * 0.5f) * GRADIENT_TILT
        var t = (hue * (1f - GRADIENT_TILT) + tilt)
        t = (t + (hashFloat(cen) - 0.5f) * JITTER).coerceIn(0f, 1f)
        c.drawPath(closedPathOf(cell.poly), fillOf(ramp.safe((t * (GRAD_STEPS - 1)).toInt())))
    }
}

private fun drawVeins(c: Canvas, cells: List<Cell>) {
    for (cell in cells) {
        val w = lerp(VEIN_MIN_W, VEIN_MAX_W, ((cell.area - AREA_LO) / (AREA_HI - AREA_LO)).coerceIn(0f, 1f))
        c.drawPath(closedPathOf(cell.poly), strokeOf(VEIN_COLOR, w))
    }
}

/** one big off-center light sweep, the pearly catch-the-light thing. */
private fun drawSheen(c: Canvas) {
    c.drawPaint(Paint().apply {
        blendMode = BlendMode.SCREEN
        shader = makeRadialGradient(
            W * 0.36f, H * 0.30f, W * 0.95f,
            gradientOf(
                intArrayOf(SHEEN_COLOR, 0x00EAF2FF, 0x00EAF2FF),
                floatArrayOf(0f, 0.55f, 1f)
            )
        )
    })
}

/** soft glow so the palest cells look backlit. */
private fun drawBloom(c: Canvas, g: Gartvas) {
    c.drawImage(g.snapshot(), 0f, 0f, Paint().apply {
        imageFilter = ImageFilter.makeBlur(BLOOM_SIGMA, BLOOM_SIGMA, FilterTileMode.CLAMP)
        blendMode = BlendMode.SCREEN
        alpha = 150
    })
}

private fun drawVignette(c: Canvas) {
    c.drawPaint(Paint().apply {
        shader = makeRadialGradient(
            W * 0.5f, H * 0.5f, W * 0.72f,
            gradientOf(
                intArrayOf(0x00000000, 0x00000000, VIGNETTE_EDGE),
                floatArrayOf(0f, 0.62f, 1f)
            )
        )
    })
}
