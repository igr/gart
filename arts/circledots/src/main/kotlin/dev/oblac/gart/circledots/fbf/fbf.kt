package dev.oblac.gart.circledots.fbf

import dev.oblac.gart.*
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.math.doubleLoopSequence
import dev.oblac.gart.math.f
import dev.oblac.gart.math.rndi
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.GradientStyle
import org.jetbrains.skia.Image
import org.jetbrains.skia.Matrix33.Companion.makeRotate
import org.jetbrains.skia.Shader.Companion.makeSweepGradient

fun main() {
    val gart = Gart.of("fbf", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDraw(g: Gartvas) : Drawing(g) {
    init {
        val b = Gartmap(g)
        val d = g.d
        draw(g.canvas, d)
    }
}

private fun draw(c: Canvas, d: Dimension) {
    val n = 12 // n×n grid
    val squareSize = d.rect.width / n

    doubleLoopSequence(n, n).map {
        Pair(it, drawSquare(squareSize, ((it.first + 1) * (it.second + 1) * 2).f()))
    }.forEach { (p, image) ->
        val x = p.second * squareSize
        val y = p.first * squareSize
        c.drawImage(image, x, y, null)
    }
}

private fun drawSquare(w: Float, rotationAngle: Float = 0f): Image {
    val rndOff = rndi(1, 80)
    val g = Gartvas.of(
        w + rndOff,
        w + rndOff
    )
    val d = g.d
    val c = g.canvas

    val colors = intArrayOf(BgColors.linen, BgColors.richBlack, BgColors.linen)
    val positions = floatArrayOf(0.0f, 0.5f, 1.0f)

    // Create the radial gradient shader
    val paint = paint()
    val rotationMatrix = makeRotate(-rotationAngle, d.center.x, d.center.y)

    paint.shader = makeSweepGradient(
        d.center,
        0f,
        360f,
        colors,
        positions,
        GradientStyle.DEFAULT
    ).makeWithLocalMatrix(rotationMatrix)
    c.drawRect(d.rect, paint)

//    val b = Gartmap(g)
//    b.updatePixelsFromCanvas()
//    //ditherSierraLite(b, 1, 32)
//    b.drawToCanvas()

    return g.snapshot()
}
