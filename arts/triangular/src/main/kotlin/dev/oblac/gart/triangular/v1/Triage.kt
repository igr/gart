package dev.oblac.gart.triangular.v1

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.noise.PoissonDiskSamplingNoise
import dev.oblac.gart.triangulation.Delaunator
import dev.oblac.gart.triangulation.delaunayToVoronoi

fun main() {
    val gart = Gart.of("Triage", 1024, 1024)
    println(gart)

    // main canvas
    val g = gart.gartvas()

    val points = PoissonDiskSamplingNoise().generate(10.0, 10.0, g.d.wd - 10, g.d.hd - 10, 10.0, 10)
    //val points = List(1000) { randomPoint(g.d) }
    val triangles = Delaunator(points).triangles()
    val voronoi = delaunayToVoronoi(triangles)

    // draw on canvas
    g.draw { c, d ->
        triangles.forEach{
            t -> c.drawTriangle(t, fillOf(Palettes.cool32.random()))
        }
        points.forEach {
            //p -> c.drawCircle(p.x, p.y, 2f, fillOf(Palettes.cool30.random()))
        }
        voronoi.forEach {
            v -> v.toPathPoints().let {
                c.drawPath(it.toPath(), strokeOfBlack(2f))
            }
        }
        c.drawBorder(d, 20f, Colors.black)
    }

    //gart.saveImage(g)

    gart.window().showImage(g)
}

