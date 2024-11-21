@file:Suppress("t")

package dev.oblac.gart.triangulation

import dev.oblac.gart.gfx.Triangle
import org.jetbrains.skia.Point
import kotlin.math.*

private val EPSILON: Float = 2.0.pow(-52).toFloat()
private val EDGE_STACK = IntArray(512)

/**
 * A Kotlin port of Mapbox's Delaunator incredibly fast JavaScript library for Delaunay triangulation of 2D points.
 *
 * @description Port of Mapbox's Delaunator (JavaScript) library - https://github.com/mapbox/delaunator
 * @author Ricardo Matias
 */
@Suppress("unused")
class Delaunator(private val points: List<Point>) {
    private var count = points.size

    // arrays that will store the triangulation graph
    private val maxTriangles = (2 * count - 5).coerceAtLeast(0)
    private val _triangles = IntArray(maxTriangles * 3)
    private val _halfEdges = IntArray(maxTriangles * 3)

    private lateinit var triangles: IntArray
    private lateinit var halfEdges: IntArray

    // temporary arrays for tracking the edges of the advancing convex hull
    private var hashSize = ceil(sqrt(count * 1.0)).toInt()
    private var hullPrev = IntArray(count) // edge to prev edge
    private var hullNext = IntArray(count) // edge to next edge
    private var hullTri = IntArray(count) // edge to adjacent triangle
    private var hullHash = IntArray(hashSize) // angular edge hash
    private var hullStart: Int = -1

    // temporary arrays for sorting points
    private var ids = IntArray(count)
    private var dists = FloatArray(count)

    private var cx: Float = Float.NaN
    private var cy: Float = Float.NaN

    private var trianglesLen: Int = -1

    private lateinit var hull: IntArray


    fun triangles(): List<Triangle> {
        val tris = mutableListOf<Triangle>()

        for (i in triangles.indices step 3) {
            val p1 = points[triangles[i]]
            val p2 = points[triangles[i + 1]]
            val p3 = points[triangles[i + 2]]

            tris.add(Triangle(p1, p2, p3))
        }

        return tris
    }

    init {
        update()
    }

