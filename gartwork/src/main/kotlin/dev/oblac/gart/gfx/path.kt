package dev.oblac.gart.gfx

import dev.oblac.gart.skia.Path
import dev.oblac.gart.skia.Point
import org.jetbrains.skia.PathMeasure

fun pathOf(first: Point, vararg points: Point): Path {
    val path = Path().moveTo(first)
    points.forEach { path.lineTo(it) }
    return path
}

fun closedPathOf(first: Point, vararg points: Point): Path {
    val path = Path().moveTo(first)
    points.forEach { path.lineTo(it) }
    return path
}

/**
 * Returns a list of [count] points on the path.
 */
fun pointsOn(path: Path, count: Int): List<Point> {
    val iter = PathMeasure(path)
    val length = iter.length
    val step = length / count
    val points = mutableListOf<Point>()
    for (i in 0..count) {
        val distance = i * step
        val position = iter.getPosition(distance)!!
        points.add(position)
    }
    return points
}
