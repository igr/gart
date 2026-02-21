package dev.oblac.gart.rectaround

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.Transform
import dev.oblac.gart.math.doubleLoop
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

fun main() {
    val gart = Gart.of("rectaround", 1280, 1280)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // Hot reload requires a real class to be created, not a lambda!

    val draw = RectAroundDraw(g)

    // save image
    //g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class RectAroundDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)

    val gap = 128f
    val n = 10  // number of squares
    val w = (d.w - 2 * gap) / n
    val deltaAngle = 66f / n
    doubleLoop(n, n) { (i, j) ->
        val x = i * w + gap
        val y = j * w + gap
        val r = Rect.makeXYWH(x, y, w, w).shrink(5f)

        val path = r.rotate(r.center(), Degrees.of(i * deltaAngle))
        c.drawPath(path, fillOf(RetroColors.white01))
        //c.drawRotatedRect(r.center(), w - 10f, w - 10f, Degrees.of(i * deltaAngle), strokeOfBlue(2f))
        c.drawRotatedRect(r.center(), w /3, w /3, Degrees.of((n-i -1) * deltaAngle), fillOf(RetroColors.red01))
    }
}

private fun Rect.rotate(center: Point, angle: Angle): Path {
    val transformation = Transform.rotate(center, angle)
    return points().map { transformation(it) }.toClosedPath()
}
