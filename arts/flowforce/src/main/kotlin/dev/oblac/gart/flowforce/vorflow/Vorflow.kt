package dev.oblac.gart.flowforce.vorflow

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Radians
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.flowforce.spring.gart
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.force.VecForce
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.Vector2
import dev.oblac.gart.math.rndGaussian
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.PoissonDiskSamplingNoise
import dev.oblac.gart.triangulation.Delaunator
import dev.oblac.gart.triangulation.delaunayToVoronoi
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of(
        "vorflow",
        1024, 1024
    )
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)

    val w = gart.window()
    gart.saveImage(g)
    w.showImage(g)
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.white01)

    // we need a bigger rectangle to avoid weird effects at the edges
    val dOut = d.rect.grow(200f).dimension()
    val vOut = Vector2(-200f, -200f)

    val points = PoissonDiskSamplingNoise().generate(0.0, 0.0, dOut.wd, dOut.hd, 160.0, 100)
    val triangles = Delaunator(points).triangles()
    val voronoi = delaunayToVoronoi(triangles)
        .map { it ->
            it.copy(
                site = it.site.offset(vOut),
                edges = it.edges.map { edge ->
                    edge.copy(
                        a = edge.a.offset(vOut),
                        b = edge.b.offset(vOut)
                    )
                }
            )
        }

//    voronoi.forEach { v ->
//        v.toPathPoints().let {
//            c.drawPath(it.toPath(), strokeOfBlack(2f))
//        }
//    }

    val centerCell = voronoi.find {
        it.toPathPoints().toClosedPath().contains(d.center)
    }!!
    val centerPath = centerCell.toPathPoints().toClosedPath()

    val ff = ForceField.of(gart.d) { x, y ->
        val p = Point(x, y)

        val lineOfP = voronoi.flatMap { it.edges }.find { it.isPointOnLine(p) }
        if (lineOfP != null) {
            // If the point is on a line, flow with the line
            VecForce(lineOfP.angle(), 10f)
        } else {
            // Otherwise, find the closest line
            val closestLine = voronoi.flatMap { it.edges }
                .minByOrNull { Line.fromPointToLine(p, it).length() }
            if (closestLine != null) {
                // Flow towards the closest line
                VecForce(Line.fromPointToLine(p, closestLine).angle(), 10f)
            } else {
                // If no lines are found, return a zero force
                VecForce(Radians(0f), 0f)
            }
        }
    }

    // ready to draw
    //var dots = PoissonDiskSamplingNoise().generate(10.0, 0.0, d.wd, d.hd, 16.0, 100)
    var dots = List(5_000) {
        val x = rndGaussian(d.cx, d.w * 0.5f)
        val y = rndGaussian(d.cx, d.w * 0.2f)
        Point(x, y)
    }

    repeat(100) {
        dots = ff.apply(dots) { p1, p2 ->
            //c.drawPoint(p2, strokeOfBlack(1f))
//            c.drawLine(p1, p2, strokeOfBlack(0.8f).apply {
//                alpha = 50
//            })
            val p = p2
            val color = if (centerPath.contains(p)) {
                RetroColors.red01
            } else {
                RetroColors.black01
            }
            c.drawCircle(p, rndf(2f, 10f), strokeOf(color, 0.5f).apply {
                alpha = 40
            })
        }
    }
}
