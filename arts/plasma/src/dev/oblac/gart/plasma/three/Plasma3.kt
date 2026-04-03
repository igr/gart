package dev.oblac.gart.plasma.three

import dev.oblac.gart.*
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.blendColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathEffect
import org.jetbrains.skia.Point
import kotlin.math.floor
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

fun main() {
    val gart = Gart.of("plasma3", 1024, 1024, 30)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

//    w.show(MyDraw(g))

    val markerEnd = 7.seconds.toFrames(gart.fps)
    val m = gart.movieGif()
    val drw = MyDraw(g)
    m.record(w)
//        .show(MyDraw(g))
        .show { c, d, f ->
            drw.draw(c, d, f)
            c.draw(g)
            f.onFrame(markerEnd) {
                gart.saveImage(c)
                //m.stopRecording()
            }
        }
}

private fun noise1(st: Point): Float {
    fun hash(st: Point) =
        (sin(st.x * 12.3 + st.y * 78.2) * 43758.5).f()
            .let { it - floor(it) }

    val i = Point(floor(st.x), floor(st.y))
    val f = Point(st.x - floor(st.x), st.y - floor(st.y))

    val a = hash(i)
    val b = hash(Point(i.x + 1.0f, i.y))
    val c = hash(Point(i.x, i.y + 1.0f))
    val d = hash(Point(i.x + 1.0f, i.y + 1.0f))

    val ux = f.x * f.x * (3.0f - 2.0f * f.x)
    val uy = f.y * f.y * (3.0f - 2.0f * f.y)

    return a * (1.0f - ux) * (1.0f - uy) +
        b * ux * (1.0f - uy) +
        c * (1.0f - ux) * uy +
        d * ux * uy
}

private fun noise2(p: Point): Float {
    fun hash(n: Point): Float {
        val dotProduct = dot(n, pointOf(123.456789, 987.654321))
        return frac(sin(dotProduct) * 54321.9876).f()
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

private fun fbm1(p: Point, octaves: Int): Float {
    var value = 0f
    var amplitude = 0.5f
    var e = 3f
    var p = p

    repeat(octaves) {
        // for noise1 use amplitude of 0.5
        // for noise2 use amplitude of 0.8 or higher
        value += amplitude * noise2(p)
        p *= e
        amplitude *= 0.8f
        e *= 0.95f
    }

    return value
}

private fun fbm2(p: Point, octaves: Int, time: Float): Float {
    return fbm1(p + fbm1(p * 5 + time, octaves), octaves)
}

private val pal = Palettes.cool66.expand(256)

private class MyDraw(g: Gartvas) : Drawing(g) {
    private val b = Gartmap(g)
    private val g2 = Gartvas(g.d)
    private val b2 = Gartmap(g2)

    init {
        g2.draw { c, d ->
            createDrawRing(d.rightBottom, 400f, 500f, 140f, 90f, 70f, Degrees(120f)).let {
                it.first(c, fillOf(pal[0]))
                it.first(c, strokeOf(pal[0], 20f).apply {
                    this.pathEffect = PathEffect.makeDiscrete(30f, 10f, rndi())
                })
            }

            createDrawRing(d.rightBottom, 700f, 700f, 140f, 100f, 40f, Degrees(170f)).let {
                it.first(c, fillOf(pal[0]))
                it.first(c, strokeOf(pal[0], 20f).apply {
                    this.pathEffect = PathEffect.makeDiscrete(30f, 10f, rndi())
                })
            }
        }
        b2.updatePixelsFromCanvas()
    }

    override fun draw(canvas: Canvas, dimension: Dimension, frames: Frames) {
        draw(b, dimension, frames.frame, b2)
        b.drawToCanvas()
    }
}

private fun draw(b: Gartmap, d: Dimension, t: Long, b2: Gartmap) {
    d.loop(2) { x, y ->
        val p = Point(x / d.wf, y / d.hf)
        val noiseValue = fbm2(p, 4, t / 100f)

        val index = (noiseValue * 255).toInt().coerceIn(0, 255)
        val back = pal.safe(index)
        val front = b2[x, y]
        val pixel = if (alpha(front) == 0x00) {
            back
        } else {
            if (index > 110) {
                back
            } else {
                blendColors(front, back)
            }
        }

        b[x, y] = pixel
        b[x + 1, y] = pixel
        b[x + 1, y + 1] = pixel
        b[x, y + 1] = pixel
    }
}
