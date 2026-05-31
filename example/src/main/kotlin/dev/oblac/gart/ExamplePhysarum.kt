package dev.oblac.gart

import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.math.PIf
import dev.oblac.gart.physarum.Physarum
import java.awt.event.MouseEvent

/**
 * The simulation engine lives in the gart module; this file is just the window,
 * the controls and the colour mapping.
 *
 * Controls (focus the window):
 *   SPACE  scatter agents randomly        D  collapse agents into a centre disc
 *   C      clear the field                 G  toggle colour / greyscale
 *   drag   draw a stream of agents         double-click  spawn a disc at the cursor
 *   Q / W  sensor angle    -/+            A / S  rotation angle  -/+
 *   Z / X  sensor distance -/+            E / R  step size       -/+
 *   O / P  brightness      -/+
 */

private const val DEG = PIf / 180

private val INFERNO = intArrayOf(
    0xFF000004.toInt(), 0xFF1b0c41.toInt(), 0xFF4a0c6b.toInt(), 0xFF781c6d.toInt(), 0xFFa52c60.toInt(),
    0xFFcf4446.toInt(), 0xFFed6925.toInt(), 0xFFfb9b06.toInt(), 0xFFf7d13d.toInt(), 0xFFfcffa4.toInt(),
)

private fun buildLut(stops: IntArray): IntArray {
    val lut = IntArray(256)
    val seg = stops.size - 1
    for (i in 0 until 256) {
        val t = i / 255f * seg
        var k = t.toInt()
        if (k > seg - 1) k = seg - 1
        lut[i] = lerpColor(stops[k], stops[k + 1], t - k)
    }
    return lut
}

private fun buildGrey(): IntArray =
    IntArray(256) { i -> (0xff shl 24) or (i shl 16) or (i shl 8) or i }

fun main() {
    val gart = Gart.of("Physarum", 1024, 1024, 60)
    val d = gart.d
    val b = Gartmap(d)          // in-memory pixel buffer, drawn each frame via image()
    val win = gart.window()

    val agentCount = 1 shl 18   // 262 144 agents — same count as the original (512x512)
    val sim = Physarum(d.w, d.h, agentCount)

    val inferno = buildLut(INFERNO)
    val grey = buildGrey()
    var colour = true
    var gain = 2.0f             // the field sits in [0.01, 1]; brighten faint trails

    // mouse brush state (written on the Swing thread, read on the render thread)
    var brushX = 0f
    var brushY = 0f
    var drawing = false

    println(
        """
        |Physarum — pure-CPU slime mould (${d.w}x${d.h}, $agentCount agents)
        |  SPACE scatter | D disc | C clear | G colour | drag draw | dbl-click disc
        |  Q/W sensor angle  A/S rotation  Z/X sensor dist  E/R step  O/P brightness
        """.trimMargin()
    )

    val view = win.show { c, _, f ->
        f.tick {
            if (drawing) sim.draw(brushX, brushY, 24f, 400)
            sim.step()

            val src = sim.trail
            val area = sim.area
            val lut = if (colour) inferno else grey
            var i = 0
            while (i < area) {
                var v = src[i] * gain
                if (v > 1f) v = 1f else if (v < 0f) v = 0f
                b[i] = lut[(v * 255f).toInt()]
                i++
            }
        }
        c.drawImage(b.image(), 0f, 0f)
    }

    view.onKey { key ->
        when (key) {
            Key.KEY_SPACE -> sim.scatter()
            Key.KEY_D -> sim.seedDisc(d.cx, d.cy, minOf(d.w, d.h) * 0.25f)
            Key.KEY_C -> sim.clear()
            Key.KEY_G -> colour = !colour
            Key.KEY_Q -> sim.sensorAngle = (sim.sensorAngle - DEG).coerceAtLeast(0f)
            Key.KEY_W -> sim.sensorAngle += DEG
            Key.KEY_A -> sim.rotationAngle = (sim.rotationAngle - DEG).coerceAtLeast(0f)
            Key.KEY_S -> sim.rotationAngle += DEG
            Key.KEY_Z -> sim.sensorDistance = (sim.sensorDistance - 1f).coerceAtLeast(1f)
            Key.KEY_X -> sim.sensorDistance += 1f
            Key.KEY_E -> sim.stepSize = (sim.stepSize - 0.1f).coerceAtLeast(0.1f)
            Key.KEY_R -> sim.stepSize += 0.1f
            Key.KEY_O -> gain = (gain - 0.25f).coerceAtLeast(0.25f)
            Key.KEY_P -> gain += 0.25f
            else -> {}
        }
    }

    view.onMouseMotion { e ->
        brushX = e.x.toFloat()
        brushY = e.y.toFloat()
        drawing = (e.modifiersEx and MouseEvent.BUTTON1_DOWN_MASK) != 0
    }

    view.onMouse { e ->
        if (e.clickCount >= 2) sim.seedDisc(e.x.toFloat(), e.y.toFloat(), minOf(d.w, d.h) * 0.2f)
    }
}
