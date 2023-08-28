package studio.oblac.gart.flowforce

import org.jetbrains.skia.Point
import studio.oblac.gart.flow.ForceField
import studio.oblac.gart.flow.SpiralVecForce
import studio.oblac.gart.gfx.Colors
import studio.oblac.gart.gfx.alpha
import studio.oblac.gart.gfx.fillOfBlack
import studio.oblac.gart.gfx.strokeOf
import studio.oblac.gart.isInside
import studio.oblac.gart.math.RotationDirection
import studio.oblac.gart.math.nextFloat
import kotlin.time.Duration.Companion.seconds

fun one(name: String) {
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

    val marker = w.frames.marker().after(10.seconds)
    val beforeMarker = marker.beforeAsFun()

    w.paintWhile(beforeMarker) {
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

    g.writeSnapshotAsImage("${name}.png")
}
