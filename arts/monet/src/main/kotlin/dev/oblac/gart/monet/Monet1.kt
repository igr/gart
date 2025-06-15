package dev.oblac.gart.monet

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("monet1", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)
    gart.saveImage(g)
    w.showImage(g)
}

private val pal = Palettes.cool48

private fun draw(c: Canvas, d: Dimension) {
    c.clear(Colors.white)

    // line
    val line = mutableListOf<Point>()
    // generate zig-zag line
    val zags = 20
    for (i in 0 until zags + 1) {
        val x = d.w * (i % 2)
        val y = d.h * i / zags
        line += Point(x, y)
    }
    val points = line.toPath().toPoints(200).map { p -> p.offset(
        rndf(-30f, 30f),
        rndf(-30f, 30f)
    ) }
//    c.drawPath(points.toPath(), strokeOf(Colors.black, 2f))

    // draw strokes
    val strokes = points.map {
        createBrushStroke(it).toList().chunked(10)
    }
    val strokes2 = strokes[0].indices.map { chunkIndex ->
        strokes.map { it[chunkIndex] }
    }
    strokes2.forEach { stroke ->
        stroke.forEachIndexed { index, groupOf10 ->
            groupOf10.parallelStream().forEach {
                c.drawPath(it, fillOf(pal.safe(index)).apply {
                    this.alpha = 20
                })
            }
        }
    }

    //c.drawPath(line.toPath(), strokeOf(Colors.black, 2f))
}

private fun createBrushStroke(p: Point, layersCount: Int = 50): Array<Path> {
    val path = createNtagonPoints(n = 8, p.x, p.y, radius = 10f)
    val layers = Array(layersCount) { createLayer(path, offsetStdDev = 20f, count = 10).toClosedPath() }
    return layers
}

private fun createLayer(points: List<Point>, offsetStdDev: Float = 15f, count: Int = 10): List<Point> {
    var p = points
    repeat(count) {
        p = deformPath(p, offsetStdDev)
    }
    return p
}


