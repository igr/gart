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

    /** The number of cells in each region, by id. */
    fun sizes(): IntArray = sizesOf(label, count)
}

/**
 * The regions of a 3D grid, from the 3D [labelRegions]. [label] and [count] work as in [Regions].
 */
class Regions3 internal constructor(val label: IntArray, val count: Int) {

    /** The number of cells in each region, by id. */
    fun sizes(): IntArray = sizesOf(label, count)
}

private fun sizesOf(label: IntArray, count: Int): IntArray {
    val sizes = IntArray(count)
    for (id in label) if (id >= 0) sizes[id]++
    return sizes
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

/**
 * [labelRegions] in 3D: splits a grid of [nx] x [ny] x [nz] cells into 6-connected regions. Two
 * open cells that share a face are in the same region. A shared edge or corner is not enough.
 *
 * The cells go as in 2D, row by row, and each cell of a row is a run of [nz] cells along z: z is
 * the fastest, then x, then y, as in `Grid3`. The index of a cell is `(y * nx + x) * nz + z`. With
 * `nz = 1` it is the 2D order. The ids and [label] work as in 2D.
 */
fun labelRegions(wall: BooleanArray, nx: Int, ny: Int, nz: Int, label: IntArray = IntArray(nx * ny * nz)): Regions3 {
    val n = nx * ny * nz
    require(wall.size == n) { "wall has ${wall.size} cells, not $nx x $ny x $nz" }
    require(label.size == n) { "label has ${label.size} cells, not $nx x $ny x $nz" }
    label.fill(-1)
    val queue = IntArray(n)
    val row = nx * nz
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
            val z = p % nz
            val x = p / nz % nx
            if (z > 0) visit(p - 1)
            if (z < nz - 1) visit(p + 1)
            if (x > 0) visit(p - nz)
            if (x < nx - 1) visit(p + nz)
            if (p >= row) visit(p - row)
            if (p < n - row) visit(p + row)
        }
    }
    return Regions3(label, count)
}
