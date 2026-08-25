package dev.oblac.gart.brush

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.color.alphaf
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.gfx.simplifyPoints
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.map
import dev.oblac.gart.math.rndGaussian
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.PathFillMode
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * A watercolour wash. The shape is turned into a coarse polygon, its edges are subdivided and
 * pushed about, and twenty-odd translucent copies of it are laid over each other
 * with a faint rim and some of the pigment rubbed out again, so the edge bleeds, the rim darkens
 * and the inside granulates.
 *
 * @property opacity  how much pigment, 0..1
 * @property bleed    how far the edge wanders, 0..1, as a fraction of the edge it grows from
 * @property texture  granulation, 0..1: rubbed-out patches and sparse flecks along the edge
 * @property border   the dark rim where pigment settles at the edge, 0..1
 * @property outward  bleed past the edge (true) or eat into the shape
 * @property angle    direction the wash ran in: the far side thins and bleeds more; null picks a random side
 * @property scatter  the sparse fleck layers at the edge; off gives a cleaner gradient
 * @property layers   coats of pigment; fewer is faster and flatter
 */
data class Watercolor(
    val opacity: Float = 0.6f,
    val bleed: Float = 0.07f,
    val texture: Float = 0.8f,
    val border: Float = 0.5f,
    val outward: Boolean = true,
    val angle: Angle? = null,
    val scatter: Boolean = true,
    val layers: Int = 20,
) {
    init {
        require(opacity in 0f..1f) { "opacity must be in 0..1" }
        require(bleed in 0f..1f) { "bleed must be in 0..1" }
        require(texture in 0f..1f) { "texture must be in 0..1" }
        require(border in 0f..1f) { "border must be in 0..1" }
        require(layers >= 1) { "layers must be >= 1" }
    }
}

/**
 * Washes [path] with [color] as watercolour. The biggest contour is the shape, every other
 * contour is a hole cut out of it (holes bleed too, they just dont get the trimmed and
 * scattered coats).
 *
 * @param blend how the wash lands on what is there; SRC_OVER works on any ground, MULTIPLY glazes on white paper
 */
fun Canvas.drawWatercolor(
    path: Path,
    color: Int,
    watercolor: Watercolor = Watercolor(),
    rnd: Random = Random.Default,
    blend: BlendMode = BlendMode.SRC_OVER,
) {
    val rings = washRings(path, watercolor.bleed)
    if (rings.isEmpty()) return
    WashPainter(this, color, watercolor, rnd, blend, rings).lay()
}

// ---- the base polygon ----

/**
 * The contours of [path] as coarse rings: flattened, simplified so corners survive, then long
 * edges split to a spacing that grows with [bleed]. The edge length is what the lobes grow
 * from, so a wet wash gets a coarse polygon and big lobes, a dry one a fine polygon and a
 * tight, finely frayed edge.
 */
internal fun washRings(path: Path, bleed: Float): List<List<Point>> {
    val fine = flattenRings(path, 1f)
    if (fine.isEmpty()) return emptyList()
    val b = path.bounds
    val size = max(b.width, b.height)
    val spacing = if (bleed < 0.06f) 8f else max(20f, size * lerp(0.15f, 0.45f, (bleed / 0.6f).coerceAtMost(1f)))
    val rings = fine.mapNotNull { ring ->
        val simple = simplifyPoints(ring, 0.75f, closed = true)
        if (simple.size < 3) return@mapNotNull null
        val out = ArrayList<Point>(simple.size * 2)
        for (i in simple.indices) {
            val a = simple[i]
            val c = simple[(i + 1) % simple.size]
            out += a
            val len = hypot(c.x - a.x, c.y - a.y)
            val n = ceil(len / spacing).toInt()
            for (k in 1 until n) {
                val t = k.toFloat() / n
                out += Point(a.x + (c.x - a.x) * t, a.y + (c.y - a.y) * t)
            }
        }
        if (out.size >= 3) out else null
    }
    return rings
}

private fun signedArea(ring: List<Point>): Float {
    var s = 0f
    for (i in ring.indices) {
        val a = ring[i]
        val b = ring[(i + 1) % ring.size]
        s += a.x * b.y - b.x * a.y
    }
    return s / 2f
}

/** One ring of a wash polygon: vertices, per-vertex bleed modifier, and which way its edges bleed. */
private class Ring(val x: FloatArray, val y: FloatArray, val m: FloatArray, val out: BooleanArray) {
    val n get() = x.size
}

/** A wash polygon: its rings plus the (jittered) centre and half-extents of the original shape. */
private class WashPoly(val rings: List<Ring>, val midX: Float, val midY: Float, val sizeX: Float, val sizeY: Float)

