package dev.oblac.gart.sf

import RingDrawFns
import createDrawRing
import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import org.jetbrains.skia.Canvas

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

    val rings = mutableListOf<RingDrawFns>()
    for (i in 1..10) {
        rings.add(
            createDrawRing(
                c,
                center.offset(-50f * i, 0f),
                180f + i * 80,
                100f + i * 50,
                10f,
                30f,
                3f,
                Degrees(-50f), colorInk
            )
        )
    }

    rings.map { it.first }.forEach { it() }
    c.drawCircle(center.offset(-40f, 0f), 180f, fillOf(colorBold).apply {
        //this.imageFilter = ImageFilter.makeBlur(10f, 10f, FilterTileMode.DECAL)
    })
    rings.map { it.second }.forEach { it() }

}

