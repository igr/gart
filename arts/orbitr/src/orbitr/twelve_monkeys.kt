package orbitr

import dev.oblac.gart.Gart
import dev.oblac.gart.color.ColorRamp
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.halftone.HalftoneConfiguration
import dev.oblac.gart.halftone.halftoneProcess
import dev.oblac.gart.math.TWO_PIf
import dev.oblac.gart.math.i
import dev.oblac.gart.reactiondiffusion.FitzHughNagumo
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val size = 1024
    val stepsPerFrame = 1
    val cr = ColorRamp.of(Palettes.cool16)

    val gart = Gart.of("12monkeys", size, size, 60)
    val g = gart.gartvas()
    val map = gart.gartmap(g)

    // delta=1.0 (vs. addon default 3.0): keeps v-diffusion under the CFL limit
    // and stops it from smearing the wavefront before the refractory notch can
    // break it. Wider delta looks fine for synchronized oscillation but washes
    // away the sharp u/v boundary the wave-break seed relies on.
    val rd = FitzHughNagumo(size, size, delta = 1.0f)
    seed(rd)

    val w = gart.window()
    w.show { canvas, d, frames ->
        if (frames.new) {
            repeat(stepsPerFrame) {
                rd.step()
            }
            for (y in 0 until rd.height) {
                for (x in 0 until rd.width) {
                    val f = rd.displayValue(x, y) + cos((x - d.cx) * 0.005f) * 0.5f + cos((y - d.cy) * 0.005f) * 0.5f
                    map[x, y] = cr.colorAt(f.coerceIn(0f, 1f))
                }
            }

            if (frames.frame > 700L) {
                map.copyPixelsFrom(halftoneProcess(map, HalftoneConfiguration()))
            }
            map.drawToCanvas()
        }
        g.snapshotTo(canvas)
        if (frames.frame == 701L) {
            gart.saveImage(canvas)
        }
    }
}

/**
 * Wave-break seeds laid out on a circle around the canvas center — produces
 * a ring of spirals that interact symmetrically.
 */
private fun seed(rd: FitzHughNagumo) {
    val count = 12
    val excitedRadius = 60
    val refractoryRadius = 30
    val cx0 = rd.width / 2f
    val cy0 = rd.height / 2f
    repeat(count) { i ->
        val ringRadius = 360
        val theta = TWO_PIf * i / count
        val cx = (cx0 + ringRadius * cos(theta)).toInt()
        val cy = (cy0 + ringRadius * sin(theta)).toInt()
//        val nx = cx + rng.nextInt(-excitedRadius / 2, excitedRadius / 2)
//        val ny = cy + rng.nextInt(-excitedRadius / 2, excitedRadius / 2)
        val nx = cx + (excitedRadius / 2) * cos(theta)
        val ny = cy + (excitedRadius / 2) * sin(theta)
        rd.stampU(cx, cy, excitedRadius, 0.95f)
        rd.stampV(cx, cy, excitedRadius, 0.05f)
        rd.stampU(nx.i(), ny.i(), refractoryRadius, 0f)
        rd.stampV(nx.i(), ny.i(), refractoryRadius, 1f)
    }
}
