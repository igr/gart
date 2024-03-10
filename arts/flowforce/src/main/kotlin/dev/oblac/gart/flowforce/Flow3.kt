package dev.oblac.gart.flowforce

import dev.oblac.gart.Media
import dev.oblac.gart.flow.ForceField
import dev.oblac.gart.flow.SpiralFlowForce
import dev.oblac.gart.flow.WaveFlowForce
import dev.oblac.gart.gfx.Colors
import dev.oblac.gart.gfx.Palettes
import dev.oblac.gart.gfx.alpha
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.isInside
import dev.oblac.gart.math.RotationDirection.CCW
import dev.oblac.gart.math.RotationDirection.CW
import dev.oblac.gart.math.nextFloat
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.time.Duration.Companion.seconds

fun three(name: String) {
    val d = gart.d
    val g = gart.g
    val f = gart.f
    val a = gart.m

    println(name)

    // prepare field

    val wave = WaveFlowForce(
        xFreq = 0.002f,
        yFreq = 0.01f,
        xAmp = 1.2f,
        yAmp = 0.5f,
        magnitude = 1f
    )

    val spiralFlow1 = SpiralFlowForce(
        400f, 400f,
        spiralSpeed = 0.8f,
        magnitude = 10f,
        direction = CW
    )

    val spiralFlow3 = SpiralFlowForce(
        2 * d.wf / 3f - 200, 2 * d.hf / 3f + 100,
        spiralSpeed = 0.3f,
        magnitude = 10f,
        direction = CCW
    )

    val flowField = ForceField.of(d) { x, y -> spiralFlow3(x, y) + wave(x, y) }
    val flowField2 = ForceField.of(d) { x, y -> spiralFlow1(x, y) }

    // prepare points
    val gradient = Palettes.gradient(Palettes.cool1, 3000)
    val palette = gradient + gradient.reversed()

    var randomPoints = Array(40000) {
        Point(nextFloat(d.wf), nextFloat(d.hf))
    }.toList()

    var randomPoints2 = Array(100) {
        Point(nextFloat(1024), 1000f)
    }.toList()

    g.fill(Colors.white)

    // paint

    val marker = f.marker().atTime(6.seconds)

    val markerMiddle = f.marker().atTime(3.seconds)

    a.draw {
        if (f after marker) {
            a.stop()
            return@draw
        }
//        flowField.drawField(g)

        if (f before markerMiddle) {
            randomPoints = randomPoints
                .filter { it.isInside(d) }
                .map { p ->
                    with(p) {
                        flowField[x.toInt(), y.toInt()].offset(this)
                    }.also {
                        val color = palette.getSafe((it.x + it.y).toInt() * 2)
                        g.canvas.drawLine(p.x, p.y, it.x, it.y, strokeOf(color.alpha(0x40), 1f))
                    }
                }
        } else {
            randomPoints2 = randomPoints2
                .filter { it.isInside(d) }
                .map { p ->
                    with(p) {
                        flowField2[x.toInt(), y.toInt()].offset(this)
                    }.also {
                        g.canvas.drawLine(-p.x, p.y, -it.x, it.y, strokeOf(Colors.white, 1f))
                    }
                }
        }
    }

    println("switching to second field")

    g.canvas.drawRect(Rect(0f, 0f, d.wf, d.hf), strokeOf(Colors.white, 40f))

    Media.saveImage(gart)
}
