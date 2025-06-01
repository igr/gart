package dev.oblac.gart.sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("sf6", 1024, 1024)
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

    val satellite = Circle(790f, 220f, 50f)
    c.drawCircle(satellite, fillOf(colorBold))

    val sun = Circle(d.cx, d.cy, 280f)

    // hexagons path for layers
    val path = createNtagonPoints(n = 8, d.cx, d.cy, radius = 300f)
    val layers = Array(50) { createLayer(path, offsetStdDev = 20f, count = 10).toClosedPath() }
    layers.forEach {
        c.drawPath(it, fillOf(colorInk).apply {
            this.alpha = 10
        })
    }

    // draw moon to emulate 3D effect
    c.save()
    c.rotate(-80f, d.cx, d.cy)
    Moon(
        sun,
        fillOf(0x00000000),
        fillOf(colorBack),
        moonPhase = 0.3f
    ).invoke(c, d)
    c.restore()

    // draw arc
    val circle = Circle(d.cx + 50, d.cy - 250f, 380f)
        .points(20, Degrees.of(53f), Degrees.of(105f))

    val oreos = Array(50) {
        createLayer2(circle, offsetStdDev = 10f, count = 6).toPath()
    }
    oreos.forEach {
        c.drawPath(it, strokeOf(colorInk, 1f).apply {
            this.alpha = 16
        })
    }
}

private fun createLayer(points: List<Point>, offsetStdDev: Float = 15f, count: Int = 10): List<Point> {
    var p = points
    repeat(count) {
        p = deformPath(p, offsetStdDev)
    }
    return p
}

private fun createLayer2(points: List<Point>, offsetStdDev: Float = 15f, count: Int = 10): List<Point> {
    var p = points
    repeat(count) {
        p = deformPath(p, offsetStdDev).dropLast(1)
    }
    return p
}
