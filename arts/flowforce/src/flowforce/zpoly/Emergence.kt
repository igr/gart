package flowforce.zpoly

import dev.oblac.gart.Gart
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.flow.FlowField
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
val d = _root_ide_package_.flowforce.zpoly.gart.d
val g = _root_ide_package_.flowforce.zpoly.gart.gartvas()

val fnz = ComplexFunctions.polesAndHoles(
    poles = Array(10) {
        val x = 0.4 * sin((rndf(0, 360)).toRadians())
        val y = 0.4 * cos((rndf(0, 360)).toRadians())
        Complex(x, y)
    },
    holes = Array(20) {
        val x = 0.2 * sin((rndf(0, 360)).toRadians())
        val y = 0.2 * cos((rndf(0, 360)).toRadians())
        Complex(x, y)
    },
)
val complexField = ComplexField.of(_root_ide_package_.flowforce.zpoly.gart.d) { x, y ->
    _root_ide_package_.flowforce.zpoly.fnz(Complex(x, y))
}
val ff = FlowField.from(_root_ide_package_.flowforce.zpoly.gart.d) { x, y ->
    _root_ide_package_.flowforce.zpoly.complexField[x, y].let { z ->
        _root_ide_package_.dev.oblac.gart.vector.Vec2(
            z.real,
            z.imag
        )
    }
}

fun main() {
    // prepare points


//    val m = gart.movie()

    _root_ide_package_.flowforce.zpoly.g.canvas.clear(CssColors.black)
    val pal = Palettes.cool34.expand(400)
    var rr = 100f
    repeat(400) {
        _root_ide_package_.flowforce.zpoly.drawww(rr, pal[it])
        //m.addFrame(g)
        rr += 1f
        println(rr)
    }
//    m.stopRecording()
//    gart.saveMovie(m)
    _root_ide_package_.flowforce.zpoly.gart.saveImage(_root_ide_package_.flowforce.zpoly.g)

    // paint
    //gart.window().showImage(g.snapshot())
}

private fun drawww(r: Float, color: Int) {
    var randomPoints = Array(360) { i ->
        val x = r * sin(i.toFloat().toRadians())
        val y = r * cos(i.toFloat().toRadians())
        Point(_root_ide_package_.flowforce.zpoly.d.cx + x, _root_ide_package_.flowforce.zpoly.d.cy + y)
    }.toList()

    //g.canvas.clear(Colors.black)  // for animation
    //val color = BgColors.cloudDancer
    repeat(8000) {
        randomPoints = _root_ide_package_.flowforce.zpoly.ff.apply(randomPoints) { _, p ->
            _root_ide_package_.flowforce.zpoly.g.canvas.drawPoint(p, strokeOf(color, 3f).also {
                it.alpha = 0x13
                it.strokeCap = PaintStrokeCap.ROUND
            })
        }
    }
}
