package dev.oblac.gart.metro

import dev.oblac.gart.gfx.closedPathOf
import dev.oblac.gart.gfx.pathOf
import dev.oblac.gart.gfx.pointsOn
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
    val rect: Path = closedPathOf(
        Point(left.x + ldx, left.y),
        Point(left.x, left.y - ldy),
        Point(right.x, right.y - rdy),
        Point(right.x + rdx, right.y)
    )

    fun toLines(count: Int): List<Pair<Point, Point>> {
        val i1 = pointsOn(pathOf(rect.getPoint(0), rect.getPoint(1)), count)
        val i2 = pointsOn(pathOf(rect.getPoint(3), rect.getPoint(2)), count) //val i1 = interpolate(rect.getPoint(0), rect.getPoint(1), count)

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
