package work.cosmic

import dev.oblac.gart.Gart
import dev.oblac.gart.color.rgb
import dev.oblac.gart.gfx.*
import dev.oblac.gart.jfa.Jfa
import dev.oblac.gart.math.f
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.smooth.catmullRomSpline
import org.jetbrains.skia.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val W = 1280
private const val H = 1280

private val lineDark = rgb(30, 30, 30)
private val lineMid = rgb(90, 90, 90)
private val lineLight = rgb(210, 210, 210)
private val white = 0xFFFFFFFF.toInt()

fun main() {
    val gart = Gart.of("cosmic", W, H)
    val g = gart.gartvas()
    val c = g.canvas
    val d = g.d
    val rng = Random(142)

    c.clear(Color.BLACK)

    val centerline = generateCenterline(rng)

    drawStarField(c, rng)
    val river = drawRiverBands(c, centerline)
    val jfaResult = Jfa(d).computeDistanceField(river)
    repeat(3) {
        val outline1 = jfaResult.tracePath(180f - it * 20)
        c.drawPath(outline1, strokeOfRed(2f))
    }
    gart.window().show(g)
}

// STAR FIELD

private fun drawStarField(c: Canvas, rng: Random) {
    val fill = fillOf(white)
    val stroke = strokeOf(white, 1f)

    repeat(5000) {
        val x = rng.nextFloat() * W
        val y = rng.nextFloat() * H
        val n = SimplexNoise.noise(x * 0.01, y * 0.01)
        if (n > -0.2) {
            val r = when {
                rng.nextFloat() < 0.75f -> 0.5f
                else -> 1f
            }
            if (rng.nextFloat() < 0.8f) c.drawCircle(x, y, r, fill)
            else c.drawCircle(x, y, r + 0.5f, stroke)
        }
    }
}

// CENTERLINE

private fun generateCenterline(rng: Random): Path {
    val pts = mutableListOf(
        Point(-80f, H * 0.88f),
        Point(W * 0.28f, H * 0.78f),
        Point(W * 0.40f, H * 0.62f),
        Point(W * 0.58f, H * 0.45f),
        Point(W * 0.78f, H * 0.18f),
        Point(W + 40f, H * 0.12f)
    )
    for (i in pts.indices) {
        pts[i] = Point(
            pts[i].x + rng.nextFloat() * 60f - 30f,
            pts[i].y + rng.nextFloat() * 100f - 50f
        )
    }
    return catmullRomSpline(pts, 20)
}

// RIVER BANDS

private fun drawRiverBands(c: Canvas, centerline: Path): Path {
    val measure = PathMeasure(centerline)
    val totalLen = measure.length

    // Create circles along the path with random offsets and varying radii
    val circlePaths = mutableListOf<Path>()

    pointsOn(centerline, 60).forEach {
        circlePaths.add(Circle(it, 30 + rndf(-10f, 10f)).toPath())
    }

    val circleCount = 10
    for (i in 0 until circleCount) {
        val t = i / (circleCount - 1).toFloat()
        val d = t * totalLen
        val pos = measure.getPosition(d) ?: continue

        // Random offset from the path
        val cx = pos.x + rndf(-1, 1) * 50f
        val cy = pos.y + rndf(-1, 1) * 50f

        // Varying radius
        val sinFactor = sin(t * 10f)
        val radius = sinFactor * sinFactor * 140f

        val circle = Circle(cx, cy, radius).toPath()
        c.drawCircle(Circle(cx, cy, radius), strokeOfBlue(1f))
        circlePaths.add(circle)
    }

    // Combine all circles into one path using union
    val combined = combinePathsWithOp(PathOp.UNION, *circlePaths.toTypedArray())

    // Get outline points from the combined path and draw smooth line
    val outlinePoints = pointsOn(combined, 40)

    val smoothPath = catmullRomSpline(outlinePoints, 40)
    c.drawPath(smoothPath, strokeOf(lineLight, 1.5f).roundStroke())
    c.drawPath(smoothPath, fillOf(lineLight))
    return smoothPath
}


