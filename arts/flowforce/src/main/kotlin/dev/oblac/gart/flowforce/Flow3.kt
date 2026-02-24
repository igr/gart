package dev.oblac.gart.flowforce

import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.alpha
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.force.SpiralFlow
import dev.oblac.gart.force.WaveFlow
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.RotationDirection.CCW
import dev.oblac.gart.math.RotationDirection.CW
import dev.oblac.gart.math.rndf
import dev.oblac.gart.toFrames
import org.jetbrains.skia.Point
import kotlin.time.Duration.Companion.seconds

fun main() {
    val d = gart.d
    val g = gart.gartvas()

    // prepare field

    val wave = WaveFlow(
        xFreq = 0.002f,
        yFreq = 0.01f,
        xAmp = 1.2f,
        yAmp = 0.5f,
        magnitude = 1f
    )

    val spiralFlow1 = SpiralFlow(
        400f, 400f,
        spiralSpeed = 0.8f,
        magnitude = 10f,
        direction = CW
    )

    val spiralFlow3 = SpiralFlow(
        2 * d.wf / 3f - 200, 2 * d.hf / 3f + 100,
        spiralSpeed = 0.3f,
        magnitude = 10f,
        direction = CCW
    )

    val flowField = ForceField.of(d) { x, y -> spiralFlow3(x, y) + wave(x, y) }
    val flowField2 = ForceField.of(d) { x, y -> spiralFlow1(x, y) }

    // prepare points
    val gradient = Palettes.cool1.expand(3000)
    val palette = gradient + gradient.reversed()

    var randomPoints = Array(40000) {
        Point(rndf(d.wf), rndf(d.hf))
    }.toList()

    var randomPoints2 = Array(100) {
        Point(rndf(1024), 1000f)
    }.toList()

//    g.fill(Colors.white)

    // paint

    val marker = 6.seconds.toFrames(gart.fps)
    val markerMiddle = 3.seconds.toFrames(gart.fps)
    val w = gart.window()
    var image = g.snapshot()
    w.show { c, _, f ->
//        flowField.drawField(g)

        f.onBeforeFrame(markerMiddle) {
            randomPoints = flowField.apply(randomPoints) { old, p ->
                val color = palette.safe((p.x + p.y).toInt() * 2)
                g.canvas.drawLine(old.x, old.y, p.x, p.y, strokeOf(color.alpha(0x40), 1f))
            }
            image = g.snapshot()
        }
        f.onAfterFrame(markerMiddle) {
            randomPoints2 = flowField2.apply(randomPoints2) { old, p ->
                g.canvas.drawLine(-old.x, old.y, -p.x, p.y, strokeOf(CssColors.white, 1f))
            }
            image = g.snapshot()
        }
        c.drawImage(image, 0f, 0f)
    }

    //g.canvas.drawRect(Rect(0f, 0f, d.wf, d.hf), strokeOf(Colors.white, 40f))
}