    fun update() {
        // populate an array of point indices calculate input data bbox
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        // points -> points
        // minX, minY, maxX, maxY
        for (i in 0 until count) {
            val x = points[i].x
            val y = points[i].y
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y

            ids[i] = i
        }

        val cx = (minX + maxX) / 2
        val cy = (minY + maxY) / 2

        var minDist = Float.POSITIVE_INFINITY

        var i0: Int = -1
        var i1: Int = -1
        var i2: Int = -1

        // pick a seed point close to the center
        for (i in 0 until count) {
            val d = dist(cx, cy, points[i].x, points[i].y)

            if (d < minDist) {
                i0 = i
                minDist = d
            }
        }

        val i0x = points[i0].x
        val i0y = points[i0].y

        minDist = Float.POSITIVE_INFINITY

        // Find the point closest to the seed
        for (i in 0 until count) {
            if (i == i0) continue

            val d = dist(i0x, i0y, points[i].x, points[i].y)

            if (d < minDist && d > 0) {
                i1 = i
                minDist = d
            }
        }

        var i1x = points[i1].x
        var i1y = points[i1].y

        var minRadius = Float.POSITIVE_INFINITY

        // Find the third point which forms the smallest circumcircle with the first two
        for (i in 0 until count) {
            if (i == i0 || i == i1) continue

            val r = circumradius(i0x, i0y, i1x, i1y, points[i].x, points[i].y)

            if (r < minRadius) {
                i2 = i
                minRadius = r
            }
        }

        if (minRadius == Float.POSITIVE_INFINITY) {
            // order collinear points by dx (or dy if all x are identical)
            // and return the list as a hull
            for (i in 0 until count) {
                val a = (points[i].x - points[0].x)
                val b = (points[i].y - points[0].y)
                dists[i] = if (a == 0.0f) b else a
            }

            quicksort(ids, dists, 0, count - 1)

            val nhull = IntArray(count)
            var j = 0
            var d0 = Float.NEGATIVE_INFINITY

            for (i in 0 until count) {
                val id = ids[i]
                if (dists[id] > d0) {
                    nhull[j++] = id
                    d0 = dists[id]
                }
            }

            hull = nhull.copyOf(j)
            triangles = IntArray(0)
            halfEdges = IntArray(0)

            return
        }

        var i2x = points[i2].x
        var i2y = points[i2].y

        // swap the order of the seed points for counter-clockwise orientation
        if (orient(i0x, i0y, i1x, i1y, i2x, i2y) < 0.0) {
            val i = i1
            val x = i1x
            val y = i1y
            i1 = i2
            i1x = i2x
            i1y = i2y
            i2 = i
            i2x = x
            i2y = y
        }


        val center = circumcenter(i0x, i0y, i1x, i1y, i2x, i2y)

        this.cx = center[0]
        this.cy = center[1]

        for (i in 0 until count) {
            dists[i] = dist(points[i].x, points[i].y, center[0], center[1])
        }

        // sort the points by distance from the seed triangle circumcenter
        quicksort(ids, dists, 0, count - 1)

        // set up the seed triangle as the starting hull
        hullStart = i0
        var hullSize = 3

        hullNext[i0] = i1
        hullNext[i1] = i2
        hullNext[i2] = i0

        hullPrev[i2] = i1
        hullPrev[i0] = i2
        hullPrev[i1] = i0

        hullTri[i0] = 0
        hullTri[i1] = 1
        hullTri[i2] = 2

        hullHash.fill(-1)
        hullHash[hashKey(i0x, i0y)] = i0
        hullHash[hashKey(i1x, i1y)] = i1
        hullHash[hashKey(i2x, i2y)] = i2

        trianglesLen = 0
        addTriangle(i0, i1, i2, -1, -1, -1)

        var xp = 0.0f
        var yp = 0.0f

        for (k in ids.indices) {
            val i = ids[k]
            val x = points[i].x
            val y = points[i].y

            // skip near-duplicate points
            if (k > 0 && abs(x - xp) <= EPSILON && abs(y - yp) <= EPSILON) continue

            xp = x
            yp = y

            // skip seed triangle points
            if (i == i0 || i == i1 || i == i2) continue

            // find a visible edge on the convex hull using edge hash
            var start = 0
            val key = hashKey(x, y)

            for (j in 0 until hashSize) {
                start = hullHash[(key + j) % hashSize]

                if (start != -1 && start != hullNext[start]) break
            }

            start = hullPrev[start]

            var e = start
            var q = hullNext[e]

            while (orient(x, y, points[e].x, points[e].y, points[q].x, points[q].y) >= 0.0) {
                e = q

                if (e == start) {
                    e = -1
                    break
                }

                q = hullNext[e]
            }

            if (e == -1) continue // likely a near-duplicate point skip it

            // add the first triangle from the point
            var t = addTriangle(e, i, hullNext[e], -1, -1, hullTri[e])

            // recursively flip triangles from the point until they satisfy the Delaunay condition
            hullTri[i] = legalize(t + 2)
            hullTri[e] = t // keep track of boundary triangles on the hull
            hullSize++

            // walk forward through the hull, adding more triangles and flipping recursively
            var next = hullNext[e]
            q = hullNext[next]

            while (orient(x, y, points[next].x, points[next].y, points[q].x, points[q].y) < 0.0) {
                t = addTriangle(next, i, q, hullTri[i], -1, hullTri[next])
                hullTri[i] = legalize(t + 2)
                hullNext[next] = next // mark as removed
                hullSize--

                next = q
                q = hullNext[next]
            }

            // walk backward from the other side, adding more triangles and flipping
            if (e == start) {
                q = hullPrev[e]

                while (orient(x, y, points[q].x, points[q].y, points[e].x, points[e].y) < 0.0) {
                    t = addTriangle(q, i, e, -1, hullTri[e], hullTri[q])
                    legalize(t + 2)
                    hullTri[q] = t
                    hullNext[e] = e // mark as removed
                    hullSize--

                    e = q
                    q = hullPrev[e]
                }
            }

            // update the hull indices
            hullStart = e
            hullPrev[i] = e

            hullNext[e] = i
            hullPrev[next] = i
            hullNext[i] = next

            // save the two new edges in the hash table
            hullHash[hashKey(x, y)] = i
            hullHash[hashKey(points[e].x, points[e].y)] = e
        }

        hull = IntArray(hullSize)

        var e = hullStart

        for (i in 0 until hullSize) {
            hull[i] = e
            e = hullNext[e]
        }

        // trim typed triangle mesh arrays
        triangles = _triangles.copyOf(trianglesLen)
        halfEdges = _halfEdges.copyOf(trianglesLen)
    }

