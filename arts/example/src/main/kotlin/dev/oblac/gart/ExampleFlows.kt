package dev.oblac.gart

import dev.oblac.gart.force.*
import dev.oblac.gart.gfx.isInside
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.rnd
import dev.oblac.gart.skia.SkikoKey
import dev.oblac.gart.skia.SkikoKeyboardEventKind
import org.jetbrains.skia.Color
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("Flows", 800, 800)

    // 1
    val cx = gart.d.wf / 3
    val cy = 1 * gart.d.hf / 3f + 100
    val spiralVec1 = SpiralVecForce(cx, cy, spiralSpeed = 0.2f, maxMagnitude = 1400f, minDistance = 200f)
    val flowField1 = ForceField.of(gart.d) { x, y -> spiralVec1(x, y) }

    // 2
    val spiralVec2 = CircularVecForce(cx, cy, maxMagnitude = 1000f)
    val flowField2 = ForceField.of(gart.d) { x, y -> spiralVec2(x, y) }

    // 3
    val circleFlowForce = CircularFlow(cx, cy)
    val flowField3 = ForceField.of(gart.d) { x, y -> circleFlowForce(x, y) }
    // 4
    val spiralFlow = SpiralFlow(cx, cy)
    val flowField4 = ForceField.of(gart.d) { x, y -> spiralFlow(x, y) }
    // 5
    val waveFlow = WaveFlow(cx, cy)
    val flowField5 = ForceField.of(gart.d) { x, y -> waveFlow(x, y) }

    var ff = flowField1
    val w = gart.window()

    fun resetPoints(): List<Point> = Array(1000) {
        Point(rnd(0f, gart.d.wf), rnd(0f, gart.d.hf))
    }.toList()

    var points = resetPoints()

    val stroke = strokeOf(Color.BLACK, 4f)

    w.show { c, d, _ ->
        ff.drawField(c, d)

        points = points
            .filter { it.isInside(d) }
            .map {
                ff[it.x, it.y]
                    .offset(it)
                    .also { p ->
                        c.drawLine(it.x, it.y, p.x, p.y, stroke)
                    }
            }.toList()
    }
        .keyboardHandler {
            if (it.kind != SkikoKeyboardEventKind.UP) {
                return@keyboardHandler
            }
            ff = when (it.key) {
                SkikoKey.KEY_1 -> {
                    points = resetPoints()
                    flowField1
                }

                SkikoKey.KEY_2 -> {
                    points = resetPoints()
                    flowField2
                }

                SkikoKey.KEY_3 -> {
                    points = resetPoints()
                    flowField3
                }

                SkikoKey.KEY_4 -> {
                    points = resetPoints()
                    flowField4
                }

                SkikoKey.KEY_5 -> {
                    points = resetPoints()
                    flowField5
                }

                else -> ff
            }
        }

}
