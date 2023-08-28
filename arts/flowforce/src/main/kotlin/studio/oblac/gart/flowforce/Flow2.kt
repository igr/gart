package studio.oblac.gart.flowforce

import org.jetbrains.skia.Point
import studio.oblac.gart.flow.ForceField
import studio.oblac.gart.flow.SpiralFlowForce
import studio.oblac.gart.gfx.Colors
import studio.oblac.gart.gfx.alpha
import studio.oblac.gart.gfx.strokeOf
import studio.oblac.gart.isInside
import studio.oblac.gart.math.RotationDirection
import studio.oblac.gart.math.nextFloat
import kotlin.time.Duration.Companion.seconds

fun two(name: String) {
    println(name)

    // prepare field

    val spiralFlow1 = SpiralFlowForce(
        d.wf / 3, 1 * d.hf / 3f + 100
    )
    val spiralFlow2 = SpiralFlowForce(
        d.wf / 3f + 440, 2 * d.hf / 3f - 100,
        direction = RotationDirection.CCW
    )
    val spiralFlow3 = SpiralFlowForce(
        2 * d.wf / 3f - 200, 2 * d.hf / 3f + 100,
        spiralSpeed = 0.4f,
        direction = RotationDirection.CCW
    )

    val flowField = ForceField.of(d) { x, y ->
        spiralFlow1(x, y) + spiralFlow2(x, y) + spiralFlow3(x, y)
    }

    // prepare points

    var randomPoints = Array(30000) {
        Point(nextFloat(d.wf), nextFloat(d.hf))
    }.toList()

    g.fill(Colors.white)

    // paint

    val marker = w.frames.marker().after(15.seconds)
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
