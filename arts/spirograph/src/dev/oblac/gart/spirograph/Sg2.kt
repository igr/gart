package dev.oblac.gart.spirograph

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.util.circular
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path

fun main() {
    val gart = Gart.of("spirograph2", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val m = gart.movie()

    var r = 0f
    var a = Degrees.of(3f)
    m.record(w).show { c, d, f ->
        //w.show { c, d, _ ->
        draw(c, d, r, a)
        r += 1f
        if (f.frame == 400L) {
            m.stopRecording()
            gart.saveImage(c)
        }
    }
}

private val pal = Palettes.cool16.expand(1000)

private fun draw(c: Canvas, d: Dimension, r: Float, a: Angle) {
    c.clear(BgColors.coolDark)

    val path1 = path1(d)
    val spiro1 = createSpirograph(
        d, path1,
        radius = r,
        a,//Degrees.of(10f),
        samples = 140,
        repetitions = 160
    )
    val spiro1Safe = spiro1.points.circular()
    spiro1Safe.forEachIndexed { ndx, p ->
        val line = Line(spiro1Safe[ndx - 1], p)
        c.drawLine(line, strokeOf(pal.safe(ndx), 1f))
    }
}

private fun path1(d: Dimension): Path {
    return createCircleOfPoints(d.center, 150f, 100).toClosedPath()
}
