package dev.oblac.gart.flowforce.eclectic

import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.flowforce.eclectic.two.TrailPath
import dev.oblac.gart.force.Force
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.Vector2
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.PerlinNoise
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

val gart = Gart.of("Eclectic", 1024, 1024)
val d = dev.oblac.gart.flowforce.eclectic.two.gart.d

const val TRAILS = 100
const val TRAIL_LEN = 300
const val MAX_WIDTH = 30f
const val MAX_DISTANCE = 50f

data class TrailPath(
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
                .any { it.distanceTo(p) < dev.oblac.gart.flowforce.eclectic.two.MAX_DISTANCE }
            if (collide) return true
        }
        return false
    }
}

fun main() {
    val g = dev.oblac.gart.flowforce.eclectic.two.gart.gartvas()
    val c = g.canvas

    c.clear(BgColors.elegant)

    Palettes.cool35.sequence().forEachIndexed { index, it -> drawww(c, it, index) }

    dev.oblac.gart.flowforce.eclectic.two.gart.saveImage(g)
    dev.oblac.gart.flowforce.eclectic.two.gart.window().showImage(g)
}

fun ff(): ForceField {
    val noise = PerlinNoise()
    val smooth = 300
    val step = 10
    val ff = ForceField.of(dev.oblac.gart.flowforce.eclectic.two.gart.d) { x, y ->
        object : Force {
            override fun apply(p: Point): Vector2 {
                val n = noise.noise(p.x / smooth, p.y / smooth) * 3
                return Vector2(cos(n - 0.5) * step, sin(n - 0.5) * step)
            }
        }
    }
    return ff
}

val ff = dev.oblac.gart.flowforce.eclectic.two.ff()

private fun drawww(c: Canvas, color: Int, index: Int) {
    //val ff = ff()
    val tps = Array(dev.oblac.gart.flowforce.eclectic.two.TRAILS) { PointsTrail(dev.oblac.gart.flowforce.eclectic.two.TRAIL_LEN).apply { add(randomPoint(dev.oblac.gart.flowforce.eclectic.two.d)) } }
        .map { TrailPath(it, rndf(6f, dev.oblac.gart.flowforce.eclectic.two.MAX_WIDTH)) }
        .toMutableList()
    repeat(dev.oblac.gart.flowforce.eclectic.two.TRAIL_LEN) {
        tps
            .filter { it.active }
            .filter { !it.trail.isEmpty() }
            .forEach { tp ->
                val lastP = tp.trail.last()
                if (tp.collide(lastP, tps)) {
                    tp.active = false
                } else {
                    tp.trail.update {
                        it.ifInside(dev.oblac.gart.flowforce.eclectic.two.d)?.let { p ->
                            dev.oblac.gart.flowforce.eclectic.two.ff[p].offset(p)
                        }
                    }
                    tp.started = true
                }
            }
    }

    // draw
    when (index) {
        3 -> c.drawCircle(dev.oblac.gart.flowforce.eclectic.two.d.w - 200f, dev.oblac.gart.flowforce.eclectic.two.d.hf / 3, 100f, fillOf(BgColors.coconutMilk))
        6 -> c.drawCircle(dev.oblac.gart.flowforce.eclectic.two.d.cx / 3 + 400, dev.oblac.gart.flowforce.eclectic.two.d.hf / 3 + 400, 80f, fillOf(BgColors.coconutMilk))
        7 -> c.drawCircle(dev.oblac.gart.flowforce.eclectic.two.d.cx / 2, dev.oblac.gart.flowforce.eclectic.two.d.hf / 3, 100f, fillOf(BgColors.coconutMilk))
        9 -> c.drawCircle(dev.oblac.gart.flowforce.eclectic.two.d.cx, dev.oblac.gart.flowforce.eclectic.two.d.cy, 50f, fillOf(BgColors.coconutMilk))
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
    c.drawBorder(dev.oblac.gart.flowforce.eclectic.two.d, 20f, BgColors.bg08)
}
