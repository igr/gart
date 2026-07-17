package lines.vitrali

import dev.oblac.gart.math.TWO_PIf
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import kotlin.math.*

// shared flat-polygon toolkit for the mosaic/glass pieces: convex polys in positive
// signed-area order, half-plane clipping, chord splits, grout insets.

internal class Poly(val p: FloatArray) {
    val n = p.size / 2
    fun x(i: Int) = p[2 * i]
    fun y(i: Int) = p[2 * i + 1]

    fun area(): Float {
        var a = 0f
        var j = n - 1
        for (i in 0 until n) {
            a += x(j) * y(i) - x(i) * y(j)
            j = i
        }
        return a * 0.5f
    }

    fun centroid(): Pair<Float, Float> {
        var cx = 0f; var cy = 0f
        for (i in 0 until n) { cx += x(i); cy += y(i) }
        return cx / n to cy / n
    }
}

internal fun rectPoly(x0: Float, y0: Float, x1: Float, y1: Float) =
    Poly(floatArrayOf(x0, y0, x1, y0, x1, y1, x0, y1))

// keep the side where (p - a) . n <= 0
internal fun clipHalf(q: Poly, ax: Float, ay: Float, nx: Float, ny: Float): Poly? {
    val out = FloatArray((q.n + 2) * 2)
    var m = 0
    var px = q.x(q.n - 1)
    var py = q.y(q.n - 1)
    var pd = (px - ax) * nx + (py - ay) * ny
    for (i in 0 until q.n) {
        val cx = q.x(i)
        val cy = q.y(i)
        val cd = (cx - ax) * nx + (cy - ay) * ny
        if (cd <= 0f) {
            if (pd > 0f) {
                val t = pd / (pd - cd)
                out[m * 2] = px + (cx - px) * t
                out[m * 2 + 1] = py + (cy - py) * t
                m++
            }
            out[m * 2] = cx
            out[m * 2 + 1] = cy
            m++
        } else if (pd <= 0f) {
            val t = pd / (pd - cd)
            out[m * 2] = px + (cx - px) * t
            out[m * 2 + 1] = py + (cy - py) * t
            m++
        }
        px = cx; py = cy; pd = cd
    }
    if (m < 3) return null
    return Poly(out.copyOf(m * 2))
}

// shift every edge inward by g and re-intersect - the grout gap
internal fun inset(q: Poly, g: Float): Poly? {
    val n = q.n
    val srcArea = q.area()
    if (srcArea <= 0f) return null
    val ax = FloatArray(n); val ay = FloatArray(n)
    val dx = FloatArray(n); val dy = FloatArray(n)
    for (i in 0 until n) {
        val j = (i + 1) % n
        val ex = q.x(j) - q.x(i)
        val ey = q.y(j) - q.y(i)
        val len = hypot(ex, ey)
        if (len < 1e-3f) return null
        ax[i] = q.x(i) - ey / len * g
        ay[i] = q.y(i) + ex / len * g
        dx[i] = ex; dy[i] = ey
    }
    val out = FloatArray(n * 2)
    var m = 0
    for (i in 0 until n) {
        val h = (i + n - 1) % n
        val cross = dx[h] * dy[i] - dy[h] * dx[i]
        if (abs(cross) < 1e-6f) continue
        val t = ((ax[i] - ax[h]) * dy[i] - (ay[i] - ay[h]) * dx[i]) / cross
        out[m * 2] = ax[h] + dx[h] * t
        out[m * 2 + 1] = ay[h] + dy[h] * t
        m++
    }
    if (m < 3) return null
    val r = Poly(out.copyOf(m * 2))
    val a = r.area()
    if (a <= 0f || a > srcArea) return null  // everted sliver
    return r
}

// split with a chord through (cx,cy) along (dx,dy)
internal fun split(q: Poly, cx: Float, cy: Float, dx: Float, dy: Float): Pair<Poly?, Poly?> {
    val a = clipHalf(q, cx, cy, -dy, dx)
    val b = clipHalf(q, cx, cy, dy, -dx)
    return a to b
}

