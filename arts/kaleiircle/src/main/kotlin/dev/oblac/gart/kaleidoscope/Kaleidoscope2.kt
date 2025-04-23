package dev.oblac.gart.kaleidoscope

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.Sprite
import dev.oblac.gart.Sprite.Companion.of
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.Lissajous
import dev.oblac.gart.noise.PoissonDiskSamplingNoise
import dev.oblac.gart.triangulation.Delaunator
import dev.oblac.gart.triangulation.delaunayToVoronoi
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.Companion.of("kaleidoscope2", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    // window 1 - source
    val g = gart.gartvas()
    val c = g.canvas

    // template image
    drawTemplateImage(c, d)

    val tSize = 450f

    val l = Lissajous(d.center, 1f, 1f, 1f, 2f)
    var angle = Degrees.ZERO + Degrees.of(30f)

    val sprite = makeTriangleSprite(g, l.position(), tSize, angle)

    drawSpriteAsKaleidoscope(c, d, sprite, tSize)

    gart.saveImage(g)
    w.showImage(g)
}

private fun drawTemplateImage(c: Canvas, d: Dimension) {
    val points = PoissonDiskSamplingNoise().generate(10.0, 10.0, d.wd - 10, d.hd - 10, 100.0, 100)

    val triangles = Delaunator(points).triangles()
    val voronoi = delaunayToVoronoi(triangles)

    triangles.forEachIndexed { i, t ->
        c.drawTriangle(t, fillOf(Palettes.cool37.safe(i)))
    }
    voronoi.forEach { v ->
        v.toPathPoints().let {
            c.drawPath(it.toPath(), strokeOfBlack(2f))
        }
    }
    c.drawBorder(d, 20f, BgColors.coolDark)
}

private fun makeTriangleSprite(g: Gartvas, point: Point, tSize: Float, angle: Degrees): Sprite {
    val sprite = customCropTriangle(g.snapshot(), point, tSize, angle).let {
        val hypotenuse = tSize / 2 * 1.5f
        val side = hypotenuse * 2 / 1.73f
        val gap = (tSize - side) / 2
        it.cropRect(gap, 0f, tSize - gap * 2, tSize * 1.5f / 2)
    }
    return sprite
}

private fun customCropTriangle(image: Image, p: Point, size: Float, angle: Degrees = Degrees.ZERO): Sprite {
    val radius = size / 2
    val triangle = Triangle.equilateral(p, radius, angle)

    val w = size.toInt()
    val h = size.toInt()
    val sprite = Gartvas(Dimension(w, h))

    val target = sprite.canvas
    target.save()
    target.clear(Colors.transparent)
    target.translate(-p.x + radius, -p.y + radius)
    target.rotate(angle.toFloat() + 30f, p.x, p.y)
    target.clipPath(triangle.path)

    target.restore()    // this is the important part, custom made chaos!!!!!!!!!!
    target.drawImage(image, 0f, 0f)

    return of(sprite)
}
