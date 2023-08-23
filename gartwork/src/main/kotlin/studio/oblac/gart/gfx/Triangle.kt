package studio.oblac.gart.gfx

import studio.oblac.gart.skia.Canvas
import studio.oblac.gart.skia.Paint
import studio.oblac.gart.skia.Path
import studio.oblac.gart.skia.Point

data class Triangle(val p1: Point, val p2: Point, val p3: Point) {
    fun points() = arrayOf(p1, p2, p3)

    val path = Path()
        .moveTo(p1)
        .lineTo(p2)
        .lineTo(p3)
        .closePath()
}

fun Canvas.drawTriangle(triangle: Triangle, paint: Paint) = this.drawPath(triangle.path, paint)

