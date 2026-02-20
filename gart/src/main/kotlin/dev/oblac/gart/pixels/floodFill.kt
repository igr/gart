package dev.oblac.gart.pixels

import dev.oblac.gart.Gartmap
import dev.oblac.gart.Pixel
import dev.oblac.gart.color.colorDistance
import dev.oblac.gart.color.lerpColor
import java.util.*

/**
 * Flood fill algorithm implementation.
 * The [shouldFill] predicate receives the current pixel color and returns
 * a fill ratio: 0.0 = don't fill, 1.0 = fully replace with [fillColor].
 * Values in between produce a blend, preserving anti-aliased edges.
 */
fun floodFill(m: Gartmap, start: Pixel, fillColor: Int, shouldFill: (Int) -> Float) {
    val w = m.d.w
    val visited = BitSet(w * m.d.h)
    val fifo = LinkedList<Pixel>()
    fifo.add(start)
    visited.set(start.y * w + start.x)

    while (fifo.isNotEmpty()) {
        val p = fifo.removeFirst()
        val pixelColor = m[p.x, p.y]
        val ratio = shouldFill(pixelColor)
        if (ratio > 0f) {
            m[p.x, p.y] = lerpColor(pixelColor, fillColor, ratio)
            fun enqueue(x: Int, y: Int) {
                val idx = y * w + x
                if (!visited.get(idx)) {
                    visited.set(idx)
                    fifo.add(Pixel(x, y))
                }
            }
            if (p.y > 0) enqueue(p.x, p.y - 1)
            if (p.y < m.d.b) enqueue(p.x, p.y + 1)
            if (p.x < m.d.r) enqueue(p.x + 1, p.y)
            if (p.x > 0) enqueue(p.x - 1, p.y)
        }
    }
}

fun matchExactColor(targetColor: Int): (Int) -> Float = {
    if (it == targetColor) 1f else 0f
}

fun matchNotColor(vararg excludedColors: Int): (Int) -> Float = { color ->
    if (excludedColors.none { it == color }) 1f else 0f
}

/**
 * Matches colors within a [tolerance] (0-255) of the [targetColor].
 * Returns 1.0 for exact matches, fading to 0.0 at the tolerance boundary,
 * preserving anti-aliased edges.
 */
fun matchSimilarColor(targetColor: Int, tolerance: Int = 30): (Int) -> Float = { color ->
    val dist = colorDistance(color, targetColor)
    if (dist > tolerance) 0f else 1f - dist.toFloat() / tolerance
}
