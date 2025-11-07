package dev.oblac.gart.gfx

import dev.oblac.gart.math.rndGaussian
import org.jetbrains.skia.*

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

fun Path.addCircle(circle: Circle) =
    addCircle(circle.center.x, circle.center.y, circle.radius)

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

fun combinePathsByAppending(vararg paths: Path): Path {
    val resultPath = Path()
    for (path in paths) {
        resultPath.addPath(path)
    }
    return resultPath
}

fun combinePathsWithOp(operation: PathOp, vararg paths: Path): Path {
    if (paths.isEmpty()) {
        // Or return Path() if that's preferred for empty input
        throw IllegalArgumentException("At least one path must be provided for boolean operations.")
    }
    if (paths.size == 1) {
        return paths[0]
    }

    var currentResultPath: Path = Path().addPath(paths[0]) // Start with a clone of the first path

    for (i in 1 until paths.size) {
        val nextPath = paths[i]
        val newResultPath = Path.makeCombining(currentResultPath, nextPath, operation)

        // Close the previous currentResultPath if it's not the initial cloned path
        // and a new path was successfully created.
        // This is important because Path.op creates new native Path objects.
        if (currentResultPath !== paths[0] && currentResultPath !== newResultPath) {
            currentResultPath.close()
        }

        if (newResultPath == null) {
            // Operation failed or resulted in an empty path.
            // Close the currentResultPath if it was a temporary one.
            if (currentResultPath !== paths[0]) { // if it was a temp path, not the initial clone
                currentResultPath.close()
            }
            return Path() // Return an empty path
        }
        currentResultPath = newResultPath
    }
    return currentResultPath // This is the final combined path
}

/**
 * Deforms a path by adding random points along its segments.
 */
fun deformPath(points: List<Point>, offsetStdDev: Float = 15f): List<Point> {
    if (points.isEmpty()) return points

    val deformedPoints = mutableListOf<Point>()

    // for each line segment
    for (i in 0 until points.size) {
        val currentPoint = points[i]
        val nextPoint = points[(i + 1) % points.size] // wrap around for closed path

        deformedPoints.add(currentPoint)

        // Find a random point along the line using Gaussian distribution
        val t = rndGaussian(0.5f, 0.15f).coerceIn(0.1f, 0.9f) // Keep it away from endpoints
        val randomX = currentPoint.x + t * (nextPoint.x - currentPoint.x)
        val randomY = currentPoint.y + t * (nextPoint.y - currentPoint.y)

        // Create offset using Gaussian random values
        val offsetX = rndGaussian(0f, offsetStdDev)
        val offsetY = rndGaussian(0f, offsetStdDev)

        val deformedPoint = org.jetbrains.skia.Point(
            randomX + offsetX,
            randomY + offsetY
        )

        deformedPoints.add(deformedPoint)
    }

    return deformedPoints
}

@JvmName("linesToPath")
fun List<Line>.toPath() = pathOf(this.flatMap { it.points(2) })

@JvmName("linesToClosedPath")
fun List<Line>.toClosedPath() = pathOf(this.flatMap { it.points(2) }).closePath()

/**
 * Checks if two paths overlap by computing their intersection.
 * Returns true if the paths intersect and the intersection has non-zero area.
 */
fun pathsOverlap(c1: Path, c2: Path, minArea: Float = 1f): Boolean {
    val intersect = Path.makeCombining(c1, c2, PathOp.INTERSECT)
    if (intersect == null) return false

    val bounds = intersect.bounds
    return bounds.width >= minArea && bounds.height >= minArea
}
