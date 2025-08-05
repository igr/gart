package dev.oblac.gart.plasma.four

import dev.oblac.gart.*
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RGBA
import dev.oblac.gart.math.lerp
import org.jetbrains.skia.Canvas
import kotlin.math.floor

fun main() {
    val gart = Gart.of("plasma4", 1024, 1024, 30)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    //w.show(MyDraw(g)).hotReload(g)

    val d = MyDraw(g)
    d.draw(g.canvas, g.d, Frames.ZERO)
    d.b.drawToCanvas(g)
    gart.saveImage(g)
}

private var animationSpeed = 0.5f
private var warpIntensity = 4.0f
private var zoom = 2.0f

private fun fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)

fun hash(x: Int, y: Int): Double {
    val h = (x * 374761393 + y * 668265263) xor 1274126177
    val hXor = h xor (h shr 13)
    val h1 = hXor.toLong() * 1274126177L
    val h2 = h1.toInt() xor 0x85ebca6b.toInt()
    val h3 = (h2 xor (h2 ushr 13)) * 0xc2b2ae35L
    val h3Shr16 = h3.toInt() shr 16
    val h3Xor = h3.toInt() xor h3Shr16
    return h3Xor / 2147483647.0
}

fun hash2(x: Int, y: Int): Double {
    val h = (x * 492876847 + y * 823641503) xor 1597463007
    val hXor = h xor (h shr 15)
    val h1 = hXor.toLong() * 1597463007L
    val h2 = h1.toInt() xor 0x9e3779b9.toInt()
    val h3 = (h2 xor (h2 ushr 11)) * 0xbf58476dL
    val h3Shr18 = h3.toInt() shr 18
    val h3Xor = h3.toInt() xor h3Shr18
    return h3Xor / 2147483647.0
}

private fun noise(x: Double, y: Double): Double {
    val xf = x - floor(x)
    val yf = y - floor(y)

    val u = fade(xf)
    val v = fade(yf)

    val xInt = floor(x).toInt() and 0xFF
    val yInt = floor(y).toInt() and 0xFF

    val a = hash2(xInt, yInt)
    val b = hash2(xInt + 1, yInt)
    val c = hash2(xInt, yInt + 1)
    val d = hash2(xInt + 1, yInt + 1)

    return lerp(lerp(u, a, b), lerp(u, c, d), v)    // 🔥 used by mistake!
//    return lerp(lerp(a, b, u), lerp(c, d, u), v)
}

// Fractional Brownian Motion
private fun fbm(x: Double, y: Double, octaves: Int = 6): Double {
    var value = 0.0
    var amplitude = 0.52
    var frequency = 0.78        // 🔥1.0

    repeat(octaves) {
        value += amplitude * noise(x * frequency, y * frequency)
        amplitude *= 0.5
        frequency *= 1.0        // 🔥kind of zoom, use 1 - 2
    }

    return value
}

private data class PatternResult(val f: Double, val qx: Double, val qy: Double, val rx: Double, val ry: Double)

private fun pattern(x: Float, y: Float, time: Float): PatternResult {
    val t = time * 0.1

    // First domain warping
    val qx = fbm(x + 0.0 + t * 0.1, y + 0.0, 4)
    val qy = fbm(x + 5.2, y + 1.3 + t * 0.15, 4)

    // Second domain warping
    val rx = fbm(x + warpIntensity * qx + 0.7, y + warpIntensity * qy + 9.2 + t * 0.2, 4)
    val ry = fbm(x + warpIntensity * qx + 8.3 + t * 0.1, y + warpIntensity * qy + 2.8, 4)

    // Final pattern
    val f = fbm(x + warpIntensity * rx, y + warpIntensity * ry, 6)

    return PatternResult(f, qx, qy, rx, ry)
}

private val pal = Palettes.cool9.expand(256)
//private val pal = Palettes.cool56.expand(256)

private fun calcColor(value: Double, qx: Double, qy: Double, rx: Double, ry: Double): RGBA {
    val r = 0.1 + value * 0.2 + qx * 0.1
    val g = 0.3 + value * 0.4 + qy * 0.2
    val b = 0.5 + value * 0.5 + rx * 0.3 + ry * 0.2

//    return ColorRGBA(
//        r.toFloat().coerceIn(0f, 1f),
//        g.toFloat().coerceIn(0f, 1f),
//        b.toFloat().coerceIn(0f, 1f),
//    ).toRGBA()

    val rr = r.toFloat().coerceIn(0f, 1f)
    val gg = g.toFloat().coerceIn(0f, 1f)
    val bb = b.toFloat().coerceIn(0f, 1f)
    val index = ((rr * 255).toInt() + (gg * 255).toInt() + (bb * 255).toInt()) % 256
//    val index  = ((rr * 255).toInt() + (gg * 255).toInt() + (bb * 255).toInt()) % 256
    return RGBA.of(pal[index])
}

private class MyDraw(g: Gartvas) : Drawing(g) {
    val b = Gartmap(g)

    override fun draw(canvas: Canvas, dimension: Dimension, frames: Frames) {
        val time = frames.frame * animationSpeed
        draw(b, dimension, time)
        b.drawToCanvas()
    }
}

private fun draw(b: Gartmap, d: Dimension, time: Float) {
    d.loop(1) { x, y ->
        // normalized coordinates
        val nx = (x.toFloat() / d.wf - 0.5f) * zoom * 0.3f //🔥
        val ny = (y.toFloat() / d.hf - 1.55f) * zoom * 1.2f

//        val t = 7155.0f
        val t = 2495f
        val patternResult = pattern(nx, ny, t)

        val color = calcColor(
            patternResult.f,
            patternResult.qx,
            patternResult.qy,
            patternResult.rx,
            patternResult.ry,
        )

        b[x, y] = color.value
    }
}
