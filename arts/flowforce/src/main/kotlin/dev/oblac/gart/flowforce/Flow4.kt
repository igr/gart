package dev.oblac.gart.flowforce

import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.alpha
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.force.WaveFlow
import dev.oblac.gart.gfx.shrink
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.toFrames
import org.jetbrains.skia.Point
import java.util.*
import kotlin.time.Duration.Companion.seconds

fun main() {
    val d = gart.d
    val g = gart.gartvas()

    val wave1 = WaveFlow(
        xFreq = 0.04f,
        yFreq = 0.04f,
        xAmp = 1.2f,
        yAmp = 1.2f,
        magnitude = 0.6f
    )


    val flowField = ForceField.of(d) { x, y -> wave1(x, y) }
    val stopDrawing = 10.seconds.toFrames(gart.fps)

    val gradient = Palettes.cool36.expand(3000)
    val palette = gradient

    val rnd = Random()

    var randomPoints = Array(30000) {
        Point(
            rnd.nextGaussian(d.wf.toDouble() / 2, d.wf.toDouble() / 4).toFloat(),
            (rnd.nextGaussian(d.hf.toDouble() / 6, d.hf.toDouble() / 4)).toFloat()
        )
    }.toList()

    g.canvas.clear(BgColors.sand)

    val w = gart.window()
    var image = g.snapshot()

    w.show { c, _, f ->
        f.onBeforeFrame(stopDrawing) {
//            flowField.drawField(g)

            randomPoints = flowField.apply(randomPoints) { old, p ->
                val color = palette.safe(p.y.toInt() * 3)
                g.canvas.drawLine(old.x, old.y, p.x, p.y, strokeOf(color.alpha(0x33), 1f))
            }
            image = g.snapshot()
        }
        f.onFrame(stopDrawing) {
            val border = 30f
            g.canvas.drawImage(image, 30f, 0f)
            g.canvas.drawRect(gart.d.rect.shrink(border / 2), strokeOf(BgColors.milkMustache, border))
            image = g.snapshot()
            println("Done")
            gart.saveImage(image)
        }
        c.drawImage(image, 0f, 0f)
    }


}
