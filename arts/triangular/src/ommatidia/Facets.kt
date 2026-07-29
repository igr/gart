package ommatidia

import dev.oblac.gart.math.TAUf
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * where it sits, which way the membrane points under it, the local spacing,
 * and the polygon we actually fill (already inset by the grout).
 */
internal class Facet(
    val id: Int,
    val x: Float,
    val y: Float,
    val nx: Float,
    val ny: Float,
    val nz: Float,
    val r: Float,
    val poly: FloatArray,
)

/** growable point soup */
private class Soup(cap: Int) {
    var x = FloatArray(cap)
    var y = FloatArray(cap)
    var s = FloatArray(cap)
    var n = 0

    fun add(px: Float, py: Float, ps: Float) {
        if (n == x.size) {
            x = x.copyOf(n * 2)
            y = y.copyOf(n * 2)
            s = s.copyOf(n * 2)
        }
        x[n] = px
        y[n] = py
        s[n] = ps
        n++
    }
}

private class Buckets(val ox: Float, val oy: Float, gw: Float, gh: Float, val cell: Float) {
    val cols = (gw / cell).toInt() + 2
    val rows = (gh / cell).toInt() + 2
    val data = arrayOfNulls<IntArray>(cols * rows)
    val fill = IntArray(cols * rows)

    fun col(px: Float) = ((px - ox) / cell).toInt().coerceIn(0, cols - 1)
    fun row(py: Float) = ((py - oy) / cell).toInt().coerceIn(0, rows - 1)

    fun clear() = java.util.Arrays.fill(fill, 0)

    fun put(i: Int, px: Float, py: Float) {
        val b = row(py) * cols + col(px)
        var a = data[b]
        if (a == null) {
            a = IntArray(4)
            data[b] = a
        } else if (fill[b] == a.size) {
            a = a.copyOf(a.size * 2)
            data[b] = a
        }
        a[fill[b]++] = i
    }

    fun near(px: Float, py: Float, rings: Int, body: (Int) -> Unit) {
        val c = col(px)
        val r = row(py)
        for (rr in max(0, r - rings)..min(rows - 1, r + rings)) {
            val base = rr * cols
            for (cc in max(0, c - rings)..min(cols - 1, c + rings)) {
                val b = base + cc
                val a = data[b] ?: continue
                for (k in 0 until fill[b]) body(a[k])
            }
        }
    }
}

private const val TRIES = 14
private const val MAXV = 64

/**
 * seed -> relax -> voronoi
 */
internal fun packFacets(
    m: Membrane,
    p: Params,
    rnd: Random,
    light: FloatArray,
    w: Float,
    h: Float,
): List<Facet> {
    val base = p.pitch * min(w, h)
    val margin = base * 3f
    val ox = -margin
    val oy = -margin
    val gw = w + margin * 2f
    val gh = h + margin * 2f

    val soup = seed(m, p, rnd, light, base, ox, oy, gw, gh, w)
    relax(soup, m, p, light, base, ox, oy, gw, gh, w)
    return cells(soup, m, p, base, ox, oy, gw, gh, w, h)
}

private fun spacingAt(
    m: Membrane,
    p: Params,
    light: FloatArray,
    base: Float,
    x: Float,
    y: Float,
    w: Float,
    nrm: FloatArray,
): Float {
    m.normalInto(x / w, y / w, nrm)
    val facing = (nrm[0] * light[0] + nrm[1] * light[1] + nrm[2] * light[2]).coerceIn(0f, 1f)
    return base * (1f - p.acuity * facing)
}

private fun squashOf(p: Params, nz: Float) = (1f - p.aniso * (1f - nz)).coerceAtLeast(0.15f)

/**
 * bridson dart throwing:)
 */
