package dev.oblac.gart.util


fun loop(count: Int, block: (x: Int) -> Unit) {
    for (x in 0 until count) {
        block(x)
    }
}

/**
 * Double loop.
 */
fun loop(countX: Int, countY: Int, block: (x: Int, y: Int) -> Unit) {
    for (x in 0 until countX) {
        for (y in 0 until countY) {
            block(x, y)
        }
    }
}
