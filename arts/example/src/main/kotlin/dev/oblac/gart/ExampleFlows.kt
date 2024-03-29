package dev.oblac.gart

import dev.oblac.gart.force.*
import dev.oblac.gart.gfx.isInside
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.*
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
        complexField[x, y].let { c -> Vector(c.real, c.img) }
    }

    var ff = forceField1
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
                    forceField1
                }

                SkikoKey.KEY_2 -> {
                    points = resetPoints()
                    forceField2
                }

                SkikoKey.KEY_3 -> {
                    points = resetPoints()
                    forceField3
                }

                SkikoKey.KEY_4 -> {
                    points = resetPoints()
                    forceField4
                }

                SkikoKey.KEY_5 -> {
                    points = resetPoints()
                    forceField5
                }

                SkikoKey.KEY_6 -> {
                    points = resetPoints()
                    forceField6
                }

                else -> ff
            }
        }

}
