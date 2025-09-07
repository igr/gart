package dev.oblac.gart.flowforce

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.force.VecForce
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndGaussian
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of(
        "circlex",
        1024, 1024
    )
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

    // prepare field
    val forceField = ForceField.of(d) { x, y ->
        val a = 90 + sin(x * 0.01f) * 40 + cos(y * 0.005f) * 40
        VecForce(Degrees.of(a), 2f)
    }

    // prepare points

    var randomPoints = Array(8000) {
        Point(
            rndGaussian(200f, 100f),
            rndGaussian(d.cy, 300f)
        )
    }.toList()

    // draw
    val clr = NipponColors.col016_KURENAI

    c.clear(clr)
    val circle = Circle(d.cx, d.cy, 400f)
    c.drawCircle(circle, fillOf(BgColors.pearlWhite))

    c.save()
    c.clipPath(Circle(d.cx, d.cy, 400f).toPath())
    c.rotate(50f, d.cx, d.cy)
    repeat(1000) {
        randomPoints = forceField.apply(randomPoints) { oldPoint, newPoint ->
            c.drawPoint(newPoint, strokeOf(clr, 1f).apply {
                alpha = 100
            })
        }
    }
    c.restore()

    //forceField.drawField(g.canvas, d)

    gart.saveImage(g)
    val w = gart.window()
    w.showImage(g)
}
