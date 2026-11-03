package gutta

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.argb
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.fx.bloom
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.between
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.lerp
import dev.oblac.gart.pixels.splat
import dev.oblac.gart.util.Stopwatch
import dev.oblac.gart.util.parallelForRows
import dev.oblac.gart.vector.MutableVec2
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * gutta - the light under a few drops.
 *
 * a few drops fell on still water at different times. each drop made a ring of waves. each
 * ring has a different radius. two long waves cross under the rings.
 *
 * the light comes from directly above. each ray bends where it enters the water. then the ray
 * lands on a floor below. the slope of the surface moves the ray by depth * (1 - 1/n) * slope.
 * where the surface curves, it focuses the rays. the light collects in thin threads. where two
 * rings cross, the threads fold into cusps. red, green and blue each have a different n, so the
 * threads have colored edges.
 *
 * i draw only the light. the picture has no water and no floor.
 */

private const val W = 1400
private const val H = 1400
private const val PAGE_H = 2000
private const val TOP = 100
private val SEED = pi("seed", 5)
private val OUT = ps("out", "gutta")

// the water
private val DROPS = pi("drops", 3, 1..8)
private val RIPPLE = pf("ripple", 164f, 16f..200f) // 64 looks more natural
private const val SWELL = 0.15f

// the light
private const val FOCUS = 2.2f
private const val PRISM = 0.08f // how far red and blue split. real water is about 0.01. at 0.14 the colors look like candy
private val RAYS = pi("rays", 4, 1..8) // rays along each side of a pixel

private const val EXPOSE = 0.08f
private const val GAMMA = 2f // more than 1 keeps the flat water dark and lets the threads burn. 1.5 is good too
private const val GLOW = 0.55f
private const val GRAIN = 0.025f

private val K = TAUf / RIPPLE

// the colors
private val GROUND = Color4f(0.027f, 0.043f, 0.086f)
private val LIGHT = Color4f(1f, 0.965f, 0.91f)

// the water

// one ring of waves around center, where the drop fell. the ring has radius r and it is about two wavelengths wide
private class Drop(val center: Point, val r: Float, val k: Float, val amp: Float, val phase: Float) {
    private val width = 2.2f * TAUf / k
    private val reach = 3.5f * width
    val steepest = amp * (k + 2f / width)

    fun slope(p: Point, s: MutableVec2) {
        val dx = p.x - center.x
        val dy = p.y - center.y
        val d = sqrt(dx * dx + dy * dy)
        val u = d - r
        if (abs(u) > reach || d < 1e-3f) return
        val q = u / width
        val env = exp(-q * q)
        val a = k * u + phase
        val dh = amp * env * (-2f * q / width * cos(a) - k * sin(a))
        s.add(dh * dx / d, dh * dy / d)
    }
}

// a long plane wave. k points in the direction that the wave moves. the length of k is the wavenumber
private class Swell(val k: Vec2, val amp: Float, val phase: Float) {
    val steepest = abs(amp) * k.length()

    fun slope(p: Point, s: MutableVec2) {
        val v = -amp * sin(k.x * p.x + k.y * p.y + phase)
        s.add(v * k.x, v * k.y)
    }
}

private class Water(val drops: List<Drop>, val swells: List<Swell>) {
    fun slope(p: Point, s: MutableVec2) {
        s.zero()
        for (d in drops) d.slope(p, s)
        for (w in swells) w.slope(p, s)
    }

    val steepest = drops.map { it.steepest }.sum() + swells.map { it.steepest }.sum()
}

private fun water(rnd: Random): Water {
    val drops = List(DROPS) {
        val k = K * rnd.between(0.8f, 1.25f)
        Drop(Point(W * rnd.between(0.08f, 0.92f), PAGE_H * rnd.between(0.08f, 0.92f)), W * rnd.between(0.12f, 0.55f), k, rnd.between(0.6f, 1.1f) / k, rnd.between(0f, TAUf))
    }

    val swells = List(2) {
        val kk = K / (2.6f * rnd.between(0.8f, 1.2f))
        val a = rnd.between(0f, TAUf)
        Swell(Vec2(kk * cos(a), kk * sin(a)), 0.6f * SWELL * K / (kk * kk), rnd.between(0f, TAUf))
    }
    return Water(drops, swells)
}

// the light

// the light that lands on the floor. it has one plane for each channel. a plane keeps its pixels row by row
private class Light(val w: Int, h: Int) {
    private val planes = Array(3) { FloatArray(w * h) }
    operator fun get(c: Int, p: Int) = planes[c][p]
    fun splat(c: Int, p: Point, v: Float) = splat(planes[c], w, p, v)
    fun copyInto(to: Light, row: Int) { for (c in 0..2) planes[c].copyInto(to.planes[c], row * w) }
}

private fun light(water: Water): Light {
    val gain = FOCUS / K
    val gains = FloatArray(3) { gain * (1f + PRISM * (it - 1)) } // red bends the least and blue bends the most
    val reach = (gains[2] * water.steepest).toInt() + 2
    val sub = 1f / RAYS
    val weight = sub * sub
    val light = Light(W, H)
    parallelForRows(H) { top, bottom ->
        val strip = Light(W, bottom - top)
        val s = MutableVec2()
        for (j in ((top - reach) * RAYS) until ((bottom + reach) * RAYS)) {
            for (i in (-reach * RAYS) until ((W + reach) * RAYS)) {

                val ray = Point((i + hash01(i, j, 1, SEED)) * sub, (j + hash01(i, j, 2, SEED)) * sub)
                water.slope(Point(ray.x, ray.y + TOP), s)

                // -0.5: the pixel centers are at .5, but splat() has them on whole numbers
                for (c in 0..2) strip.splat(c, Point(ray.x + gains[c] * s.x - 0.5f, ray.y + gains[c] * s.y - 0.5f - top), weight)
            }
        }
        strip.copyInto(light, top)
    }
    return light
}

// on flat water the light is 1 everywhere. burn(1) is about 0.08, so flat water stays near the ground
private fun burn(l: Float) = 1f - exp(-EXPOSE * l.pow(GAMMA))

// each channel has its own light, so red and blue still split
private fun expose(light: Light, g: Gartvas) {
    Gartmap(g).use { m ->
        for (p in 0 until W * H) {
            val red = lerp(GROUND.r, LIGHT.r, burn(light[0, p]))
            val green = lerp(GROUND.g, LIGHT.g, burn(light[1, p]))
            val blue = lerp(GROUND.b, LIGHT.b, burn(light[2, p]))
            m.pixels[p] = argb(1f, red, green, blue)
        }
        m.drawToCanvas()
    }
}

// i blur a copy of the image and screen it on top, so the threads glow a little
private fun glow(g: Gartvas) = bloom(g.canvas, g.snapshot(), 7f, GLOW)

fun main(args: Array<String>) {
    val gart = Gart.of("gutta", W, H)
    val sw = Stopwatch()
    val water = water(Random(SEED))
    val light = light(water)
    val g = gart.gartvas()
    expose(light, g)
    glow(g)
    addGrain(g, GRAIN, SEED)
    println("gutta: seed=$SEED, ${water.drops.size} drops, ${sw.ms}ms")

    gart.saveImage(g, OUT.ensureExtension())
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}
