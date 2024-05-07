package dev.oblac.gart

import dev.oblac.gart.force.*
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.*
import org.jetbrains.skia.Color
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("Flows", 800, 800)

    // 1
    val cx = gart.d.wf / 3
    val cy = 1 * gart.d.hf / 3f + 100
    val spiralVec1 = SpiralVecForce(cx, cy, spiralSpeed = 0.2f, maxMagnitude = 1400f, minDistance = 200f)
    val forceField1 = ForceField.of(gart.d) { x, y -> spiralVec1(x, y) }

    // 2
    val spiralVec2 = CircularVecForce(cx, cy, maxMagnitude = 1000f)
    val forceField2 = ForceField.of(gart.d) { x, y -> spiralVec2(x, y) }

    // 3
    val circleFlowForce = CircularFlow(cx, cy)
    val forceField3 = ForceField.of(gart.d) { x, y -> circleFlowForce(x, y) }
    // 4
    val spiralFlow = SpiralFlow(cx, cy)
    val forceField4 = ForceField.of(gart.d) { x, y -> spiralFlow(x, y) }
    // 5
    val waveFlow = WaveFlow(cx, cy)
    val forceField5 = ForceField.of(gart.d) { x, y -> waveFlow(x, y) }
    // 6
    val complexField = ComplexField.of(gart.d) { x, y ->
        val z = x + i * y
        ComplexFunctions.simple(z)
    }
    val forceField6 = ForceField.from(gart.d) { x, y ->
        complexField[x, y].let { c -> Vector2(c.real, c.img) }
    }

    var ff = forceField1
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
                    forceField1
                }

                Key.KEY_2 -> {
                    points = resetPoints()
                    forceField2
                }

                Key.KEY_3 -> {
                    points = resetPoints()
                    forceField3
                }

                Key.KEY_4 -> {
                    points = resetPoints()
                    forceField4
                }

                Key.KEY_5 -> {
                    points = resetPoints()
                    forceField5
                }

                Key.KEY_6 -> {
                    points = resetPoints()
                    forceField6
                }

                else -> ff
            }
        }

}
