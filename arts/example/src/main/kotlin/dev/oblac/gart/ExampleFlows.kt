package dev.oblac.gart

import dev.oblac.gart.flow.*
import dev.oblac.gart.skia.SkikoKey
import dev.oblac.gart.skia.SkikoKeyboardEventKind

fun main() {
    val gart = Gart.of("Flows", 800, 800)

    // 1
    val spiralVec1 = SpiralVecForce(
        gart.d.wf / 3, 1 * gart.d.hf / 3f + 100
    )
    val flowField1: ForceField<VecForce> = ForceField.of(gart.d) { x, y -> spiralVec1(x, y) }

    // 2
    val spiralVec2 = CircularVecForce(
        gart.d.wf / 3, 1 * gart.d.hf / 3f + 100
    )
    val flowField2: ForceField<VecForce> = ForceField.of(gart.d) { x, y -> spiralVec2(x, y) }

    // 3
    val circleFlowForce = CircularFlowForce(
        gart.d.wf / 3, 1 * gart.d.hf / 3f + 100
    )
    val flowField3: ForceField<FlowForce> = ForceField.of(gart.d) { x, y -> circleFlowForce(x, y) }
    // 4
    val spiralFlowForce = SpiralFlowForce(
        gart.d.wf / 3, 1 * gart.d.hf / 3f + 100
    )
    val flowField4: ForceField<FlowForce> = ForceField.of(gart.d) { x, y -> spiralFlowForce(x, y) }
    // 5
    val waveFlowForce = WaveFlowForce(
        gart.d.wf / 3, 1 * gart.d.hf / 3f + 100
    )
    val flowField5: ForceField<FlowForce> = ForceField.of(gart.d) { x, y -> waveFlowForce(x, y) }


    var ff: ForceField<*> = flowField1
    val w = gart.window()

    w.show { c, d, _ ->
        ff.drawField(c, d)
    }
        .keyboardHandler {
            if (it.kind != SkikoKeyboardEventKind.UP) {
                return@keyboardHandler
            }
            ff = when (it.key) {
                SkikoKey.KEY_1 -> flowField1
                SkikoKey.KEY_2 -> flowField2
                SkikoKey.KEY_3 -> flowField3
                SkikoKey.KEY_4 -> flowField4
                SkikoKey.KEY_5 -> flowField5
                else -> ff
            }
        }

}