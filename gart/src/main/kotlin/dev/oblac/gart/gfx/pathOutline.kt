package dev.oblac.gart.gfx

import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.PathMeasure
import org.jetbrains.skia.Point

data class PathOutline(val line: Path, val width: Float, val outline: Path)

fun Path.toOutline(width: Float): PathOutline = pathToOutline(this, width)

/**
 * Converts a line path to a [PathOutline] containing the original line,
 * width, and a closed outline path created by offsetting perpendicular
 * to the tangent at each sampled point.
 */
fun pathToOutline(path: Path, width: Float): PathOutline {
    val halfW = width / 2f
    val measure = PathMeasure(path)
    val length = measure.length
    val steps = maxOf(2, (length / 2f).toInt())
    val stepSize = length / steps

    val left = mutableListOf<Point>()
    val right = mutableListOf<Point>()

    for (i in 0..steps) {
        val dist = i * stepSize
        val pos = measure.getPosition(dist) ?: continue
        val tan = measure.getTangent(dist) ?: continue
        val len = kotlin.math.sqrt(tan.x * tan.x + tan.y * tan.y)
        if (len == 0f) continue
        val nx = -tan.y / len
        val ny = tan.x / len
        left.add(Point(pos.x + nx * halfW, pos.y + ny * halfW))
        right.add(Point(pos.x - nx * halfW, pos.y - ny * halfW))
    }

    val builder = PathBuilder()
    if (left.isNotEmpty()) {
        builder.moveTo(left[0].x, left[0].y)
        for (i in 1 until left.size) {
            builder.lineTo(left[i].x, left[i].y)
        }
        for (i in right.lastIndex downTo 0) {
            builder.lineTo(right[i].x, right[i].y)
        }
        builder.closePath()
    }
    return PathOutline(path, width, builder.detach())
}
