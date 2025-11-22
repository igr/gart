package dev.oblac.gart.shad.v1

import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.gfx.draw
import dev.oblac.gart.math.f
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.pixader.pixdrawAsync
import dev.oblac.gart.vector.*
import dev.oblac.gart.vector.Vec3
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

fun main() {
    val gart = Gart.of("foo", 1024, 1024)
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
        c.clear(BgColors.outerSpace)
        b.updatePixelsFromCanvas()
        runBlocking {
            draw(b, Vec2(d.w.f(), d.h.f()), 110.5f)
        }
        b.drawToCanvas()
        c.draw(g)
    }
}

private suspend fun draw(bmp: Gartmap, iResolution: Vec2, time: Float) = coroutineScope {
    with(bmp) {
        pixdrawAsync(iResolution, time) { fragCoord, iRes, iTime ->
            something(fragCoord, iRes, iTime)
        }
    }
}

private fun something(fragCoord: Vec2, resolution: Vec2, time: Float): Vec4 {
    // normalized pixel coordinates (from 0 to 1) from the center
    var uv = (fragCoord * 2 - resolution) / resolution

    var col = Vec3.ZERO
    var acc = 0f
    repeat(4) {
        var p = uv
        val angle = time * 0.3f + it * 1.2f
        p = p.rotate(angle)
        p = frac(p * (0.6f + it * 0.3f)) - 0.5f

        var v = sin(p * 4.0f + time * 0.8f).let { vvv -> vvv.x + vvv.y }
        v *= exp(-length(p * 1.6f))
        v = (0.4f / v).pow(2f)
        v = 1 - abs(v)
        v = smoothstep(0.0f, 0.4f, v)

        val c = (0.6f + 0.5f * sin(time * 1.4f + it * 1.1f)).pow(0.4f)
        val layerColor = vec3(
            c * 0.9f,
            c * 0.4f,
            c
        )
        col += layerColor * v
        acc += v
    }
    col /= (acc + 0.001f)
    if (col == Vec3.ZERO) {
        val d = length(uv)
        col = Vec3(
            0.1f + 0.3f * exp(-d * 6f),
            0.2f + 0.3f * exp(-d * 6f),
            0.3f + 0.3f * exp(-d * 6f)
        )
    }
    return Vec4.of(col, 1f)
}
