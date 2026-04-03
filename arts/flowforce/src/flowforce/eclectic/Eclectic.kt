package flowforce.eclectic

import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.flow.Flow
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.PerlinNoise
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

val gart = Gart.of("Eclectic", 1024, 1024)
val d = _root_ide_package_.flowforce.eclectic.gart.d

const val TRAILS = 100
const val TRAIL_LEN = 300
const val MAX_WIDTH = 30f
const val MAX_DISTANCE = 50f

private data class TrailPath(
    val trail: PointsTrail,
    val width: Float,
    var active: Boolean = true,    // indicates if the trail still can grow
    var started: Boolean = false   // indicates if the trail was considered
) {
    fun collide(p: Point, tps: MutableList<flowforce.eclectic.TrailPath>): Boolean {
        for (tp in tps) {
            if (tp == this) continue
            if (!tp.started) continue
            val collide = tp.trail.sequence()
                .any { it.distanceTo(p) < _root_ide_package_.flowforce.eclectic.MAX_DISTANCE }
            if (collide) return true
        }
        return false
    }
}

fun main() {
    val g = _root_ide_package_.flowforce.eclectic.gart.gartvas()
    val c = g.canvas

    c.clear(BgColors.elegant)

    Palettes.cool35.sequence().forEachIndexed { index, it ->
        _root_ide_package_.flowforce.eclectic.drawww(
            c,
            it,
            index
        )
    }

    _root_ide_package_.flowforce.eclectic.gart.saveImage(g)
    _root_ide_package_.flowforce.eclectic.gart.window().showImage(g)
}

fun ff(): FlowField {
    val noise = PerlinNoise()
    val smooth = 300
    val step = 10
    val ff = FlowField.of(_root_ide_package_.flowforce.eclectic.gart.d) { x, y ->
        object : Flow {
            override fun invoke(p: Point): Vec2 {
                val n = noise.noise(p.x / smooth, p.y / smooth) * 3
                return Vec2(cos(n - 0.5) * step, sin(n - 0.5) * step)
            }
        }
    }
    return ff
}

val ff = _root_ide_package_.flowforce.eclectic.ff()

private fun drawww(c: Canvas, color: Int, index: Int) {
    //val ff = ff()
    val tps = Array(_root_ide_package_.flowforce.eclectic.TRAILS) {
        PointsTrail(
            randomPoint(_root_ide_package_.flowforce.eclectic.d),
            _root_ide_package_.flowforce.eclectic.TRAIL_LEN
        )
    }
        .map {
            _root_ide_package_.flowforce.eclectic.TrailPath(
                it,
                rndf(6f, _root_ide_package_.flowforce.eclectic.MAX_WIDTH)
            )
        }
        .toMutableList()
    repeat(_root_ide_package_.flowforce.eclectic.TRAIL_LEN) {
        tps
            .filter { it.active }
            .filter { !it.trail.isEmpty() }
            .forEach { tp ->
                val lastP = tp.trail.last()
                if (tp.collide(lastP, tps)) {
                    tp.active = false
                } else {
                    tp.trail.update {
                        it.ifInside(_root_ide_package_.flowforce.eclectic.d)?.let { p ->
                            _root_ide_package_.flowforce.eclectic.ff[p].offset(p)
                        }
                    }
                    tp.started = true
                }
            }
    }

    // draw
    when (index) {
        3 -> c.drawCircle(_root_ide_package_.flowforce.eclectic.d.w - 200f, _root_ide_package_.flowforce.eclectic.d.hf / 3, 100f, fillOf(BgColors.coconutMilk))
        6 -> c.drawCircle(_root_ide_package_.flowforce.eclectic.d.cx / 3 + 400, _root_ide_package_.flowforce.eclectic.d.hf / 3 + 400, 80f, fillOf(BgColors.coconutMilk))
        7 -> c.drawCircle(_root_ide_package_.flowforce.eclectic.d.cx / 2, _root_ide_package_.flowforce.eclectic.d.hf / 3, 100f, fillOf(BgColors.coconutMilk))
        9 -> c.drawCircle(_root_ide_package_.flowforce.eclectic.d.cx, _root_ide_package_.flowforce.eclectic.d.cy, 50f, fillOf(BgColors.coconutMilk))
    }


    tps.forEach { trail ->
        if (trail.trail.size < 100) return@forEach
        trail.trail.toPath().let {
            c.drawPath(it, strokeOf(color, trail.width).also { paint ->
                paint.strokeCap = PaintStrokeCap.ROUND
            })
        }
    }
    //ff.drawField(c, d)
    c.drawBorder(_root_ide_package_.flowforce.eclectic.d, 20f, BgColors.bg08)
}
