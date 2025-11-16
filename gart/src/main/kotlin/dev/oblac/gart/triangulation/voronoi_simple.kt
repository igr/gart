package dev.oblac.gart.triangulation

import dev.oblac.gart.noise.cellnoise
import kotlin.math.abs
import kotlin.math.floor

fun voronoi(x: Float): Float {
    val i = floor(x).toInt()
    val f = x - i
    val x0 = cellnoise((i - 1).toFloat())
    val d0 = abs(f - (-1 + x0))
    val x1 = cellnoise(i.toFloat())
    val d1 = abs(f - x1)
    val x2 = cellnoise((i + 1).toFloat())
    val d2 = abs(f - (1 + x2))
    return minOf(d0, d1, d2)
}
