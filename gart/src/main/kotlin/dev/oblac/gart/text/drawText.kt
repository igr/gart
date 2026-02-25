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

enum class HorizontalAlign { LEFT, CENTER, RIGHT }
enum class VerticalAlign { TOP, CENTER, BOTTOM }

fun Canvas.drawStringInRect(
    text: String,
    rect: Rect,
    font: Font,
    paint: Paint,
    horizontalAlign: HorizontalAlign = HorizontalAlign.CENTER,
    verticalAlign: VerticalAlign = VerticalAlign.CENTER
) {
    val metrics = font.metrics
    val textBounds = font.measureText(text)
    val y = when (verticalAlign) {
        VerticalAlign.TOP -> rect.top - metrics.ascent
        VerticalAlign.CENTER -> rect.top + (rect.height - metrics.descent - metrics.ascent) / 2
        VerticalAlign.BOTTOM -> rect.bottom - metrics.descent
    }
    val x = when (horizontalAlign) {
        HorizontalAlign.LEFT -> rect.left
        HorizontalAlign.CENTER -> rect.left + (rect.width - textBounds.width) / 2
        HorizontalAlign.RIGHT -> rect.right - textBounds.width
    }
    this.drawString(text, x, y, font, paint)
}

fun Canvas.drawMultilineStringInRect(
    text: String,
    rect: Rect,
    font: Font,
    paint: Paint,
    horizontalAlign: HorizontalAlign = HorizontalAlign.CENTER,
    verticalAlign: VerticalAlign = VerticalAlign.CENTER
) {
    val metrics = font.metrics
    val lineHeight = metrics.descent - metrics.ascent + metrics.leading
    val lines = wrapText(text, font, rect.width)
    val totalHeight = lines.size * lineHeight
    var y = when (verticalAlign) {
        VerticalAlign.TOP -> rect.top - metrics.ascent
        VerticalAlign.CENTER -> rect.top + (rect.height - totalHeight) / 2 - metrics.ascent
        VerticalAlign.BOTTOM -> rect.bottom - totalHeight - metrics.ascent
    }

    for (line in lines) {
        val textBounds = font.measureText(line)
        val x = when (horizontalAlign) {
            HorizontalAlign.LEFT -> rect.left
            HorizontalAlign.CENTER -> rect.left + (rect.width - textBounds.width) / 2
            HorizontalAlign.RIGHT -> rect.right - textBounds.width
        }
        this.drawString(line, x, y, font, paint)
        y += lineHeight
    }
}

private fun wrapText(text: String, font: Font, maxWidth: Float): List<String> {
    val lines = mutableListOf<String>()
    for (paragraph in text.split('\n')) {
        if (paragraph.isEmpty()) {
            lines.add("")
            continue
        }
        val words = paragraph.split(' ')
        var currentLine = StringBuilder()
        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (font.measureText(candidate).width <= maxWidth) {
                currentLine = StringBuilder(candidate)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
    }
    return lines
}

