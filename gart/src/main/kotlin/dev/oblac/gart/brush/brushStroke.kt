package dev.oblac.gart.brush

import dev.oblac.gart.color.alphaf
import dev.oblac.gart.gfx.Line
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.gfx.pathOf
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.rndGaussian
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathMeasure
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// a step never goes under this whatever spacing × size comes to, so a tiny size cant spin
// millions of stamps
private const val MIN_STEP = 0.05f

// a dot never renders smaller than this. the stock brushes put down 0.2-0.3 px dots at size 1
// and antialiasing washes those out to nothing. the inflated dot keeps its alpha in proportion
// to the diameter (not the area): a hairline at size 1 stays a visible line, a spray stays a mist
private const val MIN_DOT = 1f

/**
 * Strokes [path] with [brush]: walks every contour and stamps the brush tip along it.
 *
 * @param color    ARGB ink; its alpha multiplies the brush opacity
 * @param size     uniform zoom of the brush (weight, scatter and spacing all scale with it)
 * @param rnd      source of every random decision; pass a seeded one for a repeatable render
 * @param wobble   optional field bending the stroke off the path, see [Wobble]
 * @param pressure optional extra pressure over `t` in 0..1 along each contour, multiplied into the brush's own
 * @param blend    how stamps land on what is there already; MULTIPLY reads well for dry media on paper
 */
fun Canvas.drawBrush(
    path: Path,
    brush: Brush,
    color: Int,
    size: Float = 1f,
    rnd: Random = Random.Default,
    wobble: Wobble? = null,
    pressure: ((t: Float) -> Float)? = null,
    blend: BlendMode = BlendMode.SRC_OVER,
) {
    require(size > 0f) { "size must be > 0" }
    BrushStroker(this, brush, color, size, rnd, wobble, pressure, blend).stroke(path)
}

/** Strokes the polyline through [points]; fewer than two points draws nothing. */
fun Canvas.drawBrush(
    points: List<Point>,
    brush: Brush,
    color: Int,
    size: Float = 1f,
    rnd: Random = Random.Default,
    wobble: Wobble? = null,
    pressure: ((t: Float) -> Float)? = null,
    blend: BlendMode = BlendMode.SRC_OVER,
) {
    if (points.size < 2) return
    pathOf(points).use { drawBrush(it, brush, color, size, rnd, wobble, pressure, blend) }
}

fun Canvas.drawBrush(
    line: Line,
    brush: Brush,
    color: Int,
    size: Float = 1f,
    rnd: Random = Random.Default,
    wobble: Wobble? = null,
    pressure: ((t: Float) -> Float)? = null,
    blend: BlendMode = BlendMode.SRC_OVER,
) = drawBrush(line.a, line.b, brush, color, size, rnd, wobble, pressure, blend)

fun Canvas.drawBrush(
    a: Point,
    b: Point,
    brush: Brush,
    color: Int,
    size: Float = 1f,
    rnd: Random = Random.Default,
    wobble: Wobble? = null,
    pressure: ((t: Float) -> Float)? = null,
    blend: BlendMode = BlendMode.SRC_OVER,
) {
    pathOf(a, b).use { drawBrush(it, brush, color, size, rnd, wobble, pressure, blend) }
}

/**
 * One stroke's worth of state: the paint, the ink, and the per-stamp routines for each tip.
 */
