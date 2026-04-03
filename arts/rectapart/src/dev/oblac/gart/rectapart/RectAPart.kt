package dev.oblac.gart.rectapart

import dev.oblac.gart.Gart
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.gfx.createCircleOfPoints
import dev.oblac.gart.gfx.randomPointBetween
import dev.oblac.gart.gfx.strokeOfWhite
import dev.oblac.gart.gfx.toClosedPath
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathEffect
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("rectApart", 1024, 1024)
    println(gart)

    val g = gart.gartvas()

    // ⚠️ There was a bug in Skiko that produced that image.
    // I am not able to reproduce anymore.

    // draw on canvas
    g.draw { c, _ ->
        c.clear(CssColors.black)

//        c.clipRect(Rect.makeWH(500f, 500f))
//        c.save()
        repeat(40) {
            draw(c, 3)
//            draw(c, 10)
//            draw(c, 12)
//            draw(c, 14)
//            draw(c, 15)
        }
//        c.restore()
    }


    // save & show image
    gart.saveImage(g)
    gart.window().showImage(g)
}

private fun draw(c: Canvas, type: Int) {
    val shape = createCircleOfPoints(Point(512f, 512f), 480f, type)

    var s = shape
    repeat(20) {
        c.drawPath(s.toClosedPath(), strokeOfWhite(0.5f).apply {
            this.alpha = 100
            this.pathEffect = PathEffect.makeDiscrete(2f, 2f, 173)
        })
        s = klik(s)
    }
}

private fun klik(shape: List<Point>): List<Point> {
    val newShape = mutableListOf<Point>()
    for (i in shape.indices) {
        val nextIndex = (i + 1) % shape.size
        newShape.add(randomPointBetween(shape[i], shape[nextIndex]))
    }
    return newShape
}

