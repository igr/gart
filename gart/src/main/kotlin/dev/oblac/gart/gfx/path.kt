package dev.oblac.gart.gfx

import org.jetbrains.skia.Path
import org.jetbrains.skia.PathMeasure
import org.jetbrains.skia.Point
import org.jetbrains.skia.Region

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

fun List<Point>.toQuadPath(): Path {
    val path = Path()
    path.moveTo(this[0])
    for (i in 1 until this.size - 1 step 2) {
        path.quadTo(this[i], this[i + 1])
    }
    path.lineTo(this.last())
    return path
}
fun List<Point>.toPath() = pathOf(this)
fun List<Point>.toClosedPath() = pathOf(this).closePath()

fun Point.pathTo(point: Point): Path = Path().moveTo(this).lineTo(point)


fun closedPathOf(first: Point, vararg points: Point): Path {
    val path = Path().moveTo(first)
    points.forEach { path.lineTo(it) }
    return path.closePath()
}

fun Path.toPoints(pointsCount: Int) = pointsOn(this, pointsCount)
fun Path.toPoints(pointsCount: Int, ease: EaseFn) = pointsOn(this, pointsCount, ease)

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

fun Path.toRegion(): Region {
    val region = Region()
    val clipRegion = Region()
    clipRegion.setRect(this.bounds.toIRect())
    region.setPath(this, clipRegion)
    return region
}
