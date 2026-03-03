package dev.oblac.gart

import dev.oblac.gart.flow.*
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.*
import dev.oblac.gart.vector.Vector2
import org.jetbrains.skia.Color
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("Flows", 800, 800)

    // 1
    val cx = gart.d.wf / 3
    val cy = 1 * gart.d.hf / 3f + 100
    val spiralVec1 = SpiralVecFlow(cx, cy, spiralSpeed = 0.2f, maxMagnitude = 1400f, minDistance = 200f)
    val flowField1 = FlowField.of(gart.d) { x, y -> spiralVec1(x, y) }

    // 2
    val spiralVec2 = CircularVecFlow(cx, cy, maxMagnitude = 1000f)
    val flowField2 = FlowField.of(gart.d) { x, y -> spiralVec2(x, y) }

    // 3
    val circleFlowForce = CircularFlow(cx, cy)
    val flowField3 = FlowField.of(gart.d) { x, y -> circleFlowForce(x, y) }
    // 4
    val spiralFlow = SpiralFlow(cx, cy)
    val flowField4 = FlowField.of(gart.d) { x, y -> spiralFlow(x, y) }
    // 5
    val waveFlow = WaveFlow(cx, cy)
    val flowField5 = FlowField.of(gart.d) { x, y -> waveFlow(x, y) }
    // 6
    val complexField = ComplexField.of(gart.d) { x, y ->
        val z = x + i * y
        ComplexFunctions.simple(z)
    }
    val flowField6 = FlowField.from(gart.d) { x, y ->
        complexField[x, y].let { c -> Vector2(c.real, c.imag) }
    }

    var ff = flowField1
    val w = gart.window()

    fun resetPoints(): List<Point> = Array(1000) {
        Point(rndf(0f, gart.d.wf), rndf(0f, gart.d.hf))
    }.toList()

    var points = resetPoints()

    val stroke = strokeOf(Color.BLACK, 4f)

    w.show { c, d, _ ->
        ff.drawField(c, d)

        points = ff.apply(points) { old, p ->
            c.drawLine(old.x, old.y, p.x, p.y, stroke)
        }
    }
        .onKey {
            ff = when (it) {
                Key.KEY_1 -> {
                    points = resetPoints()
                    flowField1
                }

                Key.KEY_2 -> {
                    points = resetPoints()
                    flowField2
                }

                Key.KEY_3 -> {
                    points = resetPoints()
                    flowField3
                }

                Key.KEY_4 -> {
                    points = resetPoints()
                    flowField4
                }

                Key.KEY_5 -> {
                    points = resetPoints()
                    flowField5
                }

                Key.KEY_6 -> {
                    points = resetPoints()
                    flowField6
                }

                else -> ff
            }
        }

}
