package dev.oblac.gart.kaleidoscope

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.Sprite
import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.Lissajous
import dev.oblac.gart.noise.PoissonDiskSamplingNoise
import dev.oblac.gart.triangulation.Delaunator
import dev.oblac.gart.triangulation.delaunayToVoronoi
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import kotlin.math.round
import kotlin.math.sin

fun main() {
    val gart = Gart.of("kaleidoscope3", 1024, 1024, 30)
    println(gart)

    val d = gart.d
    val w = gart.window()

    // window 1 - source
    val g = gart.gartvas()
    val c = g.canvas

    // template image
    drawTemplateImage(c, d)
    makeTriangleSprite(g, d.center, 300f, Degrees.of(0f))
    w.showImage(g)

    val l = Lissajous(d.center, 20f, 20f, 0.1f, 0.2f)

    var angle: Angle = Degrees.ZERO

    val d1 = Dimension(d.w - 4, d.h - 4)
    val m = gart.movieGif()

    m.record(w).show { c, d, f ->
        c.rotate(angle.degrees, d.cx, d.cy)
        l.step(0.1f)

        angle += Degrees(0.1f)

        val tSize = round(280f + sin(angle.degrees * 0.1f) * 100f)
        val sprite = makeTriangleSprite(g, l.position(), tSize, angle)
        // we need to draw the whole thing twice to remove the occasional "see-through"
        // between the triangles
        drawSpriteAsKaleidoscope(c, d1, sprite, tSize)
        drawSpriteAsKaleidoscope(c, d, sprite, tSize)

        f.onEveryFrame(1L) {
            println("frame: ${f.frame}")
        }
        f.onFrame(520L) {
            m.stopRecording()
            gart.saveMovie(m)
        }
    }
}

private fun drawTemplateImage(c: Canvas, d: Dimension) {
    val points = PoissonDiskSamplingNoise().generate(10.0, 10.0, d.wd - 10, d.hd - 10, 100.0)

    val triangles = Delaunator(points).triangles()
    val voronoi = delaunayToVoronoi(triangles)

    triangles.forEachIndexed { i, t ->
        c.drawTriangle(t, fillOf(Palettes.cool56.safe(i)))
    }
    voronoi.forEachIndexed { i, v ->
        v.toPathPoints().let {
            c.drawPath(it.toPath(), strokeOf(Palettes.cool56[1], 3f))
        }
    }
    c.drawBorder(d, 20f, BgColors.coolDark)
}

private fun makeTriangleSprite(g: Gartvas, point: Point, tSize: Float, angle: Angle): Sprite {
    val sprite = g.sprite().cropTriangle(point, tSize, angle).let {
        val hypotenuse = tSize / 2 * 1.5f
        val side = hypotenuse * 2 / 1.73f
        val gap = (tSize - side) / 2
        it.cropRect(gap, 0f, tSize - gap * 2, tSize * 1.5f / 2)
    }
    return sprite
}
