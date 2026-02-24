package dev.oblac.gart.flowforce

import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.alpha
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.force.SpiralFlow
import dev.oblac.gart.gfx.fillOfWhite
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.RotationDirection
import dev.oblac.gart.math.rndf
import dev.oblac.gart.toFrames
import org.jetbrains.skia.Point
import kotlin.time.Duration.Companion.seconds

fun main() {
    val d = gart.d
    val g = gart.gartvas()

    // prepare field

    val spiralFlow1 = SpiralFlow(
        d.wf / 3, 1 * d.hf / 3f + 100
    )
    val spiralFlow2 = SpiralFlow(
        d.wf / 3f + 440, 2 * d.hf / 3f - 100,
        direction = RotationDirection.CCW
    )
    val spiralFlow3 = SpiralFlow(
        2 * d.wf / 3f - 200, 2 * d.hf / 3f + 100,
        spiralSpeed = 0.4f,
        direction = RotationDirection.CCW
    )

    val flowField = ForceField.of(d) { x, y ->
        spiralFlow1(x, y) + spiralFlow2(x, y) + spiralFlow3(x, y)
    }

    // prepare points

    var randomPoints = Array(30000) {
        Point(rndf(d.wf), rndf(d.hf))
    }.toList()

    g.canvas.drawPaint(fillOfWhite())

    // paint

    val stopDrawing = 15.seconds.toFrames(gart.fps)

    val w = gart.window()
    var image = g.snapshot()
    w.show { c, _, f ->
        f.onBeforeFrame(stopDrawing) {
//            flowField.drawField(g)
            randomPoints = flowField.apply(randomPoints) { old, p ->
                g.canvas.drawLine(old.x, old.y, p.x, p.y, strokeOf(CssColors.black.alpha(0x28), 1f))
            }
            image = g.snapshot()
        }
        c.drawImage(image, 0f, 0f)
    }
}
