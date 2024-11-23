package dev.oblac.gart.triangular.v2

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.Key
import dev.oblac.gart.attractor.LangfordAizawaAttractor
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.noise.PoissonDiskSamplingNoise
import dev.oblac.gart.triangulation.Delaunator
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Point
import kotlin.math.ln

//a=0.8199995 b=0.7 c=0.6 d=3.5 e=0.25 f=0.1
var a = 0.82f
var b = 0.7f
var c = 0.6f
var d = 3.5f
var e = 0.25f
var f = 0.1f

fun main() {
    val gart = Gart.of("Triis", 1024, 1024)
    println(gart)

    // main canvas
    val g = gart.gartvas()

    var triangles = triangles(g.d)

    // draw on canvas
    draw(g, triangles)

    gart.saveImage(g)

    gart.window().showImage(g).onKey { k ->
        when (k) {
            Key.KEY_0 -> {
                a = 0.1f
                b = 0.7f
                c = 0.6f
                d = 3.5f
                e = 0.25f
                f = 0.1f
            }

            Key.KEY_Q -> a += 0.01f
            Key.KEY_A -> a -= 0.01f
            Key.KEY_W -> b += 0.01f
            Key.KEY_S -> b -= 0.01f
            Key.KEY_E -> c += 0.01f
            Key.KEY_D -> c -= 0.01f
            Key.KEY_R -> d += 0.01f
            Key.KEY_F -> d -= 0.01f
            Key.KEY_T -> e += 0.01f
            Key.KEY_G -> e -= 0.01f
            else -> {}
        }
        println("a=$a b=$b c=$c d=$d e=$e f=$f")
        triangles = triangles(g.d)
        draw(g, triangles)
    }
}

private fun draw(g: Gartvas, triangles: List<Triangle>) {
    val points = PoissonDiskSamplingNoise().generate(10.0, 10.0, g.d.wd - 10, g.d.hd - 10, 8.0, 10)
    g.draw { c, _ ->
        c.clear(BgColors.elegantDark)
        points.forEach {
            c.drawCircle(it.x, it.y, 2f, fillOf(Palettes.cool18.random()))
        }
        triangles.forEachIndexed { i, t ->
            val a = ln(t.calculateArea())
            c.drawTriangle(t, fillOf(Palettes.cool18.safe((a).toInt())).also {
                it.strokeWidth = 1f
                it.mode = PaintMode.STROKE_AND_FILL
            })
        }
        c.drawBorder(g.d, 20f, BgColors.elegantDark)
    }
}

private fun triangles(dd: Dimension): List<Triangle> {
    val attr = LangfordAizawaAttractor(
        a = a,
        b = b,
        c = c,
        d = d,
        e = e,
        f = f
    ).computeN(LangfordAizawaAttractor.initialPoint, 0.01f, 1000)
        .map { Point(it.x, it.z) }
        .map { it.fromCenter(dd, 150f) }
        .map { it.offset(-340f, -450f) }
        .map { it.scale(3f) }

    return Delaunator(attr).triangles()
}

///a=0.8199995 b=0.7 c=0.6 d=3.5 e=0.25 f=0.1