private fun offsetSplinePath(path: Path, offset: Float, jitter: Float): Path {
    val pts = pointsOn(path, 400)
    val shifted = pts.mapIndexed { i, p ->
        val prev = pts[maxOf(0, i - 1)]
        val next = pts[minOf(pts.lastIndex, i + 1)]
        val dx = next.x - prev.x
        val dy = next.y - prev.y
        val len = sqrt(dx * dx + dy * dy).takeIf { it != 0f } ?: 1f
        val nx = -dy / len
        val ny = dx / len

        val wob = SimplexNoise.noise(p.x * 0.02, p.y * 0.02).toFloat() * jitter
        Point(p.x + nx * (offset + wob), p.y + ny * (offset + wob))
    }
    return catmullRomSpline(shifted, 8)
}

// ---------------------------------------------------------------------------
// DISTANCE (FIXED)
// ---------------------------------------------------------------------------

private fun distToSpline(points: List<Point>, x: Float, y: Float): Float {
    var min = Float.MAX_VALUE
    for (i in 0 until points.lastIndex) {
        val a = points[i]
        val b = points[i + 1]

        val abx = b.x - a.x
        val aby = b.y - a.y
        val apx = x - a.x
        val apy = y - a.y

        val t = ((apx * abx + apy * aby) / (abx * abx + aby * aby))
            .coerceIn(0f, 1f)

        val px = a.x + abx * t
        val py = a.y + aby * t

        val dx = x - px
        val dy = y - py
        val d = dx * dx + dy * dy
        if (d < min) min = d
    }
    return sqrt(min)
}

// ---------------------------------------------------------------------------
// CONTOURS (IMPROVED)
// ---------------------------------------------------------------------------

private fun drawContourField(c: Canvas, pts: List<Point>) {
    val step = 8
    val cols = W / step + 1
    val rows = H / step + 1
    val paint = strokeOf(lineDark, 2f).roundStroke()

    val field = Array(rows) { gy ->
        FloatArray(cols) { gx ->
            val x = gx * step.toDouble()
            val y = gy * step.toDouble()
            val d = distToSpline(pts, x.toFloat(), y.toFloat())
            val n = SimplexNoise.noise(x * 0.012, y * 0.012).toFloat()
            n - smoothFalloff(d, 150f, 250f)
        }
    }

    val levels = floatArrayOf(-0.5f, -0.3f, -0.1f, 0.1f)

    for (iso in levels) {
        for (gy in 0 until rows) {
            val seg = mutableListOf<Point>()
            for (gx in 0 until cols) {
                if (abs(field[gy][gx] - iso) < 0.03f)
                    seg.add(Point((gx * step).toFloat(), (gy * step).toFloat()))
                else {
                    flushSegment(c, seg, paint)
                    seg.clear()
                }
            }
            flushSegment(c, seg, paint)
        }
    }
}

private fun drawStippleZone(c: Canvas, pts: List<Point>, rng: Random) {
    val paint = strokeOf(lineDark, 1f)
    repeat(40000) {
        val x = rng.nextFloat() * W
        val y = rng.nextFloat() * H
        val d = distToSpline(pts, x, y)

        val p = when {
            d < 80f -> 0.02f
            d < 140f -> 0.08f
            d < 220f -> 0.2f
            else -> 0.05f
        }

        if (rng.nextFloat() < p) {
            c.drawPoint(x, y, paint)
        }
    }
}

private fun drawRiverNodes(c: Canvas, centerline: Path, rng: Random) {
    val pts = pointsOn(centerline, 300)

    listOf(0.15f, 0.4f, 0.65f, 0.9f).forEach { t ->
        val idx = (t * (pts.size - 1)).toInt()
        val p = pts[idx]
        val r = rng.nextFloat() * 48f + 64f

        val blob = PathBuilder()
        val steps = 40
        for (i in 0..steps) {
            val a = i / steps.toFloat() * Math.PI * 2
            val noise = SimplexNoise.noise(cos(a).toDouble(), sin(a).toDouble()).toFloat()
            val rr = r * (1f + noise * 0.2f)
            val x = p.x + cos(a) * rr
            val y = p.y + sin(a) * rr
            if (i == 0) blob.moveTo(x.f(), y.f()) else blob.lineTo(x.f(), y.f())
        }
        blob.closePath()

        val blobP = blob.detach()
        c.drawPath(blobP, fillOf(white))
        c.drawPath(blobP, strokeOf(lineDark, 3f))
    }
}