private class BrushStroker(
    private val c: Canvas,
    private val brush: Brush,
    color: Int,
    private val size: Float,
    private val rnd: Random,
    private val wobble: Wobble?,
    private val modulate: ((Float) -> Float)?,
    blend: BlendMode,
) {
    private val rgb = color and 0xFFFFFF
    private val inkAlpha = alphaf(color)
    private val paint = paint().apply {
        blendMode = blend
    }

    // image tips: the image is only a shape. SRC_IN swaps its colour for the ink, keeps its alpha
    private val imagePaint = (brush.tip as? Tip.Image)?.let {
        paint().apply {
            blendMode = blend
            colorFilter = ColorFilter.makeBlend(rgb or (0xFF shl 24), BlendMode.SRC_IN)
        }
    }

    fun stroke(path: Path) {
        val m = PathMeasure(path, false)
        try {
            do contour(m) while (m.nextContour())
        } finally {
            m.close()
        }
    }

    private fun contour(m: PathMeasure) {
        val length = m.length
        if (length <= 0f) return
        val step = max(brush.spacing * size, MIN_STEP)
        val steps = max(1, (length / step).roundToInt())
        val pressureAt = brush.pressure.along(rnd, length)
        // the whole stroke a touch lighter or darker than the one before
        val opacity = (brush.opacity * (1f + rnd.rndGaussian(0f, 0.03f))).coerceIn(0f, 1f)
        var offX = 0f
        var offY = 0f
        for (i in 0 until steps) {
            val s = i * step
            val pos = m.getPosition(s) ?: break
            val tan = m.getTangent(s)
            var tx = tan?.x ?: 1f
            var ty = tan?.y ?: 0f
            val tl = sqrt(tx * tx + ty * ty)
            if (tl < 1e-6f) {
                tx = 1f; ty = 0f
            } else {
                tx /= tl; ty /= tl
            }
            // where the hand is: on the path, plus whatever the wobble has drifted it so far
            val x = pos.x + offX
            val y = pos.y + offY
            var dx = tx
            var dy = ty
            if (wobble != null) {
                val a = wobble.at(x, y)
                val ca = cos(a)
                val sa = sin(a)
                dx = tx * ca - ty * sa
                dy = tx * sa + ty * ca
                offX += step * (dx - tx)
                offY += step * (dy - ty)
            }
            var p = pressureAt(s)
            if (modulate != null) p *= modulate(s / length)
            if (p <= 0f) continue
            when (val tip = brush.tip) {
                Tip.Dots -> dots(x, y, dx, dy, p, opacity)
                is Tip.Spray -> spray(tip, x, y, p, opacity)
                Tip.Marker -> marker(x, y, p, opacity)
                is Tip.Image -> image(tip, x, y, dx, dy, p, opacity)
                is Tip.Custom -> custom(tip, x, y, dx, dy, p, opacity)
            }
        }
    }

    // ---- the tips ----

    private fun dots(x: Float, y: Float, dx: Float, dy: Float, p: Float, opacity: Float) {
        if (rnd.nextFloat() >= brush.grain * p) return
        // how far this dot may wander. sharp brushes keep it even, soft ones let it spike,
        // and a light touch spikes more
        val vib = size * brush.scatter * (brush.sharpness + (1f - brush.sharpness) * rnd.rndGaussian() / p)
        val perp = vib * rnd.rndf(-1f, 1f)
        val along = 0.3f * vib * rnd.rndf(-1f, 1f)
        val cx = x - dy * perp + dx * along
        val cy = y + dx * perp + dy * along
        val d = p * p * brush.weight * size * rnd.rndf(0.85f, 1.15f)
        val a = max(0.9f, p) * opacity * rnd.rndf(0.75f, 1.1f)
        dot(cx, cy, d, a)
    }

    private fun spray(tip: Tip.Spray, x: Float, y: Float, p: Float, opacity: Float) {
        val r = size * brush.scatter * p + size * brush.scatter * rnd.rndGaussian() / 3f
        val sw = brush.weight * size * rnd.rndf(0.9f, 1.1f)
        val n = min(ceil(tip.specks / p).toInt(), tip.specks * 4)
        repeat(n) {
            val rr = r * rnd.rndf(0.9f, 1.1f)
            val rx = rr * rnd.rndf(-1f, 1f)
            val ry = rnd.rndf(-1f, 1f) * sqrt(max(0f, rr * rr - rx * rx))
            dot(x + rx, y + ry, sw, opacity)
        }
    }

    private fun marker(x: Float, y: Float, p: Float, opacity: Float) {
        val vib = size * brush.scatter
        val cx = x + vib * rnd.rndf(-1f, 1f)
        val cy = y + vib * rnd.rndf(-1f, 1f)
        dot(cx, cy, size * brush.weight * p, opacity * max(0.8f, p) * rnd.rndf(0.9f, 1.1f))
    }

    private fun image(tip: Tip.Image, x: Float, y: Float, dx: Float, dy: Float, p: Float, opacity: Float) {
        val vib = size * brush.scatter
        val cx = x + vib * rnd.rndf(-1f, 1f)
        val cy = y + vib * rnd.rndf(-1f, 1f)
        val ext = brush.weight * size * p
        val angle = rotation(tip.rotate, dx, dy)
        val a = opacity * max(0.8f, p) * rnd.rndf(0.9f, 1.1f)
        val ip = imagePaint!!
        ip.alpha = (a.coerceIn(0f, 1f) * inkAlpha * 255f + 0.5f).toInt()
        val img = tip.image
        val scale = ext / max(img.width, img.height)
        val hw = img.width * scale / 2f
        val hh = img.height * scale / 2f
        c.save()
        c.translate(cx, cy)
        c.rotate(Math.toDegrees(angle.toDouble()).toFloat())
        c.drawImageRect(
            img,
            Rect(0f, 0f, img.width.toFloat(), img.height.toFloat()),
            Rect(-hw, -hh, hw, hh),
            SamplingMode.LINEAR,
            ip,
            true,
        )
        c.restore()
    }

    private fun custom(tip: Tip.Custom, x: Float, y: Float, dx: Float, dy: Float, p: Float, opacity: Float) {
        val vib = size * brush.scatter
        val cx = x + vib * rnd.rndf(-1f, 1f)
        val cy = y + vib * rnd.rndf(-1f, 1f)
        val ext = brush.weight * size * p
        val angle = rotation(tip.rotate, dx, dy)
        val a = opacity * max(0.8f, p) * rnd.rndf(0.9f, 1.1f)
        paint.color = withAlpha(a)
        c.save()
        c.translate(cx, cy)
        c.rotate(Math.toDegrees(angle.toDouble()).toFloat())
        c.scale(ext, ext)
        tip.draw(c, paint)
        c.restore()
    }

    private fun rotation(rotate: Rotate, dx: Float, dy: Float): Float = when (rotate) {
        Rotate.NONE -> 0f
        Rotate.NATURAL -> atan2(dy, dx)
        Rotate.RANDOM -> rnd.rndf(0f, TAUf)
    }

    private fun dot(cx: Float, cy: Float, d: Float, a: Float) {
        if (d <= 0f || a <= 0f) return
        if (d < MIN_DOT) {
            paint.color = withAlpha(a * d / MIN_DOT)
            c.drawCircle(cx, cy, MIN_DOT / 2f, paint)
        } else {
            paint.color = withAlpha(a)
            c.drawCircle(cx, cy, d / 2f, paint)
        }
    }

    private fun withAlpha(a: Float): Int =
        ((a.coerceIn(0f, 1f) * inkAlpha * 255f + 0.5f).toInt() shl 24) or rgb
}
