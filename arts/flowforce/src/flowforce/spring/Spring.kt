package flowforce.spring

import dev.oblac.gart.Dimension
import dev.oblac.gart.Frames
import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.flow.WaveFlow
import dev.oblac.gart.gfx.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point

// use high FPS so not to wait much.
val gart = Gart.of("Spring", 1024, 1024, fps = 1000)

val d = _root_ide_package_.flowforce.spring.gart.d
val w = _root_ide_package_.flowforce.spring.gart.window()

val waveFlow = WaveFlow(10f, 10f, 4f, 4f, 7f)
val ff = FlowField.of(_root_ide_package_.flowforce.spring.gart.d) { x, y ->
    _root_ide_package_.flowforce.spring.waveFlow(
        x,
        y
    )
}

// rays

const val TRAILS = 1000
const val TRAIL_LEN = 500

val pal = Palettes.cool19.expand(_root_ide_package_.flowforce.spring.TRAIL_LEN)
    .map { c ->
        Paint().apply {
            color = c
            alpha = 50
        }
    }


val trails = Array(_root_ide_package_.flowforce.spring.TRAILS) { _root_ide_package_.flowforce.spring.newTrail() }.toMutableList()

fun main() {
    _root_ide_package_.flowforce.spring.w.show { c, _, f ->
        c.clear(BgColors.dark01)
        _root_ide_package_.flowforce.spring.drawRays(c, _root_ide_package_.flowforce.spring.d, f)
        c.drawRect(_root_ide_package_.flowforce.spring.d.rect, strokeOfBlack(40f))

        if (f.frame == 620L) {
            _root_ide_package_.flowforce.spring.gart.saveImage(c)
        }
    }
}

fun drawRays(c: Canvas, d: Dimension, f: Frames) {
    _root_ide_package_.flowforce.spring.trails
        .forEach { trail ->
            trail.update { _root_ide_package_.flowforce.spring.u(it, d) }
        }
    // don't draw until the end
    if (f.frame > 610) {
        _root_ide_package_.flowforce.spring.trails.forEach { trail ->
            trail.sequenceIndexed().forEach {
                val i = it.index
                val p = it.value
                c.drawCircle(p.x, p.y, 4f, _root_ide_package_.flowforce.spring.pal[i])
            }
        }
    }

    // add more points
    if (_root_ide_package_.flowforce.spring.trails.countActive() < _root_ide_package_.flowforce.spring.TRAILS) {
        repeat(_root_ide_package_.flowforce.spring.TRAILS - _root_ide_package_.flowforce.spring.trails.countActive()) {
            _root_ide_package_.flowforce.spring.trails.add(
                _root_ide_package_.flowforce.spring.newTrail2()
            )
        }
    }
}

private fun u(it: Point, d: Dimension) =
    if (it.isInside(d)) {
        _root_ide_package_.flowforce.spring.ff[it].offset(it)
    } else {
        null
    }

private fun newTrail(): PointsTrail = PointsTrail(
    randomPoint(_root_ide_package_.flowforce.spring.d),
    _root_ide_package_.flowforce.spring.TRAIL_LEN
)

private fun newTrail2(): PointsTrail = PointsTrail(
    randomPoint(Dimension(_root_ide_package_.flowforce.spring.d.w, _root_ide_package_.flowforce.spring.d.cy.toInt())).offset(0f, _root_ide_package_.flowforce.spring.d.cy),
    _root_ide_package_.flowforce.spring.TRAIL_LEN
)
