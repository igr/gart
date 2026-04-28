package dev.oblac.gart.gfx

import dev.oblac.gart.math.rndGaussian
import org.jetbrains.skia.*

fun pathOf(first: Point, vararg points: Point): Path {
    val path = PathBuilder().moveTo(first)
    points.forEach { path.lineTo(it) }
    return path.detach()
}

fun pathOf(list: List<Point>) = pathBuilderOf(list).detach()

fun pathBuilderOf(points: List<Point>): PathBuilder {
    val path = PathBuilder()
    path.moveTo(points.first())
    for (i in 1 until points.size) path.lineTo(points[i])
    return path
}

fun closedPathOf(points: List<Point>): Path {
    return pathBuilderOf(points).closePath().detach()
}

fun List<Point>.toQuadPath(): Path {
    val path = PathBuilder()
    path.moveTo(this[0])
    for (i in 1 until this.size - 1 step 2) {
        path.quadTo(this[i], this[i + 1])
    }
    path.lineTo(this.last())
    return path.detach()
}
fun List<Point>.toPath() = pathOf(this)
fun List<Point>.toPathBuilder() = pathBuilderOf(this)
fun List<Point>.toClosedPath() = closedPathOf(this)

fun Point.pathTo(point: Point): Path =
    PathBuilder().moveTo(this).lineTo(point).detach()

fun closedPathOf(first: Point, vararg points: Point): Path {
    val path = PathBuilder().moveTo(first)
    points.forEach { path.lineTo(it) }
    return path.closePath().detach()
}

fun Path.points(): List<Point> {
    return this.points.filterNotNull().toList()
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
        iter.getPosition(distance)?.also {
            points.add(it)
        }
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
    val resultPath = PathBuilder()
    for (path in paths) {
        resultPath.addPath(path)
    }
    return resultPath.detach()
}

fun combinePathsWithOp(operation: PathOp, vararg paths: Path): Path {
    if (paths.isEmpty()) {
        // Or return Path() if that's preferred for empty input
        throw IllegalArgumentException("At least one path must be provided for boolean operations.")
    }
    if (paths.size == 1) {
        return paths[0]
    }

    var currentResultPath = PathBuilder().addPath(paths[0]).detach() // Start with a clone of the first path

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
fun List<Line>.toClosedPath() = closedPathOf(this.flatMap { it.points(2) })

/**
 * Checks if two paths overlap by computing their intersection.
 * Returns true if the paths intersect and the intersection has non-zero area.
 */
fun pathsOverlap(c1: Path, c2: Path, minArea: Float = 1f): Boolean {
    val intersect = Path.makeCombining(c1, c2, PathOp.INTERSECT) ?: return false

    val bounds = intersect.bounds
    return bounds.width >= minArea && bounds.height >= minArea
}


/**
 * Calculates the total length of the path.
 */
fun Path.length(): Float {
    return PathMeasure(this).length
}

/**
 * Bakes a discrete path effect into [path] geometry — the static-geometry
 * counterpart to Skia's `PathEffect.makeDiscrete`. Walks each contour at
 * intervals of [segLength], offsets every sample perpendicular to the local
 * tangent by a uniform random in `[-deviation, deviation]`, and connects the
 * perturbed samples with line segments.
 *
 * Unlike the paint-time effect, the result is a real [Path] you can hand to
 * boolean ops, obstacle setters, or anywhere a Path is expected.
 *
 * [seed] makes the perturbation deterministic; pass different seeds for
 * different jitter patterns.
 */
fun discretizePath(
    path: Path,
    segLength: Float,
    deviation: Float,
    seed: Int = 0,
): Path {
    val rng = kotlin.random.Random(seed.toLong())
    val builder = PathBuilder()
    val measure = PathMeasure(path, false)
    do {
        val length = measure.length
        if (length <= 0f) continue
        var distance = 0f
        var first = true
        while (distance <= length) {
            val pos = measure.getPosition(distance) ?: break
            val tan = measure.getTangent(distance) ?: Point(1f, 0f)
            val nx = -tan.y
            val ny = tan.x
            val r = (rng.nextFloat() * 2f - 1f) * deviation
            val px = pos.x + nx * r
            val py = pos.y + ny * r
            if (first) {
                builder.moveTo(px, py)
                first = false
            } else {
                builder.lineTo(px, py)
            }
            distance += segLength
        }
    } while (measure.nextContour())
    return builder.detach()
}

/**
 * Checks if a point is below a path at the point's x coordinate.
 * "Below" means the point's y is greater than the path's y (screen coordinates).
 * The path is sampled at the given [precision] interval.
 * If the path has multiple segments at the same x, the point must be below all of them.
 * Returns false if the point's x is outside the path's x range.
 */
fun isPointBelowPath(path: Path, point: Point, precision: Float = 1f): Boolean {
    val measure = PathMeasure(path, false)
    val pathYValues = mutableListOf<Float>()

    do {
        val length = measure.length
        var distance = 0f
        var prev: Point? = null

        while (distance <= length) {
            val curr = measure.getPosition(distance) ?: break
            if (prev != null) {
                val x1 = prev.x
                val x2 = curr.x
                if ((x1 <= point.x && point.x <= x2) || (x2 <= point.x && point.x <= x1)) {
                    if (x1 != x2) {
                        val t = (point.x - x1) / (x2 - x1)
                        val y = prev.y + t * (curr.y - prev.y)
                        pathYValues.add(y)
                    }
                }
            }
            prev = curr
            distance += precision
        }
    } while (measure.nextContour())

    if (pathYValues.isEmpty()) return false
    return pathYValues.all { point.y > it }
}

fun Point.isBelowPath(path: Path, precision: Float = 1f): Boolean =
    isPointBelowPath(path, this, precision)
