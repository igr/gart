package pinna

import dev.oblac.gart.gfx.Circle
import dev.oblac.gart.math.between
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.RRect
import org.jetbrains.skia.PathFillMode
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

internal data class Canopy(val cx: Float, val top: Float, val w: Float, val h: Float, val r: Float) {
    val left get() = cx - w / 2f
    val right get() = cx + w / 2f
    val bottom get() = top + h
    val cy get() = top + h / 2f
}

internal fun canopyOf(p: Params, w: Int, h: Int): Canopy {
    val cw = p.canopyW * w
    val ch = p.canopyH * h
    val r = min(p.canopyR * cw, min(cw, ch) / 2f)
    return Canopy(p.canopyX * w, p.canopyY * h, cw, ch, r)
}

internal fun Canopy.sd(x: Float, y: Float): Float {
    val qx = abs(x - cx) - (w / 2f - r)
    val qy = abs(y - cy) - (h / 2f - r)
    val outside = hypot(max(qx, 0f), max(qy, 0f))
    val inside = min(max(qx, qy), 0f)
    return outside + inside - r
}

internal fun Canopy.rrect(): RRect = RRect.makeLTRB(left, top, right, bottom, r)

internal class Twig(var pos: Point, val parent: Twig?) {
    val kids = mutableListOf<Twig>()
    var radius = 0f
    val isTip get() = kids.isEmpty()
}

internal class Skeleton(val root: Twig) {
    fun nodes(): List<Twig> {
        val out = mutableListOf<Twig>()
        fun walk(t: Twig) {
            out.add(t)
            t.kids.forEach { walk(it) }
        }
        walk(root)
        return out
    }

    fun tips(): List<Twig> = nodes().filter { it.isTip }
}

private const val MAX_ITER = 400

internal fun growSkeleton(canopy: Canopy, p: Params, rng: Random, nz: Float, w: Int, h: Int): Skeleton {
    val seg = p.segLen * canopy.w
    val influence = p.influence * canopy.w
    val kill = p.kill * canopy.w
    val attractors = scatterAttractors(canopy, p, rng).toMutableList()

    val root = Twig(Point(p.trunkX * w, p.trunkY * h), null)
    val nodes = mutableListOf(root)

    var iter = 0
    while (attractors.isNotEmpty() && iter < MAX_ITER) {
        iter++

        val pull = arrayOfNulls<Vec2>(nodes.size)
        var pulled = 0
        for (a in attractors) {
            var best = -1
            var bestD = influence
            for (i in nodes.indices) {
                val n = nodes[i]
                val d = hypot(a.x - n.pos.x, a.y - n.pos.y)
                if (d < bestD) {
                    bestD = d
                    best = i
                }
            }
            if (best >= 0) {
                val v = Vec2(a.x - nodes[best].pos.x, a.y - nodes[best].pos.y).normalize()
                if (pull[best] == null) pulled++
                pull[best] = (pull[best] ?: Vec2.ZERO) + v
            }
        }

        if (pulled == 0) {
            val n = nodes.last()
            var sx = 0f
            var sy = 0f
            attractors.forEach { sx += it.x; sy += it.y }
            val dir = Vec2(sx / attractors.size - n.pos.x, sy / attractors.size - n.pos.y).normalize()
            nodes.add(spawn(n, dir, seg, p, nz))
            continue
        }

        val grown = nodes.size
        for (i in 0 until grown) {
            val v = pull[i] ?: continue
            val dir = v.normalize()
            nodes.add(spawn(nodes[i], dir, seg, p, nz))
        }

        attractors.removeAll { a -> nodes.any { hypot(a.x - it.pos.x, a.y - it.pos.y) < kill } }
    }
    val skeleton = Skeleton(root)
    skeleton.prune(p.tips)
    skeleton.smooth(p.smooth)
    skeleton.setRadii(p, w)
    return skeleton
}

private fun spawn(parent: Twig, dir: Vec2, seg: Float, p: Params, nz: Float): Twig {
    val f = 0.004f
    val n = SimplexNoise.noise(parent.pos.x * f + nz, parent.pos.y * f + nz)
    val d = dir.rotate(n * p.wander)
    val child = Twig(Point(parent.pos.x + d.x * seg, parent.pos.y + d.y * seg), parent)
    parent.kids.add(child)
    return child
}

private fun scatterAttractors(canopy: Canopy, p: Params, rng: Random): List<Point> {
    val out = mutableListOf<Point>()
    val cols = max(2, sqrt(p.attractors * canopy.w / canopy.h).toInt())
    val rows = max(2, p.attractors / cols + 1)
    val cw = canopy.w / cols
    val chh = canopy.h / rows
    for (j in 0 until rows) {
        for (i in 0 until cols) {
            val x = canopy.left + (i + rng.between(0.15f, 0.85f)) * cw
            val y = canopy.top + (j + rng.between(0.15f, 0.85f)) * chh
            if (canopy.sd(x, y) < 0f) out.add(Point(x, y))
        }
    }
    out.shuffle(rng)
    return if (out.size > p.attractors) out.subList(0, p.attractors).toList() else out
}

