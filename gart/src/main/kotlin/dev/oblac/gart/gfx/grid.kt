package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import org.jetbrains.skia.Rect

data class GridRect(val rect: Rect, val row: Int, val col: Int)

fun gridOfDimension(d: Dimension, cellsX: Int, cellsY: Int): List<GridRect> {
    val cellWidth = d.width / cellsX
    val cellHeight = d.height / cellsY
    return (0 until cellsY).flatMap { y ->
        (0 until cellsX).map { x ->
            GridRect(
                Rect.makeXYWH(x * cellWidth, y * cellHeight, cellWidth, cellHeight),
                row = y,
                col = x
            )
        }
    }
}
