package dev.oblac.gart.monet

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.alpha
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("monet2", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)
    gart.saveImage(g)
    w.showImage(g)
}

private val pal = Palettes.cool25.expand(17)
private val pal2 = Palettes.cool54

private fun draw(c: Canvas, d: Dimension) {
    val forceField2 = force2(d)
    val n = 1000
    var l = Array(n) {
        randomPoint(d)
    }.toList()

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
    val points = line.toPath().toPoints(200).map { p ->
        p.offset(
            rndf(-30f, 30f),
            rndf(-30f, 30f)
        )
    }

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
                    this.alpha = 25
                })
            }
        }
    }

    repeat(40) {
        l = l
            .filter { it.isInside(d) }
            .map { forceField2[it.x.toInt(), it.y.toInt()].offset(it) }
        c.drawPointsAsCircles(l, strokeOf(Colors.white.alpha(8), 1f))
    }
    
//    c.drawPath(line.toPath(), strokeOf(Colors.black, 2f))
}

private fun force2(d: Dimension): ForceField {
    val poles = arrayOf(Complex(0.2, -0.8))
    val holes = arrayOf(Complex(-0.2, 0.1), Complex(0.9, 0.9))

    val complexField = ComplexField.of(d) { x, y ->
        val z = x + i * y
        ComplexFunctions.polesAndHoles(poles, holes)(z)
    }
    return ForceField.from(d) { x, y ->
        complexField[x, y].let { c -> Vector2(c.real, c.imag).normalize() }
    }
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


