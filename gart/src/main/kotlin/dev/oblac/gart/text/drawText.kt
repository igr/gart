package dev.oblac.gart.text

import dev.oblac.gart.math.PIf
import org.jetbrains.skia.*
import kotlin.math.atan2

fun drawTextOnPath(canvas: Canvas, path: Path, text: String, font: Font, paint: Paint) {
    val pathMeasure = PathMeasure(path, false)
    val length = pathMeasure.length
    val charSpacing = length / text.length
    var distance = 0f

    text.forEach { char ->
        val pos = pathMeasure.getPosition(distance)!!
        val tan = pathMeasure.getTangent(distance)!!

        val angle = atan2(tan.y, tan.x) * (180f / PIf)
        val matrix = Matrix33.makeRotate(angle, pos.x, pos.y)

        canvas.save()
        canvas.concat(matrix)
        canvas.drawString(char.toString(), pos.x, pos.y, font, paint)
        canvas.restore()

        distance += charSpacing
    }
}