private const val DEG5 = 5f * (Math.PI.toFloat() / 180f)

private class WashPainter(
    private val c: Canvas,
    color: Int,
    private val wc: Watercolor,
    private val rnd: Random,
    private val blend: BlendMode,
    rings: List<List<Point>>,
) {
    private val rgb = color and 0xFFFFFF
    private val inkAlpha = alphaf(color)

    // every edge of the original shape, for the parity tests (which side is outside, is a point inside)
    private val sides: FloatArray
    private val minX: Float
    private val minY: Float
    private val maxX: Float
    private val maxY: Float

    // a ring stops subdividing past this, more room the more it bleeds
    private val cap = 2024f * max(0.2f, 2f * wc.bleed)

    private val fill = paint()
    private val rim = paint().apply {
        mode = PaintMode.STROKE
        strokeCap = PaintStrokeCap.ROUND
        strokeJoin = PaintStrokeJoin.ROUND
    }
    private val eraser = paint().apply {
        blendMode = BlendMode.DST_OUT
    }
    private val batch = Paint().apply { blendMode = blend }

    private val base: WashPoly

    init {
        var cnt = 0
        for (r in rings) cnt += r.size
        sides = FloatArray(cnt * 4)
        var k = 0
        var mnx = Float.MAX_VALUE; var mny = Float.MAX_VALUE
        var mxx = -Float.MAX_VALUE; var mxy = -Float.MAX_VALUE
        for (r in rings) for (i in r.indices) {
            val a = r[i]
            val b = r[(i + 1) % r.size]
            sides[k++] = a.x; sides[k++] = a.y; sides[k++] = b.x; sides[k++] = b.y
            if (a.x < mnx) mnx = a.x; if (a.x > mxx) mxx = a.x
            if (a.y < mny) mny = a.y; if (a.y > mxy) mxy = a.y
        }
        minX = mnx; minY = mny; maxX = mxx; maxY = mxy
        base = buildBase(rings)
    }

    // ---- setting up the base polygon ----

    private fun buildBase(unordered: List<List<Point>>): WashPoly {
        // the biggest ring goes first: it is the one that gets trimmed and scattered, and the
        // centre is its centroid. holes only grow, a trimmed hole would paint itself back in
        val rings = unordered.sortedByDescending { abs(signedArea(it)) }
        val (cx, cy) = centroid(rings[0])
        var sizeX = 0f
        var sizeY = 0f
        val built = rings.map { ring ->
            val n = ring.size
            // index 0 ends up furthest against the wash direction, or anywhere
            val shift = wc.angle?.let { startIndex(ring, it) } ?: rnd.nextInt(n)
            val x = FloatArray(n)
            val y = FloatArray(n)
            for (i in 0 until n) {
                val p = ring[(i + shift) % n]
                x[i] = p.x; y[i] = p.y
                sizeX = max(sizeX, abs(cx - p.x))
                sizeY = max(sizeY, abs(cy - p.y))
            }
            // a run of vertices from index 0 bleeds less, so one side of the shape stays firmer
            val wr = rnd.rndf(0f, 75f)
            val fluid = (n * 0.25f * (if (wr < 5f) 1 else if (wr < 15f) 2 else 3)).toInt()
            val m = FloatArray(n) { i -> (if (i > fluid) 1f else 0.3f) * rnd.rndf(0.85f, 1.4f) * wc.bleed }
            val out = BooleanArray(n) { i -> perpIsOutside(x[i], y[i], x[(i + 1) % n], y[(i + 1) % n]) }
            Ring(x, y, m, out)
        }
        return WashPoly(
            built,
            cx + rnd.rndf(-0.6f, 0.6f) * sizeX,
            cy + rnd.rndf(-0.6f, 0.6f) * sizeY,
            sizeX, sizeY,
        )
    }

    private fun startIndex(ring: List<Point>, angle: Angle): Int {
        val dx = cos(angle.radians)
        val dy = sin(angle.radians)
        var best = 0
        var bestDot = Float.MAX_VALUE
        for (i in ring.indices) {
            val d = ring[i].x * dx + ring[i].y * dy
            if (d < bestDot) { bestDot = d; best = i }
        }
        return best
    }

    /**
     * Does the perpendicular `(-sy, sx)` of the edge a→b point out of the shape? A ray from the
     * edge's midpoint that way crosses the rest of the outline an even number of times if so.
     */
    private fun perpIsOutside(ax: Float, ay: Float, bx: Float, by: Float): Boolean {
        val px = (ax + bx) / 2f
        val py = (ay + by) / 2f
        val dx = -(by - ay)
        val dy = bx - ax
        var count = 0
        var k = 0
        while (k < sides.size) {
            val sax = sides[k]; val say = sides[k + 1]; val sbx = sides[k + 2]; val sby = sides[k + 3]
            k += 4
            val ex = sbx - sax
            val ey = sby - say
            val denom = dx * ey - dy * ex
            if (abs(denom) < 1e-9f) continue
            val wx = sax - px
            val wy = say - py
            val t = (wx * ey - wy * ex) / denom
            val u = (wx * dy - wy * dx) / denom
            if (t > 1e-4f && u >= 0f && u <= 1f) count++
        }
        return count % 2 == 0
    }

    private fun inside(px: Float, py: Float): Boolean {
        if (px < minX || px > maxX || py < minY || py > maxY) return false
        var crossings = 0
        var k = 0
        while (k < sides.size) {
            val ax = sides[k]; val ay = sides[k + 1]; val bx = sides[k + 2]; val by = sides[k + 3]
            k += 4
            if ((ay > py) == (by > py)) continue
            val t = (py - ay) / (by - ay)
            if (px < ax + t * (bx - ax)) crossings++
        }
        return crossings % 2 == 1
    }

    // ---- growing ----

    /** Subdivides every edge, pushing the new vertex sideways by that vertex's own modifier. [trim] < 1 first cuts the far side of the outer ring away. */
    private fun WashPoly.grow(trim: Float = 1f): WashPoly =
        WashPoly(rings.mapIndexed { k, r -> growRing(if (k == 0) trimRing(r, trim) else r) { i -> m[i] } }, midX, midY, sizeX, sizeY)

    /** Same, with one big random modifier for every vertex: the fine roughness. */
    private fun WashPoly.growRough(): WashPoly {
        val mod = rnd.rndf(0.6f, 0.8f)
        return WashPoly(rings.map { r -> growRing(r) { mod } }, midX, midY, sizeX, sizeY)
    }

    /** Same, with the plain bleed strength everywhere. */
    private fun WashPoly.growEven(): WashPoly =
        WashPoly(rings.map { r -> growRing(r) { wc.bleed } }, midX, midY, sizeX, sizeY)

    private fun WashPoly.flipDirs(): WashPoly =
        WashPoly(rings.map { r -> Ring(r.x, r.y, r.m, BooleanArray(r.n) { !r.out[it] }) }, midX, midY, sizeX, sizeY)

    private inline fun growRing(r: Ring, modOf: Ring.(Int) -> Float): Ring {
        val n = r.n
        if (2 * n > cap) return r
        val x = FloatArray(2 * n)
        val y = FloatArray(2 * n)
        val m = FloatArray(2 * n)
        val out = BooleanArray(2 * n)
        for (i in 0 until n) {
            val j = if (i + 1 < n) i + 1 else 0
            val cx = r.x[i]; val cy = r.y[i]
            val nx = r.x[j]; val ny = r.y[j]
            val mi = r.m[i]
            val oi = r.out[i]
            val mod = r.modOf(i)
            x[2 * i] = cx; y[2 * i] = cy; m[2 * i] = mi; out[2 * i] = oi
            out[2 * i + 1] = oi
            val sx = nx - cx
            val sy = ny - cy
            if (mod < 0.05f) {
                x[2 * i + 1] = cx + sx / 2f
                y[2 * i + 1] = cy + sy / 2f
                m[2 * i + 1] = mi
                continue
            }
            // the perpendicular, pointing out of (or into) the shape, turned a few degrees either way
            val sign = if (oi == wc.outward) 1f else -1f
            val a = rnd.rndf(-DEG5, DEG5)
            val ca = cos(a); val sa = sin(a)
            val px0 = -sy * sign; val py0 = sx * sign
            val px = px0 * ca - py0 * sa
            val py = px0 * sa + py0 * ca
            val d = rnd.rndGaussian(0.5f, 0.2f) * rnd.rndf(0.65f, 1.35f) * mod
            x[2 * i + 1] = cx + sx * 0.5f + px * d
            y[2 * i + 1] = cy + sy * 0.5f + py * d
            m[2 * i + 1] = mi + rnd.rndGaussian(0f, 0.02f)
        }
        return Ring(x, y, m, out)
    }

    /** Cuts `(1 - f)` of the vertices out of the middle of the ring and bridges the gap with a few jittered ones. */
    private fun trimRing(r: Ring, f: Float): Ring {
        val n = r.n
        if (f >= 1f || f < 0f || n <= 8) return r
        val nTrim = ((1f - f) * n).toInt()
        if (nTrim <= 0) return r
        val s = (n / 2f - nTrim / 2f).toInt()
        val trimEnd = s + nTrim
        val i0 = (s - 1 + n) % n
        val i1 = trimEnd % n
        val evx = r.x[i1] - r.x[i0]
        val evy = r.y[i1] - r.y[i0]
        val edgeLen = hypot(evx, evy)
        val sampleIdx = if (s >= 2) rnd.nextInt(s - 1) else if (trimEnd < n - 1) trimEnd else 0
        val sb = (sampleIdx + 1) % n
        val typical = max(1f, hypot(r.x[sb] - r.x[sampleIdx], r.y[sb] - r.y[sampleIdx]))
        // the bridge gets the ring's own vertex spacing. two or three vertices across it, whatever
        // its length, grow into spikes through the middle of the wash under these modifiers; at
        // ring spacing it frays like the rest of the edge
        val nInsert = max(3, ceil(edgeLen / typical).toInt())
        val len = n - nTrim + nInsert
        val x = FloatArray(len); val y = FloatArray(len); val m = FloatArray(len); val out = BooleanArray(len)
        var dst = 0
        for (i in 0 until s) { x[dst] = r.x[i]; y[dst] = r.y[i]; m[dst] = r.m[i]; out[dst] = r.out[i]; dst++ }
        val jitter = edgeLen * 0.06f
        val dirBase = r.out[s % n]
        for (k in 0 until nInsert) {
            val t = (k + 1f) / (nInsert + 1f)
            x[dst] = r.x[i0] + evx * t + rnd.rndf(-jitter, jitter)
            y[dst] = r.y[i0] + evy * t + rnd.rndf(-jitter, jitter)
            m[dst] = rnd.rndf(0.3f, 0.5f)
            out[dst] = dirBase
            dst++
        }
        for (i in trimEnd until n) { x[dst] = r.x[i]; y[dst] = r.y[i]; m[dst] = r.m[i]; out[dst] = r.out[i]; dst++ }
        return Ring(x, y, m, out)
    }

    /** Keeps about [ratio] of the outer ring's vertices, pulls the ones that have wandered outside back in, and flips their bleed. Hole rings ride along untouched, they still clip. */
    private fun WashPoly.scatter(ratio: Float): WashPoly = WashPoly(
        rings.take(1).map { r ->
            val n = r.n
            val keep = max(4, (n * ratio).toInt())
            val step = n.toFloat() / keep
            val stepRand = step * 0.8f
            val x = FloatArray(keep); val y = FloatArray(keep); val m = FloatArray(keep); val out = BooleanArray(keep)
            for (i in 0 until keep) {
                val j = (i * step + rnd.rndf(0f, stepRand)).toInt() % n
                var px = r.x[j]
                var py = r.y[j]
                if (!inside(px, py)) {
                    px = midX + (px - midX) * rnd.rndf(0.3f, 0.6f)
                    py = midY + (py - midY) * rnd.rndf(0.3f, 0.6f)
                }
                x[i] = px; y[i] = py; m[i] = r.m[j]; out[i] = !r.out[j]
            }
            Ring(x, y, m, out)
        } + rings.drop(1),
        midX, midY, sizeX, sizeY,
    )

    // ---- putting pigment down ----

    // nonzero: the grown polygons cross themselves freely and even-odd would punch holes
    // wherever a bridge folds back
    private fun ringsPath(rings: List<Ring>): Path {
        val pb = PathBuilder(PathFillMode.WINDING)
        for (r in rings) {
            if (r.n < 3) continue
            pb.moveTo(r.x[0], r.y[0])
            for (i in 1 until r.n) pb.lineTo(r.x[i], r.y[i])
            pb.closePath()
        }
        return pb.detach()
    }

    /**
     * One coat: the outer ring filled at [alpha] with the hole rings clipped out of it (a hole
     * ring on its own would fill solid once the trim has cut the outer ring away from around
     * it), every outline stroked faintly so the rim piles up where outlines coincide.
     */
    private fun WashPoly.layer(i: Int, size: Float, alpha: Float) {
        val outer = ringsPath(rings.subList(0, 1))
        val holes = if (rings.size > 1) ringsPath(rings.subList(1, rings.size)) else null
        try {
            fill.color = withAlpha(alpha)
            if (wc.border > 0f) {
                rim.strokeWidth = map(i, 0, 24, size / 25f, size / 30f).coerceIn(size / 30f, size / 25f) * wc.border
                rim.color = withAlpha(0.01f * wc.border)
            }
            if (holes != null) {
                // the outer rim is clipped too: a trimmed coat's bridge runs across the
                // middle, and sixty faint bridge strokes would haze the hole
                c.save()
                c.clipPath(holes, ClipMode.DIFFERENCE, true)
                c.drawPath(outer, fill)
                if (wc.border > 0f) c.drawPath(outer, rim)
                c.restore()
                if (wc.border > 0f) c.drawPath(holes, rim)
            } else {
                c.drawPath(outer, fill)
                if (wc.border > 0f) c.drawPath(outer, rim)
            }
        } finally {
            outer.close()
            holes?.close()
        }
    }

    /**
     * Rubs pigment out of the current batch: a few hundred faint discs, gaussian-placed around
     * the centre, most of them. The disc alpha is set for about a third gone where they pile
     * up, patchy; much past that and the whole middle goes.
     */
    private fun WashPoly.erase(texture: Float) {
        val numCircles = (rnd.rndf(80f, 110f) * (2f + 1.5f * texture)).toInt()
        val halfX = sizeX / 1.3f
        val halfY = sizeY / 1.3f
        val minSize = min(sizeX, sizeY) * 1.3f
        val rMin = 0.03f * minSize
        val rMax = 0.45f * minSize
        eraser.color = withAlpha(0.025f * texture)
        for (i in 0 until numCircles) {
            val x = midX + rnd.rndGaussian(0f, halfX)
            val y = midY + rnd.rndGaussian(0f, halfY)
            val r = rnd.rndf(rMin, rMax)
            if (i % 5 != 0) c.drawCircle(x, y, r, eraser)
        }
    }

    fun lay() {
        val size = max(base.sizeX, base.sizeY)
        if (size <= 0f) return
        val opacity = wc.opacity * inkAlpha
        val texture = wc.texture * 3f
        // alpha of one coat, a couple of percent: the wash is the pile, not the coat
        val coat = 2f * opacity * (1f + wc.texture / 2f) / 100f
        val darker = rnd.rndf(0.15f, 0.7f)

        var pol = base.grow()
        val sparse = base.scatter(0.1f).grow().scatter(0.75f).flipDirs()
        var pols: List<WashPoly> = emptyList()

        // a batch of coats lives in its own layer so the eraser only rubs at those
        val pad = (0.5f + wc.bleed) * size + 10f
        val bounds = Rect(minX - pad, minY - pad, maxX + pad, maxY + pad)
        c.saveLayer(bounds, batch)
        try {
            for (i in 0 until wc.layers) {
                if (i % 4 == 0) pol = pol.grow()
                if (i % 2 == 0) {
                    pols = listOf(
                        pol.grow(1f - 0.0125f * i),
                        pol.grow(0.7f - 0.0125f * i),
                        pol.grow(0.4f - 0.0125f * i),
                    )
                }
                for (p in pols) p.growRough().growEven().layer(i, size, coat)
                // flecks on every coat at full strength pile into a dark star in the middle
                // (the scatter pulls on-edge vertices toward the centre), so every other coat
                // and lighter
                if (wc.scatter && i % 2 == 1) sparse.growRough().flipDirs().growEven().layer(i, size, coat * texture * 0.6f)
                if (i % 2 == 0) pol.grow(darker).growRough().layer(i, size, coat * 2f)

                val last = i == wc.layers - 1
                if (i % 8 == 0 || last) {
                    if (wc.texture > 0f) pol.erase(wc.texture)
                    c.restore()
                    if (!last) c.saveLayer(bounds, batch)
                }
            }
        } catch (e: Throwable) {
            c.restore()
            throw e
        }
    }

    private fun withAlpha(a: Float): Int =
        ((a.coerceIn(0f, 1f) * 255f + 0.5f).toInt() shl 24) or rgb

    private fun centroid(ring: List<Point>): Pair<Float, Float> {
        val n = ring.size
        if (n < 8) {
            var sx = 0f; var sy = 0f
            for (p in ring) { sx += p.x; sy += p.y }
            return sx / n to sy / n
        }
        var a = 0f; var cx = 0f; var cy = 0f
        for (i in 0 until n) {
            val p = ring[i]
            val q = ring[(i + 1) % n]
            val cross = p.x * q.y - q.x * p.y
            a += cross
            cx += (p.x + q.x) * cross
            cy += (p.y + q.y) * cross
        }
        a /= 2f
        return if (a != 0f) cx / (6f * a) to cy / (6f * a) else ring[0].x to ring[0].y
    }
}
