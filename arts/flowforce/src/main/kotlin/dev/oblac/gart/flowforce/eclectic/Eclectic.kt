package dev.oblac.gart.flowforce.eclectic

import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.force.Force
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.PerlinNoise
import dev.oblac.gart.vector.Vector2
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

val gart = Gart.of("Eclectic", 1024, 1024)
val d = gart.d

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
    fun collide(p: Point, tps: MutableList<TrailPath>): Boolean {
        for (tp in tps) {
            if (tp == this) continue
            if (!tp.started) continue
            val collide = tp.trail.sequence()
                .any { it.distanceTo(p) < MAX_DISTANCE }
            if (collide) return true
        }
        return false
    }
}

fun main() {
    val g = gart.gartvas()
    val c = g.canvas

    c.clear(BgColors.elegant)

    Palettes.cool35.sequence().forEachIndexed { index, it -> drawww(c, it, index) }

    gart.saveImage(g)
    gart.window().showImage(g)
}

fun ff(): ForceField {
    val noise = PerlinNoise()
    val smooth = 300
    val step = 10
    val ff = ForceField.of(gart.d) { x, y ->
        object : Force {
            override fun apply(p: Point): Vector2 {
                val n = noise.noise(p.x / smooth, p.y / smooth) * 3
                return Vector2(cos(n - 0.5) * step, sin(n - 0.5) * step)
            }
        }
    }
    return ff
}

val ff = ff()

private fun drawww(c: Canvas, color: Int, index: Int) {
    //val ff = ff()
    val tps = Array(TRAILS) { PointsTrail(randomPoint(d), TRAIL_LEN) }
        .map { TrailPath(it, rndf(6f, MAX_WIDTH)) }
        .toMutableList()
    repeat(TRAIL_LEN) {
        tps
            .filter { it.active }
            .filter { !it.trail.isEmpty() }
            .forEach { tp ->
                val lastP = tp.trail.last()
                if (tp.collide(lastP, tps)) {
                    tp.active = false
                } else {
                    tp.trail.update {
                        it.ifInside(d)?.let { p ->
                            ff[p].offset(p)
                        }
                    }
                    tp.started = true
                }
            }
    }

    // draw
    when (index) {
        3 -> c.drawCircle(d.w - 200f, d.hf / 3, 100f, fillOf(BgColors.coconutMilk))
        6 -> c.drawCircle(d.cx / 3 + 400, d.hf / 3 + 400, 80f, fillOf(BgColors.coconutMilk))
        7 -> c.drawCircle(d.cx / 2, d.hf / 3, 100f, fillOf(BgColors.coconutMilk))
        9 -> c.drawCircle(d.cx, d.cy, 50f, fillOf(BgColors.coconutMilk))
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
    c.drawBorder(d, 20f, BgColors.bg08)
}
