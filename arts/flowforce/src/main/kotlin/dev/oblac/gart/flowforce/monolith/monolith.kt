package dev.oblac.gart.flowforce.monolith

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.flow.Flow2
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.flow.PointTracer
import dev.oblac.gart.flow.StreamlineTracer
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.distSquared
import dev.oblac.gart.math.fastSqrt
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.OpenSimplexNoise
import dev.oblac.gart.util.middle
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("monolith", 1024, 1024)
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

    val backc =  Palettes.cool97[0]//RetroColors.black01
    c.clear(backc)

    val count = 2
    // prepare flow field
    val simplex = OpenSimplexNoise(4903)
    val flowField = FlowField.of(d) { x, y ->
        val n = simplex.random2D(x * 0.0012, y * 0.0012) * 110f
        Flow2(Degrees.of(n + 180), StreamlineTracer.STEP_SIZE)
    }
    val paths = StreamlineTracer(d, flowField, 4f + 8f * count, 110 * count * 2).trace()
    // main flow:

    val backbone = PointTracer(d, flowField).trace(d.rightTop.offset(-10f, +40f), 10000)

    val p = Palettes.cool97.reversed().expand(104)
//    val p = Palettes.cool124.expand(100)
//    val p = Palettes.cool94.reversed().expand(100)

    paths.sortedByDescending {
        val middlePoint = it.points().middle()
        middlePoint.y
    }.forEachIndexed { index, it ->
        if (index == 200) {
            //c.drawCircle(d.center, 300f, fillOf(backc))
            c.drawRect(d.center.x-100f, d.center.y -200f, d.center.x + 200, d.hf,fillOf(backc))
        }
        val middlePoint = it.points().middle()
        val distance = smallestDistance(middlePoint, backbone)
        val clr = p % (distance * 0.13f).toInt()
        c.drawPath(it, strokeOf(clr, 4f - count).apply {
            this.imageFilter = ImageFilter.makeDilate(4f, 4f, null, null)
        })
        it.points().shuffled().take(10).forEach { p->
            c.drawCircle(p, rndf(1f,2f), strokeOfWhite(1f))
        }
    }
    paths.sortedByDescending {
        val middlePoint = it.points().middle()
        middlePoint.y
    }.forEachIndexed { index, it ->
        if (index == 200) {
            //c.drawCircle(d.center, 300f, fillOf(backc))
            // 1:4:9
            //   300:657
            c.drawRect(d.center.x-300, 349f, d.center.x, d.hf,fillOf(backc))
        }
        val middlePoint = it.points().middle()
        val distance = smallestDistance(middlePoint, backbone)
        val clr = p % (distance * 0.14f).toInt()
        c.drawPath(it, strokeOf(clr, 4f - count).apply {
            this.imageFilter = ImageFilter.makeDilate(4f, 4f, null, null)
        })
//        it.points().shuffled().take(10).forEach { p->
//            c.drawCircle(p, rndf(1f,2f), strokeOfWhite(1f))
//        }
    }

    paths.forEach {
        val take = (it.length() * 0.05).toInt()
        it.points().shuffled().take(take).forEach { p->
            c.drawCircle(p, rndf(1f,2f), strokeOfWhite(1f))
        }
    }

    //c.drawPath(backbone.toPath(), strokeOfRed(10f))

    gart.saveImage(g)
    val w = gart.window()
    w.showImage(g)
}

fun smallestDistance(middlePoint: Point, points: List<Point>): Float {
    return fastSqrt(points.minOf { distSquared(middlePoint, it) })
}
