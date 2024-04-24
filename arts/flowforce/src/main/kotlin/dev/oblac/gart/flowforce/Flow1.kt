package dev.oblac.gart.flowforce

import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.alpha
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.force.SpiralVecForce
import dev.oblac.gart.gfx.fillOfBlack
import dev.oblac.gart.gfx.fillOfWhite
import dev.oblac.gart.gfx.isInside
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.RotationDirection
import dev.oblac.gart.math.nextFloat
import dev.oblac.gart.skia.Point
import dev.oblac.gart.toFrames
import kotlin.time.Duration.Companion.seconds

fun main() {
    val d = gart.d
    val g = gart.gartvas()

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

    val forceField = ForceField.of(d) { x, y ->
        spiralVec1(x, y) + spiralVec2(x, y) + spiralVec3(x, y)
    }

    // prepare points

    var randomPoints = Array(20000) {
        Point(nextFloat(d.wf), nextFloat(d.hf))
    }.toList()

    g.canvas.drawPaint(fillOfWhite())
    g.canvas.drawCircle(spiralVec1.cx, spiralVec1.cy, 30f, fillOfBlack())

    // paint

    val stopDrawing = 15.seconds.toFrames(gart.fps)

    val w = gart.window()
    var image = g.snapshot()
    w.show { c, _, f ->
        f.onBeforeFrame(stopDrawing) {
//            flowField.drawField(g)
            randomPoints = randomPoints
                .filter { it.isInside(d) }
                .map {
                    forceField[it.x, it.y]
                        .offset(it)
                        .also { p ->
                            g.canvas.drawLine(it.x, it.y, p.x, p.y, strokeOf(Colors.black.alpha(0x28), 1f))
                        }
                }
            image = g.snapshot()
        }
        c.drawImage(image, 0f, 0f)
    }
}
