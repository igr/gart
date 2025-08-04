package dev.oblac.gart.hashgrid

import dev.oblac.gart.gfx.EMPTY
import dev.oblac.gart.gfx.ofXYWH
import dev.oblac.gart.gfx.squaredDistanceTo
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

private fun Float.fastFloor(): Int {
    return if (this >= 0) this.toInt() else this.toInt() - 1
}

private data class GridCoords(val x: Int, val y: Int) {
    fun offset(i: Int, j: Int): GridCoords = copy(x = x + i, y = y + j)
}

internal class Cell(val x: Int, val y: Int, val cellSize: Float) {
    var xMin = Float.POSITIVE_INFINITY
        private set
    var xMax = Float.NEGATIVE_INFINITY
        private set
    var yMin = Float.POSITIVE_INFINITY
        private set
    var yMax = Float.NEGATIVE_INFINITY
        private set

    val bounds: Rect
        get() {
            return Rect.ofXYWH(x * cellSize, y * cellSize, cellSize, cellSize)
        }

    val contentBounds: Rect
        get() {
            if (points.isEmpty()) {
                return Rect.EMPTY
            } else {
                return Rect.ofXYWH(xMin, yMin, xMax - xMin, yMax - yMin)
            }
        }


    internal val points = mutableListOf<Pair<Point, Any?>>()
    internal fun insert(point: Point, owner: Any?) {
        points.add(Pair(point, owner))
        xMin = min(xMin, point.x)
        xMax = max(xMax, point.x)
        yMin = min(yMin, point.y)
        yMax = max(yMax, point.y)
    }

    internal fun squaredDistanceTo(query: Point): Double {
        val width = xMax - xMin
        val height = yMax - yMin
        val x = (xMin + xMax) / 2.0
        val y = (yMin + yMax) / 2.0
        val dx = max(abs(query.x - x) - width / 2, 0.0)
        val dy = max(abs(query.y - y) - height / 2, 0.0)
        return dx * dx + dy * dy
    }

    fun points() = sequence {
        for (point in points) {
            yield(point)
        }
    }
}

class HashGrid(val radius: Float) {
    private val cells = mutableMapOf<GridCoords, Cell>()
    internal fun cells() = sequence {
        for (cell in cells.values) {
            yield(cell)
        }
    }

    var size: Int = 0
        private set

    val cellSize = radius / sqrt(2.0).toFloat()
    private fun coords(v: Point): GridCoords {
        val x = (v.x / cellSize).fastFloor()
        val y = (v.y / cellSize).fastFloor()
        return GridCoords(x, y)
    }

    fun points() = sequence {
        for (cell in cells.values) {
            for (point in cell.points) {
                yield(point)
            }
        }
    }

    fun random(random: Random = Random.Default): Point {
        return cells.values.random(random).points.random().first
    }

    fun insert(point: Point, owner: Any? = null) {
        val gc = coords(point)
        val cell = cells.getOrPut(gc) { Cell(gc.x, gc.y, cellSize) }
        cell.insert(point, owner)
        size += 1
    }

    internal fun cell(query: Point): Cell? = cells[coords(query)]

    fun isFree(query: Point, ignoreOwners: Set<Any> = emptySet()): Boolean {
        val c = coords(query)
        if (cells[c] == null) {
            for (j in -2..2) {
                for (i in -2..2) {
                    if (i == 0 && j == 0) {
                        continue
                    }
                    val n = c.offset(i, j)
                    val nc = cells[n]
                    if (nc != null && nc.squaredDistanceTo(query) <= radius * radius) {
                        for (p in nc.points) {

                            if (p.second == null || p.second !in ignoreOwners) {
                                if (p.first.squaredDistanceTo(query) <= radius * radius) {
                                    return false
                                }
                            }
                        }
                    }
                }
            }
            return true
        } else {
            return cells[c]!!.points.all { it.second != null && it.second in ignoreOwners }
        }
    }
}

/**
 * Construct a hash grid containing all points in the list
 * @param radius radius of the hash grid
 */
fun List<Point>.hashGrid(radius: Float): HashGrid {
    val grid = HashGrid(radius)
    for (point in this) {
        grid.insert(point)
    }
    return grid
}

/**
 * Return a list that only contains points at a minimum distance.
 * @param radius the minimum distance between any two points in the returned list
 */
fun List<Point>.filter(radius: Float): List<Point> {
    return if (size <= 1) {
        this
    } else {
        val grid = HashGrid(radius)
        for (point in this) {
            if (grid.isFree(point)) {
                grid.insert(point)
            }
        }
        grid.points().map { it.first }.toList()
    }
}
