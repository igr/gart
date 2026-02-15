package dev.oblac.gart.pixelmania.glass

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.createDrawRing
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.glass.drawGlassBall
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun main() {
    val gart = Gart.of("glass", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    val draw = GlassDraw(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw)
    //w.show(draw).hotReload(g)
}


private class GlassDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g, g.d)
    }
}

private fun draw(g: Gartvas, d: Dimension) {
    val c = g.canvas
    c.clear(RetroColors.black01)

    val p = Point(d.w3x2, d.h3)
    val n = 180   // Number of radial lines
    val maxLen = sqrt((d.wf * d.wf + d.hf * d.hf))

    for (i in 0 until n) {
        val angle = Math.toRadians(i * 360.0 / n).toFloat()
        val x2 = p.x + cos(angle) * maxLen
        val y2 = p.y + sin(angle) * maxLen
        c.drawLine(p.x, p.y, x2, y2, strokeOf(5f, RetroColors.white01))
    }
//    c.drawCircle(p.x - 2, p.y + 16, 24f, fillOf(RetroColors.black01))

    val dr = createDrawRing(
        Point(500f, 500f),
        280f,
        200f,
        30f,
        60f,
        30f,
        Degrees(-50f)
    )
    dr.first(c, fillOf(RetroColors.red01))
    dr.second(c, fillOf(RetroColors.red01))


// Glass ball / water drop effect
    drawGlassBall(g, d.w3, d.h3x2, 260f, baseColor = RetroColors.black01, eta = 1.0 / 1.3)
//    drawGlassBall(g, d.w3 + 200f, d.h3x2 - 260f, 160f, baseColor = RetroColors.black01, eta = 1.0 / 1.3)

//    val path = Path().addRRect(RRect.makeXYWH(100f, d.h3x2 - 100f, d.w - 200f, 300f, 80f))
//    drawGlassPath(g, path, eta = 1 / 1.2, baseColor = RetroColors.black01)


}
