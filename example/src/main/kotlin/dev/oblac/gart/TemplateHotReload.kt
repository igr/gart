package dev.oblac.gart

import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.fillOfGreen
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("template2", 1280, 1280)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // Hot reload requires a real class to be created, not a lambda!

//    val draw = MyDraw1()
//    val draw = MyDraw2(g)
//    val draw = MyDraw3(g)
    val draw = MyDraw4(g)

    // save image
    //g.draw(draw)
    //gart.saveImage(g)

    w.show(draw).hotReload(g)
}

/**
 * (1) This version draws directly to the canvas (not using Gartvas).
 */
private class MyDraw1 : Drawing() {
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        draw(c, d)
    }
}

/**
 * (2) This version draws to the Gartmap, and that is drawn to the canvas.
 */
private class MyDraw2(val g: Gartvas) : Drawing(g) {
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        draw(g.canvas, d)
    }
}

/**
 * This version draws static image.
 */
private class MyDraw3(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

/**
 * (4) This version draws to the Gartmap (like 2), but also updates pixels.
 */
private class MyDraw4(val g: Gartvas) : Drawing(g) {
    val b = Gartmap(g)
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        draw(g.canvas, d)
        b.updatePixelsFromCanvas()
        b.drawToCanvas()
    }
}


private fun draw(c: Canvas, d: Dimension) {
    c.clear(Colors.white)
    c.drawCircle(512f, 512f, rndf(300, 400), fillOfGreen())
}
