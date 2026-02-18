package dev.oblac.gart.rects

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.alpha
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOfBlack
import org.jetbrains.skia.*
import java.util.stream.IntStream.range

val gart = Gart.of(
    "rects",
    960, 640, 1
)

fun main() {
    println(gart)
    val g = gart.gartvas()
    g.canvas.clear(Colors.blackColor.toColor())

    val totalX = 26
    val deltaX = gart.d.rect.width / (totalX - 2)
    val totalY = 12
    val deltaY = gart.d.rect.height / (totalY - 2)

    range(0, totalX).forEach { x ->
        range(0, totalY).forEach { y ->
//                    val p = fillOf(Palettes.cool1.random().alpha(128)).apply { blendMode = BlendMode.SCREEN }
            val p = fillOf(Palettes.cool1.safe((x / 2.5).toInt()).alpha(128))
                .apply { blendMode = BlendMode.SCREEN }
            drawRandomRect(g.canvas, Point(x * deltaX + rnd(20f), y * deltaY + rnd(20f)), 50f, p)
        }
    }
    gart.window().showImage(g)
}

private fun drawRandomRect(canvas: Canvas, point: Point, r: Float, p: Paint) {
    val rect = pathOf(
        Point(point.x - r + rnd(10f), point.y - r + rnd(10f)),
        Point(point.x + r + rnd(10f), point.y - r + rnd(10f)),
        Point(point.x + r + rnd(10f), point.y + r + rnd(10f)),
        Point(point.x - r + rnd(10f), point.y + r + rnd(10f)),
    )
    canvas.drawPath(rect, p)
    canvas.drawPath(rect, strokeOfBlack(1f))
}

private fun pathOf(left: Point, bottom: Point, right: Point, top: Point): Path {
    return PathBuilder()
        .moveTo(left)
        .lineTo(bottom)
        .lineTo(right)
        .lineTo(top)
        .closePath()
        .detach()
}

private fun rnd(delta: Float) = (Math.random() * 2 * delta).toFloat() - delta