private fun seed(
    m: Membrane,
    p: Params,
    rnd: Random,
    light: FloatArray,
    base: Float,
    ox: Float,
    oy: Float,
    gw: Float,
    gh: Float,
    w: Float,
): Soup {
    val soup = Soup(8192)
    // squashing only ever makes screen distance smaller than surface distance, so a
    // euclidean search out to `base` can never miss a neighbour we'd have rejected
    val grid = Buckets(ox, oy, gw, gh, base)
    val nrm = FloatArray(3)
    val active = ArrayList<Int>()

    val sx = ox + gw * 0.5f
    val sy = oy + gh * 0.5f
    soup.add(sx, sy, spacingAt(m, p, light, base, sx, sy, w, nrm))
    grid.put(0, sx, sy)
    active.add(0)

    while (active.isNotEmpty()) {
        val ai = rnd.nextInt(active.size)
        val i = active[ai]
        val px = soup.x[i]
        val py = soup.y[i]
        val ps = soup.s[i]
        m.normalInto(px / w, py / w, nrm)
        var gx = nrm[0]
        var gy = nrm[1]
        val gl = sqrt(gx * gx + gy * gy)
        if (gl > 1e-5f) {
            gx /= gl
            gy /= gl
        } else {
            gx = 1f
            gy = 0f
        }
        val squash = squashOf(p, nrm[2])

        var placed = false
        for (k in 0 until TRIES) {
            val ang = rnd.nextFloat() * TAUf
            val d = ps * (1f + rnd.nextFloat())
            val a = d * cos(ang) * squash
            val b = d * sin(ang)
            val qx = px + a * gx - b * gy
            val qy = py + a * gy + b * gx
            if (qx < ox || qx >= ox + gw || qy < oy || qy >= oy + gh) continue

            val qs = spacingAt(m, p, light, base, qx, qy, w, nrm)
            var qgx = nrm[0]
            var qgy = nrm[1]
            val qgl = sqrt(qgx * qgx + qgy * qgy)
            if (qgl > 1e-5f) {
                qgx /= qgl
                qgy /= qgl
            } else {
                qgx = 1f
                qgy = 0f
            }
            val qsq = squashOf(p, nrm[2])

            var ok = true
            grid.near(qx, qy, 2) { j ->
                if (ok) {
                    val dx = soup.x[j] - qx
                    val dy = soup.y[j] - qy
                    val along = (dx * qgx + dy * qgy) / qsq
                    val perp = -dx * qgy + dy * qgx
                    if (sqrt(along * along + perp * perp) < max(qs, soup.s[j])) ok = false
                }
            }
            if (!ok) continue

            soup.add(qx, qy, qs)
            val id = soup.n - 1
            grid.put(id, qx, qy)
            active.add(id)
            placed = true
            break
        }
        if (!placed) active.removeAt(ai)
    }
    return soup
}

/**
 * push neighbours apart until they sit at their target spacing. dart throwing gives blue
 * noise, which looks scattered-
 */
private fun relax(
    soup: Soup,
    m: Membrane,
    p: Params,
    light: FloatArray,
    base: Float,
    ox: Float,
    oy: Float,
    gw: Float,
    gh: Float,
    w: Float,
) {
    if (p.relax <= 0) return
    val grid = Buckets(ox, oy, gw, gh, base)
    val fx = FloatArray(soup.n)
    val fy = FloatArray(soup.n)
    val nrm = FloatArray(3)

    repeat(p.relax) {
        grid.clear()
        for (i in 0 until soup.n) grid.put(i, soup.x[i], soup.y[i])
        java.util.Arrays.fill(fx, 0f)
        java.util.Arrays.fill(fy, 0f)

        for (i in 0 until soup.n) {
            val xi = soup.x[i]
            val yi = soup.y[i]
            val si = soup.s[i]
            m.normalInto(xi / w, yi / w, nrm)
            var gx = nrm[0]
            var gy = nrm[1]
            val gl = sqrt(gx * gx + gy * gy)
            if (gl > 1e-5f) {
                gx /= gl
                gy /= gl
            } else {
                gx = 1f
                gy = 0f
            }
            val squash = squashOf(p, nrm[2])
            var ax = 0f
            var ay = 0f

            grid.near(xi, yi, 1) { j ->
                if (j != i) {
                    val dx = xi - soup.x[j]
                    val dy = yi - soup.y[j]
                    val along = (dx * gx + dy * gy) / squash
                    val perp = -dx * gy + dy * gx
                    val d = sqrt(along * along + perp * perp)
                    val want = max(si, soup.s[j])
                    if (d > 1e-4f && d < want) {
                        val push = (want - d) / d
                        val fa = along * push
                        val fp = perp * push
                        ax += fa * squash * gx - fp * gy
                        ay += fa * squash * gy + fp * gx
                    }
                }
            }
            fx[i] = ax
            fy[i] = ay
        }

        for (i in 0 until soup.n) {
            soup.x[i] = (soup.x[i] + fx[i] * RELAX_RATE).coerceIn(ox, ox + gw - 0.01f)
            soup.y[i] = (soup.y[i] + fy[i] * RELAX_RATE).coerceIn(oy, oy + gh - 0.01f)
            soup.s[i] = spacingAt(m, p, light, base, soup.x[i], soup.y[i], w, nrm)
        }
    }
}

