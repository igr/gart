package dev.oblac.gart

import dev.oblac.gart.math.TWO_PIf
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.reactiondiffusion.*
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.random.Random

/**
 * Reaction-diffusion sampler: Gray-Scott, FitzHugh-Nagumo and continuous
 * Belousov-Zhabotinsky over a shared window.
 *
 * Controls:
 * - 1: Gray-Scott (default)
 * - 2: FitzHugh-Nagumo
 * - 3: Belousov-Zhabotinsky (continuous)
 * - R: Reset and reseed
 */
fun main() {
    val size = 1024
    val defaultStepsPerFrame = 4
    val fhnStepsPerFrame = 1

    val gart = Gart.of("example-reaction-diffusion", size, size, 60)
    val g = gart.gartvas()
    val map = gart.gartmap(g)

    // "Fingerprints" parameter set (Karl Sims / Pearson zoo) — produces flowing
    // labyrinthine patterns rather than the slow coral mitosis of the model
    // default. Du/Dv are divided by ~2.4 to compensate for the weighted 9-point
    // Laplacian being that much stronger than the standard 5-point kernel
    // that Pearson's feed/kill diagram is calibrated against.
    fun newGrayScott() = GrayScott(
        size, size,
        feed = 0.055f,
        kill = 0.062f,
        Du = 0.066f,
        Dv = 0.033f,
    ).also { seedGrayScott(it) }

    // delta=1.0 (vs. addon default 3.0): keeps v-diffusion under the CFL limit
    // and stops it from smearing the wavefront before the refractory notch can
    // break it. Wider delta looks fine for synchronized oscillation but washes
    // away the sharp u/v boundary the wave-break seed relies on.
    fun newFitzHughNagumo() = FitzHughNagumo(size, size, delta = 1.0f).also { seedFitzHughNagumo(it) }

    // passes < 1 slows the BZ dynamics so spirals form gradually rather than
    // flickering through frames.
    fun newBz() = BelousovZhabotinskyContinuous(size, size, passes = 0.4f).also { seedBz(it) }

    var rd: ReactionDiffusion = newGrayScott()
    var modeName = "Gray-Scott"

    val w = gart.window()
    val wv = w.show { canvas, _, frames ->
        if (frames.new) {
            val stepsPerFrame = when (rd) {
                is FitzHughNagumo -> fhnStepsPerFrame
                else -> defaultStepsPerFrame
            }
            repeat(stepsPerFrame) { rd.step() }
            rd.renderTo(map, RDColoring.Default)
        }
        g.snapshotTo(canvas)
    }

    wv.onKey { key ->
        when (key) {
            Key.KEY_1 -> {
                rd = newGrayScott(); modeName = "Gray-Scott"; println("Mode: $modeName")
            }

            Key.KEY_2 -> {
                rd = newFitzHughNagumo(); modeName = "FitzHugh-Nagumo"; println("Mode: $modeName")
            }

            Key.KEY_3 -> {
                rd = newBz(); modeName = "Belousov-Zhabotinsky"; println("Mode: $modeName")
            }
            Key.KEY_R -> {
                val current = rd
                current.reset()
                when (current) {
                    is GrayScott -> seedGrayScott(current)
                    is FitzHughNagumo -> seedFitzHughNagumo(current)
                    is BelousovZhabotinskyContinuous -> seedBz(current)
                }
                println("Reset: $modeName")
            }
            else -> {}
        }
    }

    println("Reaction-Diffusion")
    println("Controls:")
    println("  1: Gray-Scott")
    println("  2: FitzHugh-Nagumo")
    println("  3: Belousov-Zhabotinsky (continuous)")
    println("  R: Reset")
}

private fun seedGrayScott(rd: GrayScott) {
    // Scatter many small Pearson-style patches (u=0.5, v=0.25) so patterns
    // form from multiple interacting fronts instead of a single central blob.
    val rng = Random.Default
    val patchRadius = 16
    val patchCount = 130
    repeat(patchCount) {
        val cx = rng.nextInt(patchRadius, rd.width - patchRadius)
        val cy = rng.nextInt(patchRadius, rd.height - patchRadius)
        for (dy in -patchRadius..patchRadius) for (dx in -patchRadius..patchRadius) {
            if (dx * dx + dy * dy <= patchRadius * patchRadius) {
                rd.setU(cx + dx, cy + dy, 0.5f)
                rd.setV(cx + dx, cy + dy, 0.25f)
            }
        }
    }
}

private fun seedFitzHughNagumo(rd: FitzHughNagumo) {
    // Create a localized wave-break: an excited disc emits a front, while a
    // refractory notch suppresses part of that front so the open end can curl
    // into the spiral and target shapes typical for FHN excitable media.
    val rng = Random.Default
    val cx = rd.width / 2 + rng.nextInt(-80, 80)
    val cy = rd.height / 2 + rng.nextInt(-80, 80)
    val excitedRadius = 90
    val refractoryRadius = 58
    val notchCx = cx + excitedRadius / 3
    val notchCy = cy + excitedRadius / 2

    for (y in 0 until rd.height) for (x in 0 until rd.width) {
        val dx = x - cx
        val dy = y - cy
        if (dx * dx + dy * dy <= excitedRadius * excitedRadius) {
            rd.setU(x, y, 0.95f)
            rd.setV(x, y, 0.05f)
        }
    }
    for (y in 0 until rd.height) for (x in 0 until rd.width) {
        val dx = x - notchCx
        val dy = y - notchCy
        if (dx * dx + dy * dy <= refractoryRadius * refractoryRadius) {
            rd.setU(x, y, 0f)
            rd.setV(x, y, 1f)
        }
    }
}

private fun seedBz(rd: BelousovZhabotinskyContinuous) {
    // Smooth phase-field seed. Two low-frequency SimplexNoise fields act as
    // the real/imaginary parts of a complex field; atan2 maps them to a
    // phase θ(x, y) whose topological defects (where both components cross
    // zero) are exactly the spiral cores BZ wants. Each cell then sits on
    // the limit cycle at phase θ + k·120°. Produces a handful of well-formed
    // spirals in ~50 steps instead of the long grey transient from uniform
    // noise, where ~1 defect per pixel mutually annihilates.
    val rng = Random.Default
    val ox = rng.nextDouble(0.0, 1000.0)
    val oy = rng.nextDouble(0.0, 1000.0)
    val freq = 0.008
    val phaseB = TWO_PIf / 3f
    val phaseC = 2f * TWO_PIf / 3f
    for (y in 0 until rd.height) for (x in 0 until rd.width) {
        val xs = x * freq + ox
        val ys = y * freq + oy
        val re = SimplexNoise.noise(xs, ys)
        val im = SimplexNoise.noise(xs + 97.3, ys + 41.7)
        val phase = atan2(im, re).toFloat()
        rd.setA(x, y, 0.5f + 0.5f * sin(phase))
        rd.setB(x, y, 0.5f + 0.5f * sin(phase + phaseB))
        rd.setC(x, y, 0.5f + 0.5f * sin(phase + phaseC))
    }
}
