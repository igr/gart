package dev.oblac.gart.pixels

import dev.oblac.gart.Gartmap
import dev.oblac.gart.Pixel
import java.util.*

/**
 * Flood fill algorithm implementation with a predicate that determines if a pixel should be filled.
 * Fills the area connected to the start pixel with the fillColor, but only if the predicate returns true for the pixel's color.
 */
fun floodFill(m: Gartmap, start: Pixel, fillColor: Int, shouldFill: (Int) -> Boolean) {
    val fifo = LinkedList<Pixel>()
    fifo.add(start)

    while (fifo.isNotEmpty()) {
        val p = fifo.removeFirst()
        val pixelColor = m[p.x, p.y]
        if (pixelColor != fillColor && shouldFill(pixelColor)) {
            m[p.x, p.y] = fillColor
            if (p.y > 0) {
                fifo.add(Pixel(p.x, p.y - 1))
            }
            if (p.y < m.d.b) {
                fifo.add(Pixel(p.x, p.y + 1))
            }
            if (p.x < m.d.r) {
                fifo.add(Pixel(p.x + 1, p.y))
            }
            if (p.x > 0) {
                fifo.add(Pixel(p.x - 1, p.y))
            }
        }
    }
}

fun matchExactColor(targetColor: Int): (Int) -> Boolean = { it == targetColor }
fun matchNotColor(vararg excludedColors: Int): (Int) -> Boolean = { color ->
    excludedColors.none { it == color }
}
