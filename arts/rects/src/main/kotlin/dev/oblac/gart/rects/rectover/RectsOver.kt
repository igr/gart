package dev.oblac.gart.rects.rectover

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndi
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathOp
import org.jetbrains.skia.Point
import java.util.stream.IntStream.range

val gart = Gart.of(
    "rects-over",
    1024, 1024, 1
)

fun main() {
    println(gart)
    val g = gart.gartvas()

    val totalX = 14
    val deltaX = gart.d.rect.width / (totalX - 2)
    val totalY = 14
    val deltaY = gart.d.rect.height / (totalY - 2)

    val allRectPaths = mutableListOf<Path>()

    val palette = Palettes.cool22

    range(0, totalX).forEach { x ->
        range(0, totalY).forEach { y ->
            val rect  = createRandomRect(Point(x * deltaX + rnd(20f), y * deltaY + rnd(20f)), rndf(60, 150f))
            allRectPaths.add(rect)
        }
    }

    allRectPaths.shuffle()

    fun color(p: Point): Int {
        val s = palette.size
        val i = (p.x.toInt()) * s / gart.d.w
        return palette.safe(i + rndi(-2, 3))
    }

    // draw
    val c = g.canvas
    c.clear(Colors.antiqueWhite)
    allRectPaths.forEach { rect ->
        // first draw the rect
        val paint = fillOf(color(rect.bounds.topLeftPoint()))
        c.drawPath(rect, paint)
        c.drawPath(rect, strokeOfBlack(3f))

        allRectPaths.forEach { otherRect ->
            if (rect != otherRect) {
                val intersect = Path.makeCombining(rect, otherRect, PathOp.INTERSECT)
                if (intersect != null) {
                    if (intersect.bounds.height > 10 && intersect.bounds.width > 10) {      // only significant intersections
                        val paintI = fillOf(color(intersect.bounds.topLeftPoint()))
                        c.drawPath(intersect, paintI)
                        c.drawPath(intersect, strokeOfBlack(3f).apply { strokeJoin = PaintStrokeJoin.ROUND })
                    }
                }
            }
        }
    }
    c.drawBorder(gart.d, 20f, Colors.warmBlack2)
    gart.window().showImage(g)
    gart.saveImage(g)
}

private fun createRandomRect(point: Point, r: Float): Path {
    val delta = 20f
    return closedPathOf(
        Point(point.x - r + rnd(delta), point.y - r + rnd(delta)),
        Point(point.x + r + rnd(delta), point.y - r + rnd(delta)),
        Point(point.x + r + rnd(delta), point.y + r + rnd(delta)),
        Point(point.x - r + rnd(delta), point.y + r + rnd(delta)),
    )
}

private fun rnd(delta: Float) = (Math.random() * 2 * delta).toFloat() - delta
