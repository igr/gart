package dev.oblac.gart.plasma.two

import dev.oblac.gart.*
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.dot
import dev.oblac.gart.gfx.plus
import dev.oblac.gart.gfx.pointOf
import dev.oblac.gart.gfx.times
import dev.oblac.gart.math.frac
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.smoothstep
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

fun main() {
    val gart = Gart.of("plasma2", 1024, 1024, 30)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    val d = MyDraw(g)
    d.draw(g.canvas, g.d, Frames.ZERO)
    d.b.drawToCanvas(g)
    gart.saveImage(g)

    //w.show(MyDraw(g)).hotReload(g)
}

private fun noise2(p: Point): Float {
    fun hash(n: Point): Float {
        val dotProduct = dot(n, pointOf(100, 90))
        return frac(sin(dotProduct) * 5432)
    }

    val i = Point(floor(p.x), floor(p.y))
    val u = Point(
        smoothstep(0f, 1f, frac(p.x)),
        smoothstep(0f, 1f, frac(p.y))
    )

    val a = hash(i + Point(0f, 0f))
    val b = hash(i + Point(1f, 0f))
    val c = hash(i + Point(0f, 1f))
    val d = hash(i + Point(1f, 1f))

    val r = lerp(
        lerp(a, b, u.x),
        lerp(c, d, u.x),
        u.y
    )

    return r * r
}

private fun fbm3(p: Point, octaves: Int): Float {
    val time = -3.1f
    var value = 0f
    var amplitude = 0.6f
    var frequency = 3.5f            // zoom
    val pp = p + Point(time * 0.1f, time * 0.07f)

    repeat(octaves) {
        val n = 1.14f - abs(noise2(pp * frequency) * 2.8f - 1.1f)
        value += n * n * amplitude

        frequency *= 1.6f               // shapes
        amplitude *= 0.53f              // sharp
    }

    return value.coerceIn(0f, 1f)
}

//private val pal = Palettes.colormap009.expand(256)
private val pal = Palettes.cool56.expand(256)

private class MyDraw(g: Gartvas) : Drawing(g) {
    val b = Gartmap(g)

    init {
        println("Hello")
    }

    override fun draw(canvas: Canvas, dimension: Dimension, frames: Frames) {
        draw(b, dimension, frames.frame)
        b.drawToCanvas()
    }
}

private fun draw(b: Gartmap, d: Dimension, t: Long) {
    d.loop(2) { x, y ->
        val p = Point(x / d.wf, y / d.hf)
        val noiseValue = fbm3(p, 10)

        val index = (noiseValue * 255).toInt().coerceIn(0, 255)
        val back = pal.safe(index)

        b[x, y] = back
        b[x + 1, y] = back
        b[x + 1, y + 1] = back
        b[x, y + 1] = back
    }
}