private fun drawEmbeddedGlyphs(c: Canvas, centerline: Path, rng: Random) {
    val pts = pointsOn(centerline, 100)
    repeat(4) {
        val p = pts[rng.nextInt(pts.size)]
        c.drawCircle(p.x, p.y, rng.nextFloat() * 10f + 5f, fillOf(lineDark))
    }
}

private fun drawSpiralIslands(c: Canvas, rng: Random) {
    repeat(6) {
        val cx = rng.nextFloat() * W
        val cy = rng.nextFloat() * H
        val base = rng.nextFloat() * 30f + 20f

        for (k in 0..4) {
            val r = base + k * 8f
            val path = PathBuilder()
            val steps = 40

            for (i in 0..steps) {
                val a = i / steps.toFloat() * Math.PI * 2
                val noise = SimplexNoise.noise(
                    (cx + cos(a) * r) * 0.02,
                    (cy + sin(a) * r) * 0.02
                ).toFloat()

                val rr = r * (1f + noise * 0.08f)
                val x = cx + cos(a) * rr
                val y = cy + sin(a) * rr

                if (i == 0) path.moveTo(x.f(), y.f()) else path.lineTo(x.f(), y.f())
            }
            path.closePath()
            c.drawPath(path.detach(), strokeOf(lineMid, 2f))
        }

        c.drawCircle(cx, cy, 2f, fillOf(lineDark))
    }
}

private fun drawOrbitals(c: Canvas, rng: Random) {
    repeat(16) {
        val x = rng.nextFloat() * W
        val y = rng.nextFloat() * H
        val r = rng.nextFloat() * 12f + 6f

        c.drawCircle(x, y, r, strokeOf(white, 2f))
        c.drawCircle(x, y, r + 4f, strokeOf(lineMid, 1f))
    }
}

private fun darkenOuterRegions(c: Canvas) {
    // minimal no-op placeholder (can expand later)
}

private fun addFinalSparkles(c: Canvas, rng: Random) {
    val paint = fillOf(white)
    repeat(1200) {
        val x = rng.nextFloat() * W
        val y = rng.nextFloat() * H
        c.drawCircle(x, y, 1f, paint)
    }
}

// ---------------------------------------------------------------------------
// HELPERS
// ---------------------------------------------------------------------------

private fun flushSegment(c: Canvas, seg: List<Point>, p: Paint) {
    if (seg.size > 3) c.drawPath(catmullRomSpline(seg, 3), p)
}

private fun smoothFalloff(d: Float, c: Float, r: Float): Float {
    if (d < c) return 1f
    if (d > c + r) return 0f
    val t = (d - c) / r
    return 1f - t * t * (3f - 2f * t)
}

// ---------------------------------------------------------------------------
// WOBBLE OUTLINE (NEW)
// ---------------------------------------------------------------------------

private fun wobbleOutline(
    path: Path,
    thickness: Float,
    profile: List<Pair<Float, Float>>,
    amp: Float
): Path {
    val measure = PathMeasure(path)
    val len = measure.length
    val steps = (len / 6f).toInt()

    val left = mutableListOf<Point>()
    val right = mutableListOf<Point>()

    for (i in 0..steps) {
        val d = i * len / steps
        val pos = measure.getPosition(d) ?: continue
        val tan = measure.getTangent(d) ?: continue

        val l = sqrt(tan.x * tan.x + tan.y * tan.y)
        val nx = -tan.y / l
        val ny = tan.x / l

        val t = d / len
        val w = thickness * 0.5f * sampleProfile(profile, t)
        val wob = SimplexNoise.noise(d * 0.02, 0.0).toFloat() * amp

        left.add(Point(pos.x + nx * (w + wob), pos.y + ny * (w + wob)))
        right.add(Point(pos.x - nx * (w + wob), pos.y - ny * (w + wob)))
    }

    return closedPathOf(left + right.reversed())
}

private fun sampleProfile(p: List<Pair<Float, Float>>, t: Float): Float {
    for (i in 0 until p.lastIndex) {
        val (t0, v0) = p[i]
        val (t1, v1) = p[i + 1]
        if (t in t0..t1) {
            val k = (t - t0) / (t1 - t0)
            return v0 + (v1 - v0) * k
        }
    }
    return 1f
}
