package dev.oblac.gart.metro

import dev.oblac.gart.skia.Path
import dev.oblac.gart.skia.Point

data class RLine(
    val left: Point,
    val ldy: Float,
    val ldx: Float,
    val right: Point,
    val rdy: Float,
    val rdx: Float,
) {
    val rect: Path = pathOf(
        Point(left.x + ldx, left.y),
        Point(left.x, left.y - ldy),
        Point(right.x, right.y - rdy),
        Point(right.x + rdx, right.y)
    )

    fun toLines(count: Int): List<Pair<Point, Point>> {
//        val i1 = interpolate(rect.getPoint(0), rect.getPoint(1), count)
//        val i2 = interpolate(rect.getPoint(3), rect.getPoint(2), count)
        val i1 = interpolate(rect.getPoint(0), rect.getPoint(1), count)
        val i2 = interpolate(rect.getPoint(3), rect.getPoint(2), count)

        // make paris of points
        return i1.zip(i2)
    }

    fun nextTo(
        right: Point,
        rdy: Float,
        rdx: Float,
    ): RLine {
        return RLine(
            this.right,
            this.rdy,
            this.rdx,
            right,
            rdy,
            rdx
        )
    }
}

private fun pathOf(left: Point, bottom: Point, right: Point, top: Point): Path {
    return Path()
        .moveTo(left)
        .lineTo(bottom)
        .lineTo(right)
        .lineTo(top)
        .closePath()
}

fun interpolate(
    start: Point,
    end: Point,
    steps: Int,
): List<Point> {
    val dx = (end.x - start.x) / steps
    val dy = (end.y - start.y) / steps
    return (0..steps).map {
        Point(start.x + dx * it, start.y + dy * it)
    }
}
