package dev.oblac.gart.flowforce.spring

import dev.oblac.gart.Dimension
import dev.oblac.gart.Frames
import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.force.WaveFlow
import dev.oblac.gart.gfx.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point

// use high FPS so not to wait much.
val gart = Gart.of("Spring", 1024, 1024, fps = 1000)

val d = gart.d
val w = gart.window()

val waveFlow = WaveFlow(10f, 10f, 4f, 4f, 7f)
val ff = ForceField.of(gart.d) { x, y -> waveFlow(x, y) }

// rays

const val TRAILS = 1000
const val TRAIL_LEN = 500

val pal = Palettes.cool19.expand(TRAIL_LEN)
    .map { c ->
        Paint().apply {
            color = c
            alpha = 50
        }
    }


val trails = Array(TRAILS) { newTrail() }.toMutableList()

fun main() {
    w.show { c, _, f ->
        c.clear(BgColors.dark01)
        drawRays(c, d, f)
        c.drawRect(d.rect, strokeOfBlack(40f))

        if (f.frame == 620L) {
            gart.saveImage(c)
        }
    }
}

fun drawRays(c: Canvas, d: Dimension, f: Frames) {
    trails
        .forEach { trail ->
            trail.update { u(it, d) }
        }
    // don't draw until the end
    if (f.frame > 610) {
        trails.forEach { trail ->
            trail.sequenceIndexed().forEach {
                val i = it.index
                val p = it.value
                c.drawCircle(p.x, p.y, 4f, pal[i])
            }
        }
    }

    // add more points
    if (trails.countActive() < TRAILS) {
        repeat(TRAILS - trails.countActive()) { trails.add(newTrail2()) }
    }
}

private fun u(it: Point, d: Dimension) =
    if (it.isInside(d)) {
        ff[it].offset(it)
    } else {
        null
    }

private fun newTrail(): PointsTrail = PointsTrail(randomPoint(d), TRAIL_LEN)

private fun newTrail2(): PointsTrail = PointsTrail(
    randomPoint(Dimension(d.w, d.cy.toInt())).offset(0f, d.cy), TRAIL_LEN
)
