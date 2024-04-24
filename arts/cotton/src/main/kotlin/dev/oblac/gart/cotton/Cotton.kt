package dev.oblac.gart.cotton

import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.contains
import dev.oblac.gart.gfx.move
import dev.oblac.gart.gfx.thirds
import dev.oblac.gart.math.rnd
import dev.oblac.gart.math.rndIn
import dev.oblac.gart.noise.NoiseColor
import dev.oblac.gart.shader.toPaint
import dev.oblac.gart.skia.Paint
import dev.oblac.gart.skia.Rect
import fatLine
import org.jetbrains.skia.Canvas

val gart = Gart.of(
    "cotton",
    1024, 1024
)

fun main() {
    println(gart)
    val g = gart.gartvas()

    val noiseColor = NoiseColor()
//    val palette = Palettes.cool14
//    val palette = Palettes.cool33
    val palette = Palettes.cool28
    val colors = palette.map {
        noiseColor.composeShader(it).toPaint()
            .apply {
                //imageFilter = ImageFilter.makeBlur(1f, 1f, FilterTileMode.CLAMP)
            }
    }

    g.canvas.clear(BgColors.bg01)
    //g.canvas.drawRect(Rect(100f, 100f, 600f, 600f), paint)

    val count = 10
    val delta = gart.d.w / count

    // big rects
    val bigRects = distributedRects(rnd(2, 4)) { bigRect(delta, count) }
    bigRects.forEachIndexed { index, it ->
        g.canvas.drawRect(it.move(rnd(-40f, 40f)), colors[index])
    }

    // small rects
    val smallRects = distributedRects(rnd(2, 4)) { smallRect(delta, count) }
    smallRects.forEachIndexed { index, it ->
        g.canvas.drawRect(it.move(rnd(-40f, 40f)), colors[colors.size - 1 -index])
    }

    // draw lines in thirds
    val thirdsRect = gart.d.rect.thirds()
    if (rndIn(1, 4)) {
        line(g.canvas, 0f, thirdsRect.top, thirdsRect.right * rnd(1,2), thirdsRect.top, colors.random())
    }
    if (rndIn(1, 4)) {
        line(g.canvas, 0f, thirdsRect.top * 2, thirdsRect.right * rnd(1,2), thirdsRect.top * 2, colors.random())
    }
    if (rndIn(1, 4)) {
        line(g.canvas, thirdsRect.left, thirdsRect.top, thirdsRect.left, thirdsRect.top + thirdsRect.top * rnd(1,3), colors.random())
    }
    if (rndIn(1, 4)) {
        line(g.canvas, thirdsRect.left * 2, thirdsRect.top, thirdsRect.left * 2, thirdsRect.top + thirdsRect.top * rnd(1,3), colors.random())
    }

    gart.showImage(g)

    gart.saveImage(g)
}

fun line(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, paint: Paint) {
    val r = fatLine(x1, y1, x2, y2, 80f)
    canvas.drawPath(r, paint)
}

private fun bigRect(delta: Int, total: Int): Rect {
    val x = rnd(1, 3) * delta
    val y = rnd(1, 3) * delta
    val r = rnd(total - 3, total) * delta
    val b = rnd(total - 3, total) * delta
    return Rect(x.toFloat(), y.toFloat(), r.toFloat(), b.toFloat())
}

private fun smallRect(delta: Int, total: Int): Rect {
    val x = rnd(1, 6) * delta
    val y = rnd(1, 6) * delta
    val r = x + rnd(1, 3) * delta
    val b = y + rnd(1, 3) * delta
    return Rect(x.toFloat(), y.toFloat(), r.toFloat(), b.toFloat())
}

private fun distributedRects(count: Int, rectsProducer: () -> Rect): List<Rect> {
    val rects = mutableListOf<Rect>()
    while (rects.size < count) {
        val rect = rectsProducer()
        if (rects.any { it.contains(rect) }) {
            continue
        }
        if (rects.any { rect.contains(it) }) {
            continue
        }
        rects.add(rect)
    }
    println(rects.size)
    return rects
}
