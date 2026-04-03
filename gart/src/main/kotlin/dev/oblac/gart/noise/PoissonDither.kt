package dev.oblac.gart.noise

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import org.jetbrains.skia.Point
import kotlin.math.*
import kotlin.random.Random

private const val TAU = (2.0 * Math.PI).toFloat()

/**
 * Variable-density Poisson-disk dithering.
 * Black pixels → dense dots (small spacing), white pixels → sparse dots (large spacing).
 *
 * Returns point positions; the caller decides how to render them (circle size, color, etc.).
 *
 * @param pixels source image (brightness drives density)
 * @param minR minimum spacing (darkest areas)
 * @param maxR maximum spacing (lightest areas)
 * @param gamma density curve exponent (higher → dots concentrate in darks)
 * @param k Bridson candidate attempts per active point
 * @param seed PRNG seed for reproducible output
 */
fun poissonDither(
    pixels: Pixels,
    minR: Float,
    maxR: Float,
    gamma: Float = 1.6f,
    brightnessMax: Float = 0.9f,
    k: Int = 30,
    seed: Int = 42
): List<Point> {
    val w = pixels.d.w
    val h = pixels.d.h
    val wf = pixels.d.wf
    val hf = pixels.d.hf
    val rng = Random(seed)

    // brightness: 0 = black, 1 = white
    fun brightness(x: Float, y: Float): Float {
        val px = floor(x).toInt().coerceIn(0, w - 1)
        val py = floor(y).toInt().coerceIn(0, h - 1)
        val color = pixels[px, py]
        return (0.299f * red(color) + 0.587f * green(color) + 0.114f * blue(color)) / 255f
    }

    // local radius: dark → minR (dense), white → maxR (sparse)
    fun localR(x: Float, y: Float): Float {
        val bri = brightness(x, y).coerceIn(0f, 1f)
        val dens = (1f - bri).pow(gamma)
        return minR + (1f - dens) * (maxR - minR)
    }

    val cell = minR / sqrt(2f)
    val cols = ceil(wf / cell).toInt() + 1
    val rows = ceil(hf / cell).toInt() + 1

    // list-based grid: multiple points per cell for correctness
    val grid = arrayOfNulls<MutableList<Int>>(cols * rows)

    val active = mutableListOf<Int>()
    val pxs = ArrayList<Float>(4096)
    val pys = ArrayList<Float>(4096)

    val reach = ceil(maxR / cell).toInt() + 1

    fun place(x: Float, y: Float) {
        val i = pxs.size
        pxs.add(x)
        pys.add(y)
        active.add(i)
        val ci = floor(y / cell).toInt() * cols + floor(x / cell).toInt()
        val list = grid[ci] ?: mutableListOf<Int>().also { grid[ci] = it }
        list.add(i)
    }

    fun valid(x: Float, y: Float): Boolean {
        if (x < 0f || x >= wf || y < 0f || y >= hf) return false
        val ri = localR(x, y)
        val cc = floor(x / cell).toInt()
        val rr = floor(y / cell).toInt()
        val c0 = max(0, cc - reach)
        val c1 = min(cols - 1, cc + reach)
        val r0 = max(0, rr - reach)
        val r1 = min(rows - 1, rr + reach)
        for (row in r0..r1) {
            val rowOff = row * cols
            for (col in c0..c1) {
                val cell = grid[rowOff + col] ?: continue
                for (ni in cell) {
                    val dx = pxs[ni] - x
                    val dy = pys[ni] - y
                    val rj = localR(pxs[ni], pys[ni])
                    val lim = max(ri, rj)
                    if (dx * dx + dy * dy < lim * lim) return false
                }
            }
        }
        return true
    }

    // distribute seeds across the area for uniform coverage
    val seedStep = maxR * 2
    var sy = seedStep / 2
    while (sy < hf) {
        var sx = seedStep / 2
        while (sx < wf) {
            val px = (sx + (rng.nextFloat() - 0.5f) * seedStep * 0.5f).coerceIn(0f, wf - 1f)
            val py = (sy + (rng.nextFloat() - 0.5f) * seedStep * 0.5f).coerceIn(0f, hf - 1f)
            if (valid(px, py)) {
                place(px, py)
            }
            sx += seedStep
        }
        sy += seedStep
    }

    if (active.isEmpty()) {
        place(wf / 2, hf / 2)
    }

    // Bridson's algorithm with variable radius
    while (active.isNotEmpty()) {
        val ai = rng.nextInt(active.size)
        val idx = active[ai]
        val px = pxs[idx]
        val py = pys[idx]
        val ri = localR(px, py)
        var ok = false

        for (attempt in 0 until k) {
            val angle = rng.nextFloat() * TAU
            val dist = ri * (1f + rng.nextFloat())
            val nx = px + dist * cos(angle.toDouble()).toFloat()
            val ny = py + dist * sin(angle.toDouble()).toFloat()
            if (valid(nx, ny)) {
                place(nx, ny)
                ok = true
            }
        }
        if (!ok) {
            active.removeAt(ai)
        }
    }

    // filter out dots in bright areas (white background)
    val result = ArrayList<Point>(pxs.size)
    for (i in 0 until pxs.size) {
        if (brightness(pxs[i], pys[i]) <= brightnessMax) {
            result.add(Point(pxs[i], pys[i]))
        }
    }
    return result
}
