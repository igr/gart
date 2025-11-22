package dev.oblac.gart.shad.v2

import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.gfx.draw
import dev.oblac.gart.math.f
import dev.oblac.gart.pixader.pixdraw
import dev.oblac.gart.vector.Vec2
import dev.oblac.gart.vector.Vec3
import dev.oblac.gart.vector.Vec4
import dev.oblac.gart.vector.Vector3
import dev.oblac.gart.vector.frac
import dev.oblac.gart.vector.length
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

fun main() {
    val gart = Gart.of("earth2", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // Hot reload requires a real class to be created, not a lambda!

    val draw = MyFooDraw(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyFooDraw(g: Gartvas) : Drawing(g) {
    val b = Gartmap(g)
    val d = g.d
    val c = g.canvas

    init {
        draw(b, Vec2(d.w.f(), d.h.f()), 11f)
        b.drawToCanvas()
        c.draw(g)
    }
}

private fun draw(bmp: Gartmap, iResolution: Vec2, time: Float) {
    with(bmp) {
        pixdraw(iResolution, time) { fragCoord, iRes, iTime ->
            draw(fragCoord, iRes, iTime)
        }
    }
}

private val pal = Palettes.mix9.expand(1000)

private fun palette(t: Float): Vec3 {
    val index = t.coerceIn(0f, 1f) * (pal.size - 1)
    val color = pal[index.toInt()]
    return Vector3(
        red(color) / 255f,
        green(color) / 255f,
        blue(color) / 255f
    )
}

private fun draw(fragCoord: Vec2, resolution: Vec2, time: Float): Vec4 {
    // normalized pixel coordinates (from -1 to 1) from the center
    var uv = (fragCoord * 2f - resolution) / resolution
    val uv0 = uv
    var col = Vec3.ZERO

    repeat(2) {
        // tile the space
        uv = frac(uv * 1.05) - 0.1

        var d = uv.length() * exp(-length(uv0) * 1.1f)
        col = palette(d + it * 0.5f)

        d = sin(d*4)
        d = abs(d)
        d = 0.03f / d

        // add layer
        col += col * d
    }

    return Vec4.of(col, 1f)
}
