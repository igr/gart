package dev.oblac.gart.text

import dev.oblac.gart.math.PIf
import org.jetbrains.skia.*
import kotlin.math.atan2

fun Canvas.drawTextOnPath(path: Path, text: String, font: Font, paint: Paint) {
    val pathMeasure = PathMeasure(path, false)
    val length = pathMeasure.length
    val charSpacing = length / text.length
    var distance = 0f
    text.forEach { char ->
        val pos = pathMeasure.getPosition(distance)!!
        val tan = pathMeasure.getTangent(distance)!!

        val angle = atan2(tan.y, tan.x) * (180f / PIf)
        val matrix = Matrix33.makeRotate(angle, pos.x, pos.y)

        save()
        concat(matrix)
        drawString(char.toString(), pos.x, pos.y, font, paint)
        restore()

        distance += charSpacing
    }
}

fun Canvas.drawStringToRight(
    text: String,
    right: Float,
    y: Float,
    font: Font,
    paint: Paint
) {
    val textWidth = font.measureText(text)
    this.drawString(text, right - textWidth.width, y, font, paint)
}

enum class TextAlign { LEFT, CENTER, RIGHT }

fun Canvas.drawStringInRect(
    text: String,
    rect: Rect,
    font: Font,
    paint: Paint,
    align: TextAlign = TextAlign.CENTER
) {
    val metrics = font.metrics
    val textBounds = font.measureText(text)
    val y = rect.top + (rect.height - metrics.descent - metrics.ascent) / 2
    val x = when (align) {
        TextAlign.LEFT -> rect.left
        TextAlign.CENTER -> rect.left + (rect.width - textBounds.width) / 2
        TextAlign.RIGHT -> rect.right - textBounds.width
    }
    this.drawString(text, x, y, font, paint)
}

