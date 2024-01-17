package dev.oblac.gart.flowforce

import dev.oblac.gart.Media
import dev.oblac.gart.flow.ForceField
import dev.oblac.gart.flow.SpiralVecForce
import dev.oblac.gart.gfx.Colors
import dev.oblac.gart.gfx.alpha
import dev.oblac.gart.gfx.fillOfBlack
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.isInside
import dev.oblac.gart.math.RotationDirection
import dev.oblac.gart.math.nextFloat
import org.jetbrains.skia.Point
import kotlin.time.Duration.Companion.seconds

fun one(name: String) {
    val d = gart.d
    val g = gart.g
    val f = gart.f
    val a = gart.a

    println(name)

    // prepare field

    val spiralVec1 = SpiralVecForce(
        d.wf / 3, 1 * d.hf / 3f + 100
    )
    val spiralVec2 = SpiralVecForce(
        d.wf / 3f + 140, 2 * d.hf / 3f - 100,
        minDistance = 150f,
        direction = RotationDirection.CCW
    )
    val spiralVec3 = SpiralVecForce(
        2 * d.wf / 3f + 200, 2 * d.hf / 3f + 100,
        spiralSpeed = 0.4f,
        direction = RotationDirection.CCW
    )

    val flowField = ForceField.of(d) { x, y ->
        spiralVec1(x, y) + spiralVec2(x, y) + spiralVec3(x, y)
    }

    // prepare points

    var randomPoints = Array(20000) {
        Point(nextFloat(d.wf), nextFloat(d.hf))
    }.toList()

    g.fill(Colors.white)
    g.canvas.drawCircle(spiralVec1.cx, spiralVec1.cy, 30f, fillOfBlack())

    // paint

    val marker = f.marker().atTime(10.seconds)

    a.draw {
        if (f before marker) {
            a.stop()
            return@draw
        }
//        flowField.drawField(g)
        randomPoints = randomPoints
            .filter { it.isInside(d) }
            .map { p ->
                with(p) {
                    flowField[x.toInt(), y.toInt()].offset(this)
                }.also {
                    g.canvas.drawLine(p.x, p.y, it.x, it.y, strokeOf(Colors.black.alpha(0x28), 1f))
                }
            }
    }

    Media.saveImage(gart)
}
