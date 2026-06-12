package cell2

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.math.isEven
import dev.oblac.gart.math.rndi
import dev.oblac.gart.physarum.Physarum
import dev.oblac.gart.pixels.applyGaussianBlur
import kotlin.math.pow


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


/**
 * Builds the displayed color ramp: a smooth gradient over [stops], reshaped by
 * [gamma] on the intensity axis. gamma < 1 lifts low/mid trail values so more of
 * the palette becomes visible; gamma == 1 is the plain linear gradient.
 * The curve is baked here (256 samples) so [renderTrail] stays a single lookup.
 */
private fun buildShades(stops: IntArray, gamma: Float): IntArray {
    val grad = buildLut(stops)
    return IntArray(256) { i ->
        val t = (i / 255f).pow(gamma)
        grad[(t * 255f).toInt()]
    }
}

/** Maps the trail field [src] through [gain] (clamped to 0..1) and the [lut] into the pixel buffer [dst]. */
private fun renderTrail(dst: Gartmap, src: FloatArray, area: Int, lut: IntArray, gain: Float) {
    var i = 0
    while (i < area) {
        var v = src[i] * gain
        if (v > 1f) v = 1f else if (v < 0f) v = 0f
        dst[i] = lut[(v * 255f).toInt()]
        i++
    }
}

fun main() {
    val gart = Gart.of("cell2", 1024, 1024, 60)
    val d = gart.d
    val b = Gartmap(d)          // in-memory pixel buffer, drawn each frame via image()
    val win = gart.window()
//    val cool = Palettes.cool56
//    val cool = Palettes.cool159.reversed()
    val cool = Palettes.cool9.reversed()
    val shades = buildShades(cool.toIntArray(), gamma = 0.6f)

    val agentCount = (1 shl 18) * 2
    val sim = Physarum(d.w, d.h, agentCount)

    sim.clear()

    //Two rules of thumb while exploring: the SA:RA ratio sets the character
    // (SA < RA → networks; SA ≈ RA and both large → spots; RA tiny → flow),
    // and sensorDistance sets the scale of whatever that character is.

//    sim.presetFineLace()
    sim.presetSilk()

    val lines = 14
    val amp = d.wf / 16
    for (k in 0 until lines) {
        val x = d.wf * (2 * k + 1) / (2f * lines)
        val lineFrom = agentCount * k / lines
        val span = agentCount / lines
        val zigs = rndi(4, 8)
        for (s in 0 until zigs) {
            val dir = if (s.isEven()) 1f else -1f
            sim.seedLine(
                x + dir * amp, d.hf * s / zigs,
                x - dir * amp, d.hf * (s + 1) / zigs,
                thickness = 3f,
                from = lineFrom + span * s / zigs,
                to = lineFrom + span * (s + 1) / zigs,
            )
        }
    }

    val brushX = 0f
    val brushY = 0f
    val drawing = true
    val gain = 1.5f          // 4.0 saturated nearly everything to the last palette color

    win.show { c, _, f ->
        f.tick {
            if (drawing) sim.draw(brushX, brushY, 24f, 800)
            sim.step()
            renderTrail(b, sim.trail, sim.area, shades, gain)

            // pixel effects — toggle one (gaussian blur active)
            applyGaussianBlur(b)
        }

        c.drawImage(b.image(), 0f, 0f)
        if (f.frame == 3_000L) {
            gart.saveImage(b.image())
        }
    }
}
