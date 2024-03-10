package dev.oblac.gart.rects

import dev.oblac.gart.Gart
import dev.oblac.gart.Media
import dev.oblac.gart.gfx.*
import dev.oblac.gart.skia.BlendMode
import dev.oblac.gart.skia.Paint
import dev.oblac.gart.skia.Path
import dev.oblac.gart.skia.Point
import java.util.stream.IntStream.range

val gart = Gart.of(
    "rects",
    960, 640, 1
)

fun main() {
    with(gart) {
        println(name)
        w.show()
        m.draw {
            g.canvas.clear(Colors.blackColor.toColor())

            val totalX = 26
            val deltaX = g.rect.width / (totalX - 2)
            val totalY = 12
            val deltaY = g.rect.height / (totalY - 2)

            range(0, totalX).forEach {x ->
                range(0, totalY).forEach { y ->
//                    val p = fillOf(Palettes.cool1.random().alpha(128)).apply { blendMode = BlendMode.SCREEN }
                    val p = fillOf(Palettes.cool1.getSafe((x / 2.5).toInt()).alpha(128))
                        .apply { blendMode = BlendMode.SCREEN }
                    drawRandomRect(Point(x * deltaX + rnd(20f), y * deltaY + rnd(20f)), 50f, p)
                }
            }
        }

        Media.saveImage(this)
    }
}

private fun drawRandomRect(point: Point, r: Float, p: Paint) {
    val canvas = gart.g.canvas

    val rect = pathOf(
        Point(point.x - r + rnd(10f), point.y - r + rnd(10f)),
        Point(point.x + r + rnd(10f), point.y - r + rnd(10f)),
        Point(point.x + r + rnd(10f), point.y + r + rnd(10f)),
        Point(point.x - r + rnd(10f), point.y + r + rnd(10f)),
    )
    canvas.drawPath(rect, p)
    canvas.drawPath(rect, strokeOfBlack(1f))
}

private fun pathOf(left: Point, bottom: Point, right: Point, top:Point) : Path {
    return Path()
        .moveTo(left)
        .lineTo(bottom)
        .lineTo(right)
        .lineTo(top)
        .closePath()
}

private fun rnd(delta: Float)  = (Math.random() * 2 * delta).toFloat() - delta
