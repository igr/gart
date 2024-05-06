package dev.oblac.gart.flowforce.zpoly

import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Colors
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.gfx.drawPoint
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.*
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

val gart = Gart.of(
    "emergence",
    1024, 1024, 10
)
val d = gart.d
val g = gart.gartvas()

val fnz = ComplexFunctions.polesAndHoles(
    poles = Array(10) {
        val x = 0.4 * sin((rnd(0, 360)).toFloat().toRadian())
        val y = 0.4 * cos((rnd(0, 360)).toFloat().toRadian())
        Complex(x, y)
    },
    holes = Array(20) {
        val x = 0.2 * sin((rnd(0, 360)).toFloat().toRadian())
        val y = 0.2 * cos((rnd(0, 360)).toFloat().toRadian())
        Complex(x, y)
    },
)
val complexField = ComplexField.of(gart.d) { x, y ->
    fnz(Complex(x, y))
}
val ff = ForceField.from(gart.d) { x, y ->
    complexField[x, y].let { z -> Vector(z.real, z.img) }
}

fun main() {
    // prepare points


    val m = gart.movie()

    var rr = 100f
    repeat(400) {
        drawww(rr)
        m.addFrame(g)
        rr += 1f
        println(rr)
    }
    m.stopRecording()
    gart.saveMovie(m)

    // paint
    //gart.window().showImage(g.snapshot())
}

private fun drawww(r: Float) {
    var randomPoints = Array(360) { i ->
        val x = r * sin(i.toFloat().toRadian())
        val y = r * cos(i.toFloat().toRadian())
        Point(d.cx + x, d.cy + y)
    }.toList()

    g.canvas.clear(Colors.black)
    val color = BgColors.cloudDancer
    repeat(8000) {
        randomPoints = ff.apply(randomPoints) { _, p ->
            g.canvas.drawPoint(p, strokeOf(color, 3f).also {
                it.alpha = 0x13
                it.strokeCap = PaintStrokeCap.ROUND
            })
        }
    }
}
