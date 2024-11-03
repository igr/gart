package dev.oblac.gart.gfx

import org.jetbrains.skia.Path
import org.jetbrains.skia.PathMeasure
import org.jetbrains.skia.Point

fun pathOf(first: Point, vararg points: Point): Path {
    val path = Path().moveTo(first)
    points.forEach { path.lineTo(it) }
    return path
}

fun pathOf(list: List<Point>): Path {
    val path = Path()
    list.forEachIndexed { index, point ->
        if (index == 0) {
            path.moveTo(point)
        } else {
            path.lineTo(point)
        }
    }
    return path
}

fun List<Point>.toPath() = pathOf(this)

fun Point.pathTo(point: Point): Path = Path().moveTo(this).lineTo(point)


fun closedPathOf(first: Point, vararg points: Point): Path {
    val path = Path().moveTo(first)
    points.forEach { path.lineTo(it) }
    return path.closePath()
}

/**
 * Returns a list of [count] points on the path.
 */
fun pointsOn(path: Path, pointsCount: Int): List<Point> {
    val count = pointsCount - 1
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

fun pointsOn(path: Path, pointsCount: Int, ease: EaseFn = EaseFn.Linear): List<Point> {
    val count = pointsCount - 1
    val iter = PathMeasure(path)
    val length = iter.length

    val stepRelative = 1.0f / count
    var x = 0f
    val points = mutableListOf<Point>()
    for (i in 0..count) {
        val distance = ease(x) * length
        x += stepRelative

        val position = iter.getPosition(distance)!!
        points.add(position)
    }
    return points
}

