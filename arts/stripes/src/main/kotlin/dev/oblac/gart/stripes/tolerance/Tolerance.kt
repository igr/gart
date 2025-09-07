package dev.oblac.gart.stripes.tolerance

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.angle.cosf
import dev.oblac.gart.angle.sinf
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.toPath
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.floor

fun main() {
    val gart = Gart.of("tolerance", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    val d = g.d
    val c = g.canvas
    val w = gart.window()

    draw(c, d)
    gart.saveImage(g)
    w.showImage(g)
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(BgColors.coolDark)

    val h = 16

    val ps = mutableListOf<Point>()
    for (y in 0 until d.h + h step h) {
        val p = Point(
            d.cx
                + 100 * sinf(Degrees.of(y / 3))
                - 150 * cosf(Degrees.of(y / 5)), y.toFloat()
        )
        ps.add(p)
    }

    // draw white to the left
    val path = ps.toPath()
    path.lineTo(0f, d.h.toFloat())
    path.lineTo(0f, 0f)
    path.closePath()
    c.drawPath(path, fillOf(BgColors.clottedCream))

    // draw rectangles
    val w = 200

    ps.forEachIndexed { index, p ->
        val xx = p.x + floor(rndf(-8f, 8f)) * 10f
        val rectLeft = Rect.makeXYWH(xx - w, p.y, w.toFloat(), h.toFloat())
        val rectRight = Rect.makeXYWH(xx, p.y, w.toFloat(), h.toFloat())
        c.drawRect(rectLeft, fillOf(pal.safe(index * 2)))
        c.drawRect(rectRight, fillOf(pal.safe(index + 1)))
    }

//    // draw text
//    val text = "толеранце"
//    val textLine = TextLine.make(text, font(FontFamily.NotoSans, 36f).apply {
//        scaleX = 0.9f
//    })
//    c.save()
//    c.rotate(90f)
//    c.translate(0f, -20f)
//    c.drawTextLine(textLine, 60f, 0f, fillOfBlack())
//    c.restore()
}

//private val pal = Palettes.mix3.shuffle()
private val pal = Palettes.mix9.shuffle()
