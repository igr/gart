package dev.oblac.gart.gfx

import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.PathMeasure
import org.jetbrains.skia.Point
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class PathOutline(val line: Path, val width: Float, val outline: Path)

/**
 * Closed outline of a sampled polyline that has its own half-width at every sample: walks up
 * one side and back down the other, so the stroke can swell in the middle and run to a point
 * at both ends - which stroking a path cannot do. The variable-width twin of [pathToOutline].
 *
 * Only the first [n] samples are read, so the arrays can be reused buffers. Normals come from
 * the central difference of the neighbouring samples (one-sided at the ends); a degenerate step
 * falls back to a horizontal tangent. Fewer than two samples give an empty path.
 */
fun outlineOf(xs: FloatArray, ys: FloatArray, halfWidths: FloatArray, n: Int = xs.size): Path {
    if (n < 2) return Path()
    val nx = FloatArray(n)
    val ny = FloatArray(n)
    for (i in 0 until n) {
        val a = max(0, i - 1)
        val b = min(n - 1, i + 1)
        var tx = xs[b] - xs[a]
        var ty = ys[b] - ys[a]
        val l = hypot(tx, ty)
        if (l < 1e-5f) {
            tx = 1f; ty = 0f
        } else {
            tx /= l; ty /= l
        }
        nx[i] = -ty
        ny[i] = tx
    }

    val pb = PathBuilder()
    for (i in 0 until n) {
        val h = halfWidths[i]
        pb.lineOrMove(i == 0, xs[i] + nx[i] * h, ys[i] + ny[i] * h)
    }
    for (i in n - 1 downTo 0) {
        val h = halfWidths[i]
        pb.lineTo(xs[i] - nx[i] * h, ys[i] - ny[i] * h)
    }
    pb.closePath()
    return pb.detach()
}

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
