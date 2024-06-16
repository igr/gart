package dev.oblac.gart.rects.mondrian

import dev.oblac.gart.Gart
import dev.oblac.gart.Key
import dev.oblac.gart.color.toColor4f
import dev.oblac.gart.gfx.drawBorder
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.rndi
import dev.oblac.gart.rects.divide.divideRects
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Rect

val gart = Gart.of(
    "mondrian",
    1024, 1024
)

fun main() {

    val g = gart.gartvas()
    val c = g.canvas

    drawAll(c)

    gart.window().showImage(g)
        .onKey { key ->
            when (key) {
                Key.KEY_R -> drawAll(c)
                Key.KEY_S -> gart.saveImage(g)
                else -> {}
            }
        }
}

fun drawAll(c: Canvas) {
    c.clear(MondrianColors.white)
    val gap = 70f

    var rects = arrayOf(Rect(0f, 0f, gart.d.rect.width, gart.d.rect.height))
    repeat(3) {
        rects = divideRects(rects, gap)
    }

    val borderStroke = strokeOf(MondrianColors.black, 10f).also { it.strokeCap = PaintStrokeCap.ROUND }
    val colors = randomizeColors(rects)

    rects.forEachIndexed() { index, rect ->
        fillRect(c, rect, colors[index].toColor4f())
        c.drawRect(rect, borderStroke)
    }

    c.drawBorder(gart.d, borderStroke)
}


fun fillRect(c: Canvas, rect: Rect, color: Color4f) {
    c.drawRect(rect, fillOf(color))
}

fun divideRects(startingRect: Array<Rect>, gap: Float): Array<Rect> {
    val newRects = mutableListOf<Rect>()

    startingRect.forEach { rect ->
        if (rect.width < gap * 4 || rect.height < gap * 4) {
            newRects.add(rect)
            return@forEach
        }

        when (rndi(12)) {
            in 0..2 -> {
                // don't split
                newRects.add(rect)
            }

            in 3..4 -> {
                // split vertically
                val divideWidth = rect.width / 2

                val rect1 = Rect(rect.left, rect.top, rect.left + divideWidth, rect.bottom)    // left
                val rect2 = Rect(rect.left + divideWidth, rect.top, rect.right, rect.bottom) // right

                newRects.addAll(listOf(rect1, rect2))
            }

            in 5..6 -> {
                // split horizontally
                val divideHeight = rect.height / 2

                val rect1 = Rect(rect.left, rect.top, rect.right, rect.top + divideHeight)    // top
                val rect2 = Rect(rect.left, rect.top + divideHeight, rect.right, rect.bottom) // bottom

                newRects.addAll(listOf(rect1, rect2))
            }

            else -> {
                // split in 4

                val divideWidth = rndi(1, (rect.width / gap).toInt()) * gap
                val divideHeight = rndi(1, (rect.height / gap).toInt()) * gap

                val rect1 = Rect(rect.left, rect.top, rect.left + divideWidth, rect.top + divideHeight)     // top left
                val rect2 = Rect(rect.left + divideWidth, rect.top, rect.right, rect.top + divideHeight)    // top right
                val rect3 = Rect(rect.left, rect.top + divideHeight, rect.left + divideWidth, rect.bottom)  // bottom left
                val rect4 = Rect(rect.left + divideWidth, rect.top + divideHeight, rect.right, rect.bottom) // bottom right

                newRects.addAll(listOf(rect1, rect2, rect3, rect4))
            }
        }
    }

    return newRects.toTypedArray()
}


fun randomizeColors(rects: Array<Rect>): IntArray {
    val size = rects.size
    val colors = IntArray(size)

    // mostly white
    for (i in 0 until size) {
        colors[i] = MondrianColors.white
    }

    for (i in 0 until 2) {
        colors[rndi(size)] = MondrianColors.blue
    }

    for (i in 0 until 2) {
        colors[rndi(size)] = MondrianColors.yellow
    }

//    for (i in 0 until 1) {
//        colors[rndi(size)] = MondrianColors.red
//    }

    rects.maxBy { it.area }.let { maxRect ->
        colors[rects.indexOf(maxRect)] = MondrianColors.red
    }

    return colors
}

private val Rect.area: Float
    get() {
        return width * height
    }

