package dev.oblac.gart.pack

import dev.oblac.gart.gfx.Circle
import dev.oblac.gart.math.rndf
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

class CirclePacker(
    private val width: Float,
    private val height: Float,
    private val growth: Int = 1,
    private val numGrid: Int = 15,
    private val padding: Int = 1
) {
    private val gridSizeX = width / numGrid
    private val gridSizeY = height / numGrid
    private val grid: Array<Array<MutableList<Circle>>> = Array(numGrid) { Array(numGrid) { mutableListOf() } }
    val items = mutableListOf<Circle>()

    private fun getGridTilesAround(x: Float, y: Float, r: Float): List<MutableList<Circle>> {
        val tlX = floor((x - r) / gridSizeX).toInt().coerceAtLeast(0)
        val tlY = floor((y - r) / gridSizeY).toInt().coerceAtLeast(0)
        val brX = floor((x + r) / gridSizeX).toInt().coerceAtMost(numGrid - 1)
        val brY = floor((y + r) / gridSizeY).toInt().coerceAtMost(numGrid - 1)

        val tiles = mutableListOf<MutableList<Circle>>()
        for (i in tlX..brX) {
            for (j in tlY..brY) {
                tiles.add(grid[i][j])
            }
        }
        return tiles
    }

    private fun distCirc(c1: Circle, c2: Circle): Float {
        return sqrt((c1.x - c2.x).pow(2) + (c1.y - c2.y).pow(2)) - (c1.radius + c2.radius)
    }

    fun tryToAddCircle(x: Float, y: Float, minRadius: Float = 0.0f, maxRadius: Float = 900.0f): Circle? {
        var c1 = Circle(x, y, minRadius)

        while (true) {
            val gridTiles = getGridTilesAround(x, y, c1.radius)

            for (tile in gridTiles) {
                for (c2 in tile) {
                    if (distCirc(c1, c2) - padding < 0) {
                        return if (c1.radius == minRadius) {
                            null
                        } else {
                            addCircleToItems(gridTiles, c1)
                        }
                    }
                }
            }

            c1 = Circle(x, y, c1.radius + growth)

            if (c1.radius > maxRadius) {
                return addCircleToItems(gridTiles, c1)
            }
        }
    }

    private fun addCircleToItems(gridTiles: List<MutableList<Circle>>, c: Circle): Circle {
        gridTiles.forEach { it.add(c) }
        items.add(c)
        return c
    }

    fun removeCircles(x: Float, y: Float, radius: Float) {
        val gridTiles = getGridTilesAround(x, y, radius)
        val c1 = Circle(x, y, radius)

        for (tile in gridTiles) {
            tile.retainAll { distCirc(c1, it) - padding >= 0 }
        }

        items.retainAll { distCirc(c1, it) - padding >= 0 }
    }

    fun pack(tries: Int, minRadius: Float = 0.0f, maxRadius: Float = 900.0f): List<Circle> {
        repeat(tries) {
            val x = rndf(0f, width)
            val y = rndf(0f, height)
            tryToAddCircle(x, y, minRadius, maxRadius)
        }
        return items
    }
}
