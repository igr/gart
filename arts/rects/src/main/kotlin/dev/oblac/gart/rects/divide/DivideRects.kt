package dev.oblac.gart.rects.divide

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndi
import dev.oblac.gart.shader.createNoiseGrainFilter
import org.jetbrains.skia.*
import kotlin.math.max
import kotlin.math.min

val gart = Gart.of(
    "divine-divide",
    1024, 1024
)

fun main() {

    val g = gart.gartvas()
    val c = g.canvas
    c.clear(Colors.blackColor.toColor())

    val gap = 40f       // we don't want any random

    var rects = arrayOf(Rect(0f, 0f, gart.d.rect.width, gart.d.rect.height))
    repeat(5) {
        rects = divideRects(rects, gap)
    }

    rects.forEach { rect ->
        fillRect(c, rect, Colors.lightSalmonColor, Colors.lightSalmonColor)
        c.drawRect(rect, strokeOfWhite(10f).also { it.strokeCap = PaintStrokeCap.ROUND })
    }

    c.drawBorder(gart.d, strokeOfWhite(10f))
    gart.saveImage(g)
    gart.window().showImage(g)
}

fun fillRect(c: Canvas, rect: Rect, color: Color4f, colorAlt: Color4f) {
    c.drawRect(rect, fillOf(Color.WHITE))

    if (max(rect.width, rect.height) / min(rect.width, rect.height) > 6f) {
        c.drawRect(rect.shrink(10f), fillOf(colorAlt).also { it.imageFilter = createNoiseGrainFilter(2f, rect.dimension()) })
        return
    }

    var gap = 1f
    var verticalP = rect.topLeftPoint()
    var horizontalP = rect.topLeftPoint()

    var i = 0f
    while (true) {
        i += 0.1f
        gap += 0.07f * i
        verticalP = if (verticalP.y < rect.bottom) {
            verticalP.offset(0f, gap)
        } else {
            verticalP.offset(gap, 0f)
        }
        horizontalP = if (horizontalP.x < rect.right) {
            horizontalP.offset(gap, 0f)
        } else {
            horizontalP.offset(0f, gap)
        }

        if (verticalP.x >= rect.right && horizontalP.y >= rect.bottom) {
            break
        }
        c.drawLine(verticalP, horizontalP, strokeOf(color, 1.5f))
    }
}

fun divideRects(startingRect: Array<Rect>, gap: Float): Array<Rect> {
    val newRects = mutableListOf<Rect>()

    startingRect.forEach { rect ->
        if (rect.width < gap * 4 || rect.height < gap * 4) {
            newRects.add(rect)
            return@forEach
        }

        val divideWidth = rndi(1, (rect.width / gap).toInt()) * gap
        val divideHeight = rndi(1, (rect.height / gap).toInt()) * gap

        val rect1 = Rect(rect.left, rect.top, rect.left + divideWidth, rect.top + divideHeight)     // top left
        val rect2 = Rect(rect.left + divideWidth, rect.top, rect.right, rect.top + divideHeight)    // top right
        val rect3 = Rect(rect.left, rect.top + divideHeight, rect.left + divideWidth, rect.bottom)  // bottom left
        val rect4 = Rect(rect.left + divideWidth, rect.top + divideHeight, rect.right, rect.bottom) // bottom right

        newRects.addAll(listOf(rect1, rect2, rect3, rect4))
    }

    return newRects.toTypedArray()
}