private const val RELAX_RATE = 0.35f

/**
 * the voronoi cell of each site
 */
private fun cells(
    soup: Soup,
    m: Membrane,
    p: Params,
    base: Float,
    ox: Float,
    oy: Float,
    gw: Float,
    gh: Float,
    w: Float,
    h: Float,
): List<Facet> {
    val grid = Buckets(ox, oy, gw, gh, base)
    for (i in 0 until soup.n) grid.put(i, soup.x[i], soup.y[i])

    val out = ArrayList<Facet>(soup.n)
    val nrm = FloatArray(3)
    val bufA = FloatArray(MAXV * 2)
    val bufB = FloatArray(MAXV * 2)
    val nbr = IntArray(256)

    for (i in 0 until soup.n) {
        val xi = soup.x[i]
        val yi = soup.y[i]
        // sites out in the margin exist only so the frame edge gets proper neighbours
        if (xi < -base || xi > w + base || yi < -base || yi > h + base) continue

        val si = soup.s[i]
        m.normalInto(xi / w, yi / w, nrm)
        val grout = p.grout * si
        val half = si * 1.7f

        var src = bufA
        var dst = bufB
        var n = 4
        src[0] = xi - half; src[1] = yi - half
        src[2] = xi + half; src[3] = yi - half
        src[4] = xi + half; src[5] = yi + half
        src[6] = xi - half; src[7] = yi + half

        var nc = 0
        grid.near(xi, yi, 2) { j ->
            if (j != i && nc < nbr.size) nbr[nc++] = j
        }

        for (k in 0 until nc) {
            val j = nbr[k]
            var ux = soup.x[j] - xi
            var uy = soup.y[j] - yi
            val len = sqrt(ux * ux + uy * uy)
            if (len < 1e-4f) continue
            ux /= len
            uy /= len
            val mx = (xi + soup.x[j]) * 0.5f
            val my = (yi + soup.y[j]) * 0.5f
            n = clipHalfPlane(src, n, ux, uy, mx * ux + my * uy - grout * 0.5f, dst)
            val t = src
            src = dst
            dst = t
            if (n < 3) break
        }

        if (n >= 3) {
            out.add(Facet(out.size, xi, yi, nrm[0], nrm[1], nrm[2], si * 0.6f, src.copyOf(n * 2)))
        }
    }
    return out
}

/** sutherland-hodgman, one plane */
private fun clipHalfPlane(src: FloatArray, n: Int, ux: Float, uy: Float, d: Float, dst: FloatArray): Int {
    var m = 0
    var ax = src[(n - 1) * 2]
    var ay = src[(n - 1) * 2 + 1]
    var da = ax * ux + ay * uy - d
    for (i in 0 until n) {
        val bx = src[i * 2]
        val by = src[i * 2 + 1]
        val db = bx * ux + by * uy - d
        if (db <= 0f) {
            if (da > 0f && m < MAXV) {
                val t = da / (da - db)
                dst[m * 2] = ax + (bx - ax) * t
                dst[m * 2 + 1] = ay + (by - ay) * t
                m++
            }
            if (m < MAXV) {
                dst[m * 2] = bx
                dst[m * 2 + 1] = by
                m++
            }
        } else if (da <= 0f && m < MAXV) {
            val t = da / (da - db)
            dst[m * 2] = ax + (bx - ax) * t
            dst[m * 2 + 1] = ay + (by - ay) * t
            m++
        }
        ax = bx
        ay = by
        da = db
    }
    return m
}
