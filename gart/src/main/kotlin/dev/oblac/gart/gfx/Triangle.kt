package dev.oblac.gart.gfx

import dev.oblac.gart.skia.Canvas
import dev.oblac.gart.skia.Paint
import dev.oblac.gart.skia.Path
import dev.oblac.gart.skia.Point

data class Triangle(val p1: Point, val p2: Point, val p3: Point) {
    fun points() = arrayOf(p1, p2, p3)

    val path = Path()
        .moveTo(p1)
        .lineTo(p2)
        .lineTo(p3)
        .closePath()
}

fun Canvas.drawTriangle(triangle: Triangle, paint: Paint) = this.drawPath(triangle.path, paint)

