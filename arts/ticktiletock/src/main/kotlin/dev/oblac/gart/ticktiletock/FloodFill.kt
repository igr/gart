package dev.oblac.gart.ticktiletock

import dev.oblac.gart.Gartmap
import java.util.*

data class Pixel(val x: Int, val y: Int)

fun floodFill(m: Gartmap, start: Pixel, fillColor: Int)  {
    val fifo = LinkedList<Pixel>()
    fifo.add(start)

    while (fifo.isNotEmpty()) {
        val p = fifo.removeFirst()
        if (m[p.x, p.y] != fillColor) {
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


fun floodFill(m: Gartmap, start: Pixel, fillColor: Int, downColor: Int)  {
    val fifo = LinkedList<Pixel>()
    fifo.add(start)

    while (fifo.isNotEmpty()) {
        val p = fifo.removeFirst()
        if (m[p.x, p.y] == downColor) {
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
