package ac.obl.gart.ticktiletock

import ac.obl.gart.Box

fun splitBox(box: Box, parts: Int): Array<Array<Tile>> {
    val d = box.wf / parts

    return Array(parts) { i ->
        Array(parts) { j ->
            Tile(i * d, j * d, d)
        }
    }
}
