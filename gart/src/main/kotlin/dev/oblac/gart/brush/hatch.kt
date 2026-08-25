package dev.oblac.gart.brush

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.gfx.Line
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathMeasure
import org.jetbrains.skia.Point
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Hatching: parallel lines [dist] px apart, running at [angle], clipped to a shape.
 *
 * @property dist      spacing between lines, px
 * @property angle     direction the lines run in
 * @property rand      0..1, jitters the spacing and the line ends by that fraction of [dist]
 * @property gradient  spacing grows by this factor across the shape (0 keeps it even, 1 doubles it)
 * @property overshoot px every line runs past the edge, the way a quick hand hatches
 */
data class Hatch(
    val dist: Float,
    val angle: Angle = Degrees(45f),
    val rand: Float = 0f,
    val gradient: Float = 0f,
    val overshoot: Float = 0f,
) {
    init {
        require(dist > 0f) { "dist must be > 0" }
        require(rand in 0f..1f) { "rand must be in 0..1" }
        require(gradient >= 0f) { "gradient must be >= 0" }
    }
}

/**
 * The hatch lines of [path] as plain segments, in sweep order, so they can be brushed, stroked
 * with an ordinary paint, or bent some more first. Every contour is taken as a closed polygon
 * and the crossings along each line are paired even-odd, so holes and concave shapes come out
 * right without any path booleans.
 */
fun hatchLines(path: Path, hatch: Hatch, rnd: Random = Random.Default): List<Line> {
    val rings = flattenRings(path, 1f)
    if (rings.isEmpty()) return emptyList()

    // work in a frame where the hatch lines are horizontal
    val bounds = path.bounds
    val cx = (bounds.left + bounds.right) / 2f
    val cy = (bounds.top + bounds.bottom) / 2f
    val ca = cos(hatch.angle.radians)
    val sa = sin(hatch.angle.radians)
    fun toFrame(p: Point) = Point((p.x - cx) * ca + (p.y - cy) * sa, -(p.x - cx) * sa + (p.y - cy) * ca)
    fun fromFrame(x: Float, y: Float) = Point(x * ca - y * sa + cx, x * sa + y * ca + cy)

    val frame = rings.map { ring -> ring.map(::toFrame) }
    var minY = Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    for (ring in frame) for (p in ring) {
        if (p.y < minY) minY = p.y
        if (p.y > maxY) maxY = p.y
    }
    val span = maxY - minY
    if (span <= 0f) return emptyList()

    val lines = mutableListOf<Line>()
    val xs = ArrayList<Float>()
    var y = minY + hatch.dist / 2f
    while (y < maxY) {
        xs.clear()
        for (ring in frame) {
            var p = ring[ring.size - 1]
            for (q in ring) {
                // half-open test, so a line through a vertex counts it once
                if ((p.y > y) != (q.y > y)) xs += p.x + (y - p.y) * (q.x - p.x) / (q.y - p.y)
                p = q
            }
        }
        xs.sort()
        var k = 0
        while (k + 1 < xs.size) {
            val j1 = if (hatch.rand > 0f) hatch.rand * hatch.dist * rnd.rndf(-1f, 1f) else 0f
            val j2 = if (hatch.rand > 0f) hatch.rand * hatch.dist * rnd.rndf(-1f, 1f) else 0f
            val x1 = xs[k] - hatch.overshoot - j1
            val x2 = xs[k + 1] + hatch.overshoot + j2
            if (x2 > x1) lines += Line(fromFrame(x1, y), fromFrame(x2, y))
            k += 2
        }
        val t = (y - minY) / span
        val jitter = if (hatch.rand > 0f) rnd.rndf(1f - hatch.rand, 1f + hatch.rand) else 1f
        y += hatch.dist * (1f + hatch.gradient * t) * max(jitter, 0.2f)
    }
    return lines
}

/**
 * Hatches [path] with brush strokes: every line from [hatchLines] becomes its own stroke with
 * its own pressure and its own slightly different darkness, which is what makes it read as
 * hatched by hand rather than ruled.
 */
fun Canvas.drawBrushHatch(
    path: Path,
    hatch: Hatch,
    brush: Brush,
    color: Int,
    size: Float = 1f,
    rnd: Random = Random.Default,
    wobble: Wobble? = null,
    blend: BlendMode = BlendMode.SRC_OVER,
) {
    for (line in hatchLines(path, hatch, rnd)) {
        drawBrush(line, brush, color, size, rnd, wobble, null, blend)
    }
}

/** Samples every contour of [path] into a closed ring of points about [step] px apart. */
internal fun flattenRings(path: Path, step: Float): List<List<Point>> {
    val rings = mutableListOf<List<Point>>()
    val m = PathMeasure(path, true)
    try {
        do {
            val length = m.length
            if (length <= 0f) continue
            val n = max(3, ceil(length / step).toInt())
            val ring = ArrayList<Point>(n)
            for (i in 0 until n) m.getPosition(i * length / n)?.let(ring::add)
            if (ring.size >= 3) rings += ring
        } while (m.nextContour())
    } finally {
        m.close()
    }
    return rings
}
