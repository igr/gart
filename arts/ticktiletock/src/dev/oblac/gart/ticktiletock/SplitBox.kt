package dev.oblac.gart.ticktiletock

import dev.oblac.gart.Dimension

fun splitBox(d: Dimension, parts: Int): Array<Array<Tile>> {
    val partW = d.wf / parts

    return Array(parts) { i ->
        Array(parts) { j ->
            Tile(i * partW, j * partW, partW)
        }
    }
}
