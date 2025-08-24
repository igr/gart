package dev.oblac.gart

import dev.oblac.gart.gfx.draw
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("template2", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw(g)

    // save image
    //g.draw(draw)
    //gart.saveImage(g)

    w.show(draw).hotReload(g)
}

/**
 * Hot reload requires a real class to be created, not a lambda.
 */
private class MyDraw(val g: Gartvas) : Drawing(g) {
    val b = Gartmap(g)
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        draw(g.canvas, d)
        //b.updatePixelsFromCanvas()
        // draw pixels
        //b.drawToCanvas()
        c.draw(g)
    }
}


private fun draw(c: Canvas, d: Dimension) {

}
