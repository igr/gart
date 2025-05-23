package dev.oblac.gart.sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angles.Angle
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import org.jetbrains.skia.*

fun main() {
    val gart = Gart.of("sf3", 1024, 1024)
    println(gart)
    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)

    gart.saveImage(g)

    w.showImage(g)
}

private val colorBack = RetroColors.black01
private val colorInk = RetroColors.white01
private val colorBold = RetroColors.red01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)

    val center = d.center

    for (i in 1..10) {
        drawRing(
            c,
            center.offset(-40f * i, 0f),
            180f + i * 80,
            100f + i * 50,
            30f, 10f,
            Degrees(-50f), colorInk
        )
    }
    c.drawCircle(center.offset(-20f, 0f), 80f, fillOf(colorBold))

//    drawRing(c, center, 160f, 10f, 20f, Degrees(-60f), colorBold)
//    drawRing(c, center, 180f, 10f, 20f, Degrees(-60f), colorBold)
    //drawRing(c, center, 240f, 10f, 20f, Degrees(-60f), colorBold)

}

private fun drawRing(
    c: Canvas,
    center: Point,
    radius: Float,
    radius2: Float,
    width1: Float,
    width2: Float,
    angle: Angle,
    colorBold: Int
) {
    // calculate the inner and outer radii for the ellipse
    val outerRadiusX = radius
    val outerRadiusY = radius2

    // Create the outer oval rect
    val outerRect = Rect.makeXYWH(
        center.x - outerRadiusX,
        center.y - outerRadiusY,
        outerRadiusX * 2,
        outerRadiusY * 2
    )

    // calculate the inner radii, subtracting the width which varies around the ellipse
    // we'll interpolate between width1 and width2 based on the angle
    val innerRadiusX = outerRadiusX - width1
    val innerRadiusY = outerRadiusY - width2

    // create the inner oval rect
    val innerRect = Rect.makeXYWH(
        center.x - innerRadiusX,
        center.y - innerRadiusY,
        innerRadiusX * 2,
        innerRadiusY * 2
    )

    // draw

    c.save()
    c.rotate(angle.degrees, center.x, center.y)

    // clip the inner oval using DIFFERENCE mode to exclude it from drawing
    c.clipRRect(RRect.makeOvalXYWH(innerRect.left, innerRect.top, innerRect.width, innerRect.height), ClipMode.DIFFERENCE, true)
    // draw the filled outer oval (only the ring part will be visible due to clipping)
    c.drawOval(outerRect, fillOf(colorBold))

    c.restore()
}
