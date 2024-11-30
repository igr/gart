package dev.oblac.gart.stripes

import dev.oblac.gart.Gart
import dev.oblac.gart.borderize
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.toIntColor
import dev.oblac.gart.gfx.fillOfBlack
import dev.oblac.gart.gfx.of
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

val gart = Gart.of(
    "stripes",
    1024, 1500
)
val gg = gart.gg()

fun main() {
    println(gart)

    val g = gg.g
    g.draw { c, _ -> draw(c) }

    val gg2 = borderize(gg, 20, 0xfffadd4a.toIntColor())
    gg2.saveImage()
    gg2.showImage()
}


const val a = 60
val stripesCount = gart.d.h / a

fun draw(canvas: Canvas) {
    canvas.clear(Colors.white)
    for (i in 0 until stripesCount) {
        val y = i * a
        canvas.drawRect(Rect.of(0, y, gart.d.w, y + a / 2), fillOfBlack())
        Line(y.toFloat()).draw(canvas)
    }
}

