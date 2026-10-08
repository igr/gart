package dev.oblac.gart.pixels

/**
 * The regions of a grid, from [labelRegions]. [label] has one value per cell, row by row: -1 for
 * a wall, else the id of the region of the cell, from 0 until [count].
 */
class Regions internal constructor(val label: IntArray, val count: Int, private val w: Int, private val h: Int) {

    /**
     * One flag per region: true when the region has a cell on the edge of the grid. A region
     * without such a cell is closed: walls go all the way around it.
     */
    fun touchesEdge(): BooleanArray {
        val edge = BooleanArray(count)
        fun mark(i: Int) {
            val id = label[i]
            if (id >= 0) edge[id] = true
        }
        for (x in 0 until w) {
            mark(x)
            mark((h - 1) * w + x)
        }
        for (y in 0 until h) {
            mark(y * w)
            mark(y * w + w - 1)
        }
        return edge
    }
}

/**
 * Splits a [w] x [h] grid into 4-connected regions. A cell where [wall] is true is in no region.
 * Two open cells that share a side are in the same region. Two cells that touch only at a corner
 * are not.
 *
 * The ids count up from 0 in the order of the first cell of each region, row by row. Thus the
 * same grid always gets the same ids. The ids go into [label], one per cell. Give an array of
 * w x h cells to use it again. Its old values do not matter.
 */
fun labelRegions(wall: BooleanArray, w: Int, h: Int, label: IntArray = IntArray(w * h)): Regions {
    val n = w * h
    require(wall.size == n) { "wall has ${wall.size} cells, not $w x $h" }
    require(label.size == n) { "label has ${label.size} cells, not $w x $h" }
    label.fill(-1)
    val queue = IntArray(n)
    var count = 0
    for (start in 0 until n) {
        if (wall[start] || label[start] >= 0) continue
        val id = count++
        var head = 0
        var tail = 0
        fun visit(q: Int) {
            if (wall[q] || label[q] >= 0) return
            label[q] = id
            queue[tail++] = q
        }
        visit(start)
        while (head < tail) {
            val p = queue[head++]
            val x = p % w
            if (x > 0) visit(p - 1)
            if (x < w - 1) visit(p + 1)
            if (p >= w) visit(p - w)
            if (p < n - w) visit(p + w)
        }
    }
    return Regions(label, count, w, h)
}