    private fun legalize(a: Int): Int {
        var i = 0
        var na = a
        var ar: Int

        // recursion eliminated with a fixed-size stack
        while (true) {
            val b = _halfEdges[na]

            /* if the pair of triangles doesn't satisfy the Delaunay condition
             * (p1 is inside the circumcircle of [p0, pl, pr]), flip them,
             * then do the same check/flip recursively for the new pair of triangles
             *
             *           pl                    pl
             *          /||\                  /  \
             *       al/ || \bl            al/    \a
             *        /  ||  \              /      \
             *       /  a||b  \    flip    /___ar___\
             *     p0\   ||   /p1   =>   p0\---bl---/p1
             *        \  ||  /              \      /
             *       ar\ || /br             b\    /br
             *          \||/                  \  /
             *           pr                    pr
             */
            val a0 = na - na % 3
            ar = a0 + (na + 2) % 3

            if (b == -1) { // convex hull edge
                if (i == 0) break
                na = EDGE_STACK[--i]
                continue
            }

            val b0 = b - b % 3
            val al = a0 + (na + 1) % 3
            val bl = b0 + (b + 2) % 3

            val p0 = _triangles[ar]
            val pr = _triangles[na]
            val pl = _triangles[al]
            val p1 = _triangles[bl]

            val illegal = inCircle(
                points[p0].x, points[p0].y,
                points[pr].x, points[pr].y,
                points[pl].x, points[pl].y,
                points[p1].x, points[p1].y
            )

            if (illegal) {
                _triangles[na] = p1
                _triangles[b] = p0

                val hbl = _halfEdges[bl]

                // edge swapped on the other side of the hull (rare) fix the halfedge reference
                if (hbl == -1) {
                    var e = hullStart
                    do {
                        if (hullTri[e] == bl) {
                            hullTri[e] = na
                            break
                        }
                        e = hullPrev[e]
                    } while (e != hullStart)
                }
                link(na, hbl)
                link(b, _halfEdges[ar])
                link(ar, bl)

                val br = b0 + (b + 1) % 3

                // don't worry about hitting the cap: it can only happen on extremely degenerate input
                if (i < EDGE_STACK.size) {
                    EDGE_STACK[i++] = br
                }
            } else {
                if (i == 0) break
                na = EDGE_STACK[--i]
            }
        }

        return ar
    }

    private fun link(a: Int, b: Int) {
        _halfEdges[a] = b
        if (b != -1) _halfEdges[b] = a
    }

    // add a new triangle given vertex indices and adjacent half-edge ids
    private fun addTriangle(i0: Int, i1: Int, i2: Int, a: Int, b: Int, c: Int): Int {
        val t = trianglesLen

        _triangles[t] = i0
        _triangles[t + 1] = i1
        _triangles[t + 2] = i2

        link(t, a)
        link(t + 1, b)
        link(t + 2, c)

        trianglesLen += 3

        return t
    }

    private fun hashKey(x: Float, y: Float): Int {
        return (floor(pseudoAngle(x - cx, y - cy) * hashSize) % hashSize).toInt()
    }
}

fun circumradius(
    ax: Float, ay: Float,
    bx: Float, by: Float,
    cx: Float, cy: Float
): Float {
    val dx = bx - ax
    val dy = by - ay
    val ex = cx - ax
    val ey = cy - ay

    val bl = dx * dx + dy * dy
    val cl = ex * ex + ey * ey
    val d = 0.5f / (dx * ey - dy * ex)

    val x = (ey * bl - dy * cl) * d
    val y = (dx * cl - ex * bl) * d

    return x * x + y * y
}