private fun twigLength(tip: Twig): Float {
    var len = 0f
    var n = tip
    while (true) {
        val parent = n.parent ?: return len
        len += hypot(n.pos.x - parent.pos.x, n.pos.y - parent.pos.y)
        if (parent.kids.size > 1) return len
        n = parent
    }
}

internal fun Skeleton.prune(targetTips: Int) {
    while (true) {
        val tips = tips()
        if (tips.size <= targetTips) return
        val victim = tips.minByOrNull { twigLength(it) } ?: return
        var n = victim
        while (true) {
            val parent = n.parent ?: return
            parent.kids.remove(n)
            if (parent.kids.isNotEmpty() || parent.parent == null) break
            n = parent
        }
    }
}

private const val SMOOTH_W = 0.5f

internal fun Skeleton.smooth(passes: Int) {
    repeat(passes) {
        val ns = nodes()
        val next = arrayOfNulls<Point>(ns.size)
        for (i in ns.indices) {
            val n = ns[i]
            val parent = n.parent ?: continue
            var sx = parent.pos.x
            var sy = parent.pos.y
            var count = 1
            for (k in n.kids) {
                sx += k.pos.x
                sy += k.pos.y
                count++
            }
            val ax = sx / count
            val ay = sy / count
            next[i] = Point(n.pos.x + (ax - n.pos.x) * SMOOTH_W, n.pos.y + (ay - n.pos.y) * SMOOTH_W)
        }
        for (i in ns.indices) next[i]?.let { ns[i].pos = it }
    }
}

internal fun Skeleton.setRadii(p: Params, w: Int) {
    val tipR = p.tipR * w
    fun walk(t: Twig): Float {
        if (t.isTip) {
            t.radius = tipR
            return tipR
        }
        var sum = 0.0
        for (k in t.kids) sum += walk(k).toDouble().pow(p.taper.toDouble())
        t.radius = sum.pow(1.0 / p.taper).toFloat()
        return t.radius
    }
    walk(root)
}

internal fun soleY(p: Params, h: Int): Float = p.trunkY * h

internal fun flareTop(p: Params, h: Int): Float = soleY(p, h) - p.footRun * h

private fun radiusAt(pos: Point, base: Float, side: Float, p: Params, h: Int): Float {
    if (p.foot <= 0f) return base
    val ft = flareTop(p, h)
    if (pos.y <= ft) return base
    val sy = soleY(p, h)
    val t = ((pos.y - ft) / (sy - ft)).coerceIn(0f, 1f)
    val skew = 1f + side * p.footSkew
    return base * (1f + p.foot * skew * t.pow(p.footPow))
}

internal fun Skeleton.discs(p: Params, h: Int): List<Circle> = nodes().map {
    Circle(it.pos.x, it.pos.y, max(radiusAt(it.pos, it.radius, 1f, p, h), radiusAt(it.pos, it.radius, -1f, p, h)))
}

internal fun Skeleton.branchPath(p: Params, h: Int): Path {
    val pb = PathBuilder()
    pb.setFillType(PathFillMode.WINDING)

    fun chainNormal(node: Twig): Vec2? {
        val par = node.parent
        if (par == null || node.pos.y <= flareTop(p, h) || node.kids.size != 1) return null
        val kid = node.kids[0]
        val idx = node.pos.x - par.pos.x
        val idy = node.pos.y - par.pos.y
        val ilen = hypot(idx, idy)
        val odx = kid.pos.x - node.pos.x
        val ody = kid.pos.y - node.pos.y
        val olen = hypot(odx, ody)
        if (ilen < 1e-4f || olen < 1e-4f) return null
        val aux = idx / ilen + odx / olen
        val auy = idy / ilen + ody / olen
        val alen = hypot(aux, auy)
        if (alen < 1e-4f) return null
        return Vec2(-auy / alen, aux / alen)
    }

    for (t in nodes()) {
        val parent = t.parent ?: continue
        val dx = t.pos.x - parent.pos.x
        val dy = t.pos.y - parent.pos.y
        val len = hypot(dx, dy)
        if (len < 1e-4f) continue
        val nx = -dy / len
        val ny = dx / len
        val ap = radiusAt(parent.pos, parent.radius, 1f, p, h)
        val am = radiusAt(parent.pos, parent.radius, -1f, p, h)
        val bp = radiusAt(t.pos, t.radius, 1f, p, h)
        val bm = radiusAt(t.pos, t.radius, -1f, p, h)
        val (pnx, pny) = chainNormal(parent) ?: Vec2(nx, ny)
        val (cnx, cny) = chainNormal(t) ?: Vec2(nx, ny)
        pb.addPoly(
            floatArrayOf(
                parent.pos.x + pnx * ap, parent.pos.y + pny * ap,
                parent.pos.x - pnx * am, parent.pos.y - pny * am,
                t.pos.x - cnx * bm, t.pos.y - cny * bm,
                t.pos.x + cnx * bp, t.pos.y + cny * bp,
            ),
            true,
        )
        pb.addCircle(t.pos.x, t.pos.y, min(bp, bm))
        pb.addCircle(parent.pos.x, parent.pos.y, min(ap, am))
    }
    val path = pb.detach()
    pb.close()
    return path.also { it.fillMode = PathFillMode.WINDING }
}
