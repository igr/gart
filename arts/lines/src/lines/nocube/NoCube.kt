package lines.nocube

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.gfx.toPath
import dev.oblac.gart.gfx.toPoints
import dev.oblac.gart.math.i
import dev.oblac.gart.math.rndi
import dev.oblac.gart.noise.noise
import dev.oblac.gart.perspective.Block3D
import dev.oblac.gart.util.forSequence
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import kotlin.math.sin

fun main() {
    val gart = Gart.of("nocube", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)
    gart.saveImage(g)
    w.showImage(g)
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)

    val pLeft = Point(-100f, 700f)
    val pRight = Point(1100f, 700f)
    val xCenter = 660f
    val block = Block3D.of(
        vpLeft = pLeft,
        vpRight = pRight,
        frontBottom = Point(xCenter, 980f),
        height = 900f,
        leftWidth = 400f,
        rightWidth = 500f
    )

    val left = block.left

    val yFrom = left.topPoint().y
    val yTo = left.bottomPoint().y

//    forSequence(yFrom.i(), yTo.i(), 40).forEach { y ->
//        val p = PathBuilder().moveTo(pLeft).lineTo(Point(xCenter, y)).lineTo(pRight).detach()
//        c.drawPath(p, strokeOfWhite(0.5f))
//    }


    forSequence(yFrom.i(), yTo.i(), 20).forEach { y ->
        val p = PathBuilder().moveTo(pLeft).lineTo(Point(xCenter, y)).lineTo(pRight).detach()
        //c.drawPath(p, strokeOfWhite(2f))
        val N = 1000
        val points = p.toPoints(N)

        // create two sinusoids waves, perturbed by noise (offset right so the edges wiggle independently)
        val left = 300 + sin(y * 0.01f) * 50 + noise(y * 0.03f) * 60
        val right = N - 300 - sin(y * 0.01f - 0.2f) * 100 + noise(y * 0.03f + 1000f) * 80

        // sub-path following the bent line, trimmed at the oscillating ends
        val from = left.i().coerceIn(0, N - 1)
        val to = right.i().coerceIn(from, N)
        val wave = points.subList(from, to).toPath()
        c.drawPath(wave, strokeOf(RetroColors.white01, 4f))

        val from2 = left.i() + rndi(0, 10) * 10
        val to2 = right.i() - rndi(0, 10) * 10
        val wave2 = points.subList(from2, to2).toPath()
        c.drawPath(wave2, strokeOf(RetroColors.red01, 4f))
    }


}