fun circumcenter(
    ax: Float, ay: Float,
    bx: Float, by: Float,
    cx: Float, cy: Float
): FloatArray {
    val dx = bx - ax
    val dy = by - ay
    val ex = cx - ax
    val ey = cy - ay

    val bl = dx * dx + dy * dy
    val cl = ex * ex + ey * ey
    val d = 0.5f / (dx * ey - dy * ex)

    val x = ax + (ey * bl - dy * cl) * d
    val y = ay + (dx * cl - ex * bl) * d

    return floatArrayOf(x, y)
}

fun quicksort(ids: IntArray, dists: FloatArray, left: Int, right: Int) {
    if (right - left <= 20) {
        for (i in (left + 1)..right) {
            val temp = ids[i]
            val tempDist = dists[temp]
            var j = i - 1
            while (j >= left && dists[ids[j]] > tempDist) ids[j + 1] = ids[j--]
            ids[j + 1] = temp
        }
    } else {
        val median = (left + right) shr 1
        var i = left + 1
        var j = right

        swap(ids, median, i)

        if (dists[ids[left]] > dists[ids[right]]) swap(ids, left, right)
        if (dists[ids[i]] > dists[ids[right]]) swap(ids, i, right)
        if (dists[ids[left]] > dists[ids[i]]) swap(ids, left, i)

        val temp = ids[i]
        val tempDist = dists[temp]

        while (true) {
            do i++ while (dists[ids[i]] < tempDist)
            do j-- while (dists[ids[j]] > tempDist)
            if (j < i) break
            swap(ids, i, j)
        }

        ids[left + 1] = ids[j]
        ids[j] = temp

        if (right - i + 1 >= j - left) {
            quicksort(ids, dists, i, right)
            quicksort(ids, dists, left, j - 1)
        } else {
            quicksort(ids, dists, left, j - 1)
            quicksort(ids, dists, i, right)
        }
    }
}

private fun swap(arr: IntArray, i: Int, j: Int) {
    val tmp = arr[i]
    arr[i] = arr[j]
    arr[j] = tmp
}

// return 2d orientation sign if we're confident in it through J. Shewchuk's error bound check
private fun orientIfSure(px: Float, py: Float, rx: Float, ry: Float, qx: Float, qy: Float): Float {
    val l = (ry - py) * (qx - px)
    val r = (rx - px) * (qy - py)
    return if (abs(l - r) >= (3.3306690738754716e-16.toFloat() * abs(l + r))) l - r else 0.0f
}

// a more robust orientation test that's stable in a given triangle (to fix robustness issues)
private fun orient(rx: Float, ry: Float, qx: Float, qy: Float, px: Float, py: Float): Float {
    val a = orientIfSure(px, py, rx, ry, qx, qy)
    val b = orientIfSure(rx, ry, qx, qy, px, py)
    val c = orientIfSure(qx, qy, px, py, rx, ry)

    return if (!a.isFalsy()) {
        a
    } else {
        if (!b.isFalsy()) {
            b
        } else {
            c
        }
    }
}

// monotonically increases with real angle, but doesn't need expensive trigonometry
private fun pseudoAngle(dx: Float, dy: Float): Float {
    val p = dx / (abs(dx) + abs(dy))
    val a = if (dy > 0.0f) 3.0f - p else 1.0f + p

    return a / 4.0f // [0..1]
}

private fun inCircle(
    ax: Float, ay: Float,
    bx: Float, by: Float,
    cx: Float, cy: Float,
    px: Float, py: Float
): Boolean {
    val dx = ax - px
    val dy = ay - py
    val ex = bx - px
    val ey = by - py
    val fx = cx - px
    val fy = cy - py

    val ap = dx * dx + dy * dy
    val bp = ex * ex + ey * ey
    val cp = fx * fx + fy * fy

    return dx * (ey * cp - bp * fy) -
        dy * (ex * cp - bp * fx) +
        ap * (ex * fy - ey * fx) < 0
}

private fun dist(ax: Float, ay: Float, bx: Float, by: Float): Float {
    val dx = ax - bx
    val dy = ay - by
    return dx * dx + dy * dy
}

private fun Float?.isFalsy() = this == null || this == -0.0f || this == 0.0f || isNaN()
