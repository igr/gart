package pinna

import dev.oblac.gart.gfx.Circle
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

internal class Field(
    val canopy: Canopy,
    val cell: Float,
    val originX: Float,
    val originY: Float,
    val gw: Int,
    val gh: Int,
) {
    val blocked = ByteArray(gw * gh)

    val sdf = ShortArray(gw * gh)

    fun idx(gx: Int, gy: Int) = gy * gw + gx
    fun gx(x: Float) = ((x - originX) / cell).toInt()
    fun gy(y: Float) = ((y - originY) / cell).toInt()
    fun wx(gx: Int) = originX + (gx + 0.5f) * cell
    fun wy(gy: Int) = originY + (gy + 0.5f) * cell
    fun inBounds(gx: Int, gy: Int) = gx in 0 until gw && gy in 0 until gh
}

internal fun fieldOf(canopy: Canopy, discs: List<Circle>, p: Params): Field {
    val cell = canopy.w / p.packRes
    val pad = cell * 4f
    val originX = canopy.left - pad
    val originY = canopy.top - pad
    val gw = ceil((canopy.w + 2 * pad) / cell).toInt()
    val gh = ceil((canopy.h + 2 * pad) / cell).toInt()
    val f = Field(canopy, cell, originX, originY, gw, gh)

    for (gy in 0 until gh) {
        for (gx in 0 until gw) {
            val d = canopy.sd(f.wx(gx), f.wy(gy)) / cell
            f.sdf[f.idx(gx, gy)] = d.coerceIn(-32000f, 32000f).roundToInt().toShort()
        }
    }

    for (c in discs) {
        val x0 = max(0, f.gx(c.x - c.radius))
        val x1 = min(gw - 1, f.gx(c.x + c.radius))
        val y0 = max(0, f.gy(c.y - c.radius))
        val y1 = min(gh - 1, f.gy(c.y + c.radius))
        val r2 = c.radius * c.radius
        for (gy in y0..y1) {
            for (gx in x0..x1) {
                val dx = f.wx(gx) - c.x
                val dy = f.wy(gy) - c.y
                if (dx * dx + dy * dy <= r2) f.blocked[f.idx(gx, gy)] = 1
            }
        }
    }
    return f
}

private const val INF = 1e20f

internal fun branchDistance(field: Field): FloatArray {
    val gw = field.gw
    val gh = field.gh
    val d = FloatArray(gw * gh) { if (field.blocked[it].toInt() != 0) 0f else INF }

    val n = max(gw, gh)
    val f = FloatArray(n)
    val dd = FloatArray(n)
    val v = IntArray(n)
    val z = FloatArray(n + 1)

    for (gx in 0 until gw) {
        for (gy in 0 until gh) f[gy] = d[gy * gw + gx]
        edt1d(f, dd, v, z, gh)
        for (gy in 0 until gh) d[gy * gw + gx] = dd[gy]
    }
    for (gy in 0 until gh) {
        val row = gy * gw
        for (gx in 0 until gw) f[gx] = d[row + gx]
        edt1d(f, dd, v, z, gw)
        for (gx in 0 until gw) d[row + gx] = sqrt(dd[gx])
    }
    return d
}

private fun edt1d(f: FloatArray, d: FloatArray, v: IntArray, z: FloatArray, n: Int) {
    var k = 0
    v[0] = 0
    z[0] = -INF
    z[1] = INF
    for (q in 1 until n) {
        var s = ((f[q] + q * q) - (f[v[k]] + v[k] * v[k])) / (2f * q - 2f * v[k])
        while (k > 0 && s <= z[k]) {
            k--
            s = ((f[q] + q * q) - (f[v[k]] + v[k] * v[k])) / (2f * q - 2f * v[k])
        }
        k++
        v[k] = q
        z[k] = s
        z[k + 1] = INF
    }
    k = 0
    for (q in 0 until n) {
        while (z[k + 1] < q) k++
        val dq = (q - v[k]).toFloat()
        d[q] = dq * dq + f[v[k]]
    }
}

internal class Blob(val x: Float, val y: Float, val r: Float, val tone: Int)

private const val SITE_STEP = 0.55f

internal fun packCircles(field: Field, dist: FloatArray, p: Params, rng: Random, h: Int): List<Blob> {
    val out = mutableListOf<Blob>()
    val maxR = p.circMax * h
    val minR = p.circMin * h
    val ratio = (minR / maxR).pow(1f / (p.circSteps - 1))
    val gap = p.circGap * h
    val clear = p.branchGap * h

    var r = maxR
    repeat(p.circSteps) {
        val stride = max(1, (r * SITE_STEP / field.cell).toInt())
        val sites = mutableListOf<Int>()
        var gy = 0
        while (gy < field.gh) {
            var gx = 0
            while (gx < field.gw) {
                sites.add(gy * field.gw + gx)
                gx += stride
            }
            gy += stride
        }
        sites.shuffle(rng)

        for (s in sites) {
            val bx = s % field.gw + rng.nextInt(stride)
            val by = s / field.gw + rng.nextInt(stride)
            if (!field.inBounds(bx, by)) continue
            val i = field.idx(bx, by)

            if (dist[i] * field.cell < r + clear) continue
            if (field.sdf[i] * field.cell > -r) continue

            val x = field.wx(bx)
            val y = field.wy(by)
            val reach = 1f - p.circOverlap
            var hits = false
            for (b in out) {
                if (hypot(x - b.x, y - b.y) < (r + b.r) * reach + gap) {
                    hits = true
                    break
                }
            }
            if (hits) continue

            out.add(Blob(x, y, r, rng.nextInt(p.folSteps)))
        }
        r *= ratio
    }
    return out
}
