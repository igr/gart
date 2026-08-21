package dev.oblac.gart.hashgrid

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * A dense uniform grid of buckets over the box at ([x0], [y0]), [cols] x [rows] cells of size
 * [cs], holding plain int ids. Built for hot loops: primitive arrays, nothing boxed, and a disc
 * is registered in every cell its bounding box touches, so a query only ever looks at its own
 * neighbourhood. Coordinates off the box clamp to the edge cells.
 *
 * Two ways to ask. [forEachCellTouching] with [forEachIn] for "who could be near this spot" -
 * the overlap test while packing. Or [ring] to walk outward one square ring at a time until some
 * bound says stop - a nearest-surface search. Because a disc is registered everywhere it
 * reaches, nothing first met on ring m is closer than (m - 1) * cs, which is what lets such a
 * search stop early.
 *
 * After [seal] the grid also keeps 2d prefix sums of its counts, so [ringEmpty] answers in o(1)
 * and [ring] skips an empty ring whole - worth it when the grid is sized for the smallest item
 * and the big empty stretches would otherwise be walked cell by cell. Adding again unseals.
 *
 * [HashGrid] is the sparse, point-based cousin for "is this spot free" over a few thousand points.
 */
class BucketGrid(private val x0: Float, private val y0: Float, val cs: Float, val cols: Int, val rows: Int) {
    @PublishedApi internal val items = arrayOfNulls<IntArray>(cols * rows)
    @PublishedApi internal val count = IntArray(cols * rows)
    private var cum: IntArray? = null

    fun col(x: Float) = floor((x - x0) / cs).toInt().coerceIn(0, cols - 1)
    fun row(y: Float) = floor((y - y0) / cs).toInt().coerceIn(0, rows - 1)
    fun cell(x: Float, y: Float) = row(y) * cols + col(x)

    /** Registers [id] in the one cell under ([x], [y]). */
    fun add(id: Int, x: Float, y: Float) = put(id, cell(x, y))

    /** Registers [id] in every cell the bounding box of the disc ([x], [y], [r]) touches. */
    fun add(id: Int, x: Float, y: Float, r: Float) = forEachCellTouching(x, y, r) { put(id, it) }

    private fun put(id: Int, c: Int) {
        var a = items[c]
        if (a == null) { a = IntArray(6); items[c] = a }
        else if (count[c] == a.size) { a = a.copyOf(a.size * 2); items[c] = a }
        a[count[c]++] = id
        cum = null
    }

    /** Every cell the bounding box of the disc ([x], [y], [r]) touches, row by row. */
    inline fun forEachCellTouching(x: Float, y: Float, r: Float, f: (cell: Int) -> Unit) {
        for (cy in row(y - r)..row(y + r)) for (cx in col(x - r)..col(x + r)) f(cy * cols + cx)
    }

    /** Every id registered in cell [c], in the order they were added. */
    inline fun forEachIn(c: Int, f: (id: Int) -> Unit) {
        val a = items[c] ?: return
        for (i in 0 until count[c]) f(a[i])
    }

    /** Builds the prefix sums behind [ringEmpty]. Call it once the grid is filled. */
    fun seal() {
        val s = cols + 1
        val c = IntArray(s * (rows + 1))
        for (y in 0 until rows) {
            var run = 0
            for (x in 0 until cols) {
                run += count[y * cols + x]
                c[(y + 1) * s + x + 1] = c[y * s + x + 1] + run
            }
        }
        cum = c
    }

    // entries registered anywhere in cells [ax..bx] x [ay..by], clamped to the grid
    private fun within(c: IntArray, ax0: Int, ay0: Int, bx0: Int, by0: Int): Int {
        val ax = max(ax0, 0)
        val ay = max(ay0, 0)
        val bx = min(bx0, cols - 1)
        val by = min(by0, rows - 1)
        if (ax > bx || ay > by) return 0
        val s = cols + 1
        return c[(by + 1) * s + bx + 1] - c[ay * s + bx + 1] - c[(by + 1) * s + ax] + c[ay * s + ax]
    }

    /** True when nothing is registered on ring [m] around ([cx], [cy]). Never true before [seal]. */
    fun ringEmpty(cx: Int, cy: Int, m: Int): Boolean {
        val c = cum ?: return false
        val outer = within(c, cx - m, cy - m, cx + m, cy + m)
        val inner = if (m > 0) within(c, cx - m + 1, cy - m + 1, cx + m - 1, cy + m - 1) else 0
        return outer == inner
    }

    /**
     * Walks ring [m] around cell ([cx], [cy]) - the square perimeter at that chebyshev distance -
     * calling [visit] per cell: top row left to right, then the two sides row by row, then the
     * bottom row. A sealed grid skips the ring whole when nothing lives on it. Returns false once
     * the ring has left the grid on all four sides, so a search knows to stop.
     */
    inline fun ring(cx: Int, cy: Int, m: Int, visit: (cell: Int) -> Unit): Boolean {
        val left = cx - m
        val right = cx + m
        val top = cy - m
        val bot = cy + m
        if (left < 0 && top < 0 && right >= cols && bot >= rows) return false
        if (ringEmpty(cx, cy, m)) return true
        for (gy in max(top, 0)..min(bot, rows - 1)) {
            if (gy == top || gy == bot) {
                for (gx in max(left, 0)..min(right, cols - 1)) visit(gy * cols + gx)
            } else {
                if (left >= 0) visit(gy * cols + left)
                if (right < cols) visit(gy * cols + right)
            }
        }
        return true
    }
}
