package dev.oblac.gart.pixels

import dev.oblac.gart.Pixels

/**
 * Scrolls the pixels up by the given delta.
 */
fun Pixels.scrollUp(delta: Int) {
    for (y in delta until this.d.h) {
        for (x in 0 until this.d.w) {
            this[x, y - delta] = this[x, y]
        }
    }
}