// clip against a circle, keeping the inside or the outside. where the boundary runs
// along the circle the arc is sampled into short segments, so the result is still a
// plain Poly (gently non-convex on the bitten side). returns the sampled arc chain too,
// handy for drawing the cut as a heavier lead line.
// meant for gentle cuts: radius comfortably bigger than the polygon, center outside it.
internal fun clipCircleSide(
    q: Poly, cx: Float, cy: Float, r: Float,
    keepInside: Boolean, arcStep: Float,
): Pair<Poly?, FloatArray?> {
    val n = q.n
    val pts = ArrayList<Float>(n * 2 + 16)
    val tag = ArrayList<Int>(n + 8)   // 0 kept vertex, 1 leaving the kept side, 2 entering it
    val sgn = if (keepInside) 1f else -1f
    fun dd(x: Float, y: Float) = (hypot(x - cx, y - cy) - r) * sgn
    var px = q.x(n - 1)
    var py = q.y(n - 1)
    var pd = dd(px, py)
    for (i in 0 until n) {
        val vx = q.x(i)
        val vy = q.y(i)
        val vd = dd(vx, vy)
        // circle crossings along this edge, in walk order
        val ex = vx - px; val ey = vy - py
        val ea = ex * ex + ey * ey
        val fx = px - cx; val fy = py - cy
        val eb = 2f * (fx * ex + fy * ey)
        val ec = fx * fx + fy * fy - r * r
        val disc = eb * eb - 4f * ea * ec
        var inKept = pd <= 0f
        if (disc > 0f && ea > 1e-9f) {
            val sq = sqrt(disc)
            for (t in floatArrayOf((-eb - sq) / (2f * ea), (-eb + sq) / (2f * ea))) {
                if (t <= 1e-5f || t >= 1f - 1e-5f) continue
                pts.add(px + ex * t); pts.add(py + ey * t)
                tag.add(if (inKept) 1 else 2)
                inKept = !inKept
            }
        }
        if (vd <= 0f) { pts.add(vx); pts.add(vy); tag.add(0) }
        px = vx; py = vy; pd = vd
    }
    if (tag.size < 3) return null to null

    // where an exit meets the next entry the boundary follows the circle. the sweep
    // direction comes from orientation, not shortest-way: an inside piece runs along
    // the disk's positive direction, an outside piece the negative - correct even when
    // a deep bite wraps past half the circle
    val res = ArrayList<Float>(pts.size * 2)
    val chain = ArrayList<Float>()
    val m = tag.size
    for (i in 0 until m) {
        val x = pts[i * 2]; val y = pts[i * 2 + 1]
        res.add(x); res.add(y)
        val j = (i + 1) % m
        if (tag[i] == 1 && tag[j] == 2) {
            val jx = pts[j * 2]; val jy = pts[j * 2 + 1]
            val a1 = atan2(y - cy, x - cx)
            val a2 = atan2(jy - cy, jx - cx)
            var da = (a2 - a1) % TWO_PIf
            if (keepInside) {
                if (da < 0f) da += TWO_PIf
            } else {
                if (da > 0f) da -= TWO_PIf
            }
            val steps = max(1, (abs(da) * r / arcStep).toInt())
            chain.add(x); chain.add(y)
            for (s in 1 until steps) {
                val aa = a1 + da * s / steps
                val ax = cx + cos(aa) * r
                val ay = cy + sin(aa) * r
                res.add(ax); res.add(ay)
                chain.add(ax); chain.add(ay)
            }
            chain.add(jx); chain.add(jy)
        }
    }
    if (res.size < 6) return null to null
    val poly = Poly(res.toFloatArray())
    if (poly.area() < 1e-3f) return null to null
    return poly to (if (chain.isEmpty()) null else chain.toFloatArray())
}

internal fun pathOfPoly(p: Poly, s: Float, dx: Float, dy: Float): Path {
    val b = PathBuilder()
    b.moveTo(Point(p.x(0) * s + dx, p.y(0) * s + dy))
    for (i in 1 until p.n) b.lineTo(Point(p.x(i) * s + dx, p.y(i) * s + dy))
    return b.closePath().detach()
}
