package dev.oblac.gart.alphastripes

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndb
import dev.oblac.gart.text.drawTextOnPath
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.PathBuilder

fun main() {
    val gart = Gart.of("alpha-stripes", 1280, 1280)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    val draw = AlphaStripesDraw(g)

    // save image
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class AlphaStripesDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

private val font = font(FontFamily.IBMPlexMono, 20f)

private val white = NipponColors.col234_GOFUN
private val black = NipponColors.col248_SUMI

private fun draw(c: Canvas, d: Dimension) {
    c.clear(black)

    val center = Point(d.w3, d.h3)
    repeat(800) {
        val string = randomString()
        val p = randomPoint(d)
        if (it == 400) {
            c.drawCircle(center.offset(150f, 50f), 700f, strokeOf(80f, NipponColors.col047_TERIGAKI).alpha(200).apply {
                blendMode = BlendMode.OVERLAY
                maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, 20f)
            })
        }
        if (it == 600) {
            c.drawCircle(center.offset(-150f, -50f), 700f, strokeOf(120f, NipponColors.col047_TERIGAKI).alpha(200).apply {
                blendMode = BlendMode.OVERLAY
                maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, 20f)
            })
        }
        PathBuilder().moveTo(center).lineTo(p).detach().let { path ->
            c.drawTextOnPath(path, string, font, fillOf(white).apply { alpha = 100})
        }
    }
    d.rect.splitToGrid(10, 10).forEach { cell ->
        c.drawRect(cell, fillOf(black).apply {
//            blendMode = if (rndb()) BlendMode.DIFFERENCE else BlendMode.OVERLAY
            blendMode = if (rndb()) BlendMode.OVERLAY else BlendMode.DIFFERENCE
        })
    }

}

private fun randomString(): String {
    return buildString {
        repeat(70) { append((0..1).random()) }
    }
}
