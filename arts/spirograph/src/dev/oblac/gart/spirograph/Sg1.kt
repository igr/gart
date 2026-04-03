package dev.oblac.gart.spirograph

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.util.circular
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathOp

fun main() {
    val gart = Gart.of("spirograph1", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()
    val g = gart.gartvas()
    val c = g.canvas

    draw(c, d)
    gart.saveImage(g,)
    w.showImage(g)
}

private val pal = Palettes.colormap058.expand(1000)

private fun draw(c: Canvas, d: Dimension) {
    c.clear(BgColors.coolDark)

    val path1 = path1(d)
    val spiro1 = createSpirograph(d, path1,
        80f,
        Degrees.of(10f),
        samples = 140
    , repetitions = 1220)
    val spiro1Safe = spiro1.points.circular()
    spiro1Safe.forEachIndexed { ndx, p ->
        val line = Line(spiro1Safe[ndx - 1], p)
        c.drawLine(line, strokeOf(pal.safe(ndx), 1f))
    }
}

private fun path1(d: Dimension): Path {
    val dc = d.center
    val path1 = createCircleOfPoints(dc, 150f, 55).toClosedPath()
    val path2 = createCircleOfPoints(dc.offset(50f, -100f), 100f, 55).toClosedPath()
    val combined = combinePathsWithOp(PathOp.UNION, path1, path2)
    return combined
}
