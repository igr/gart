package dev.oblac.gart.flowforce

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.angle.Radians
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.alpha
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.force.VecForce
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.HALF_PIf
import dev.oblac.gart.math.rndf
import dev.oblac.gart.toFrames
import org.jetbrains.skia.Color
import org.jetbrains.skia.Point
import kotlin.time.Duration.Companion.seconds

fun main() {
    val gart = Gart.of(
        "interruption",
        1024, 1024
    )
    val d = gart.d
    val g = gart.gartvas()

    val voids = listOf(
        Circle(400f, 550f, 160f),
        Circle(700f, 400f, 80f),
        Circle(900f, 650f, 80f),
    )
    // prepare field
    val forceField = ForceField.of(d) { x, y ->
        for (circ in voids) {
            if (Point(x, y).distanceTo(circ.center) < circ.radius + rndf(-10f, 10f)) {
                return@of VecForce(Radians(HALF_PIf), 0f)
            }
        }
        VecForce(Radians.PI + Degrees(x * y), 2f)
    }

    // prepare points

    var randomPoints = Array(8000) {
        Point(rndf(d.wf), rndf(100f))
    }.toList()

    g.canvas.drawPaint(fillOf(BgColors.pearlWhite))
//    voids.forEach {
//        g.canvas.drawCircle(it.x, it.y, it.radius, strokeOfRed(1f))
//    }


    // paint

    val stopDrawing = 6.seconds.toFrames(gart.fps)
    val pal = Palettes.gradient(BgColors.elegantDark, CssColors.crimson, 100)

    val w = gart.window()
    var image = g.snapshot()
    w.show { c, _, f ->
        f.onBeforeFrame(stopDrawing) {
            g.canvas.translate(-20f, 20f)
//            flowField.drawField(g)
            randomPoints = randomPoints
                .filter { it.isInside(d) }
                .map {
                    forceField[it.x, it.y]
                        .offset(it)
                        .also { p ->
                            var color = BgColors.elegantDark.alpha(0x28)
                            if (it.y > 200) {
                                color = pal[it.y.toInt() / 10]
                            }
                            val paint = strokeOf(color, 1f + it.y / 100f).also {
                                it.alpha = 0x28
                            }
                            g.canvas.drawPoint(p.x, p.y, paint)
                            //g.canvas.drawLine(it.x, it.y, p.x, p.y, strokeOf(BgColors.elegantDark.alpha(0x28), 1f))
                        }
                }
            g.canvas.translate(20f, -20f)
            g.canvas.drawBorder(g.d, 20f, Color.WHITE)
            image = g.snapshot()
        }
        f.onFrame(stopDrawing) {
            gart.saveImage(image)
        }
        c.drawImage(image, 0f, 0f)
    }
}
