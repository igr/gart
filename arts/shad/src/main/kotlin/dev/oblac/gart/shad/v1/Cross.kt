package dev.oblac.gart.shad.v1

import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.argb
import dev.oblac.gart.color.blendColors
import dev.oblac.gart.gfx.draw
import dev.oblac.gart.math.DOUBLE_PIf
import dev.oblac.gart.vector.*
import kotlin.math.*

fun main() {
    val gart = Gart.of("cross", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // Hot reload requires a real class to be created, not a lambda!

    val draw = MyDraw(g)

    // save image
//    g.draw(draw)
//    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDraw(g: Gartvas) : Drawing(g) {
    val b = Gartmap(g)
    val d = g.d
    val c = g.canvas

    init {
        draw(b, Vec2(d.w.toFloat(), d.h.toFloat()), 111f)
        b.drawToCanvas()
        c.draw(g)
    }
}

private fun draw(bmp: Gartmap, resolution: Vec2, time: Float) {
    val d = bmp.d
    val height = d.h
    val width = d.w

    for (y in 0 until height) {
        for (x in 0 until width) {
            val fragCoord = Vec2(x.toFloat(), y.toFloat())
            val color = cross(fragCoord, resolution, time)
            bmp[x, y] = blendColors(
                argb(color.w, color.x, color.y, color.z),
                BgColors.outerSpace
            )
        }
    }
}

private fun palette(t: Float): Vec3 {
    val a = Vec3(0.5f, 0.5f, 0.5f)
    val b = Vec3(0.5f, 0.5f, 0.5f)
    val c = Vec3(1.0f, 1.0f, 1.0f)
    val d = Vec3(0.163f, 0.416f, 0.557f)

    return a + b * cos(Vec3(DOUBLE_PIf, DOUBLE_PIf, DOUBLE_PIf) * (c * t + d))
}

private fun cross(fragCoord: Vec2, resolution: Vec2, time: Float): Vec4 {
    var uv = (fragCoord * 2f - resolution) / resolution.y
    val uv0 = uv
    var finalColor = Vec3.ZERO

    for (i in 0 until 4) {
        uv = frac(uv * 1.5f) - 0.5f

        val d0 = length(uv) * exp(-length(uv0))

        val col = palette(length(uv0) + i * 0.4f + time * 0.4f)

        var d = sin(d0 * 8f + time) / 8f
        d = abs(d)

        d = (0.01f / d).pow(1.3f)

        finalColor += col * d
    }

    return Vec4(finalColor.x, finalColor.y, finalColor.z, 1f)
}
