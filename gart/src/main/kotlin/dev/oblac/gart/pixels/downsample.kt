package dev.oblac.gart.pixels

import dev.oblac.gart.Pixels

/**
 * Box-averages an [ss] x [ss] supersampled ARGB buffer down into [dst]: every output pixel is
 * the plain mean of its block, written opaque. [src] is row-major with a stride of
 * `dst.d.w * ss` and at least `dst.d.h * ss` rows; alpha is ignored.
 *
 * Integer sum over the block, integer division by the block size - the same arithmetic as
 * `(sum * (1f / n)).toInt()` for the usual ss of 2..4, and easier to reason about.
 */
fun boxDownsample(src: IntArray, ss: Int, dst: Pixels) {
    val w = dst.d.w
    val h = dst.d.h
    val stride = w * ss
    val n = ss * ss
    val px = dst.pixels
    for (y in 0 until h) {
        val by = y * ss
        for (x in 0 until w) {
            val bx = x * ss
            var r = 0
            var g = 0
            var b = 0
            var yy = 0
            while (yy < ss) {
                var i = (by + yy) * stride + bx
                var xx = 0
                while (xx < ss) {
                    val c = src[i]
                    r += (c ushr 16) and 0xFF; g += (c ushr 8) and 0xFF; b += c and 0xFF
                    i++; xx++
                }
                yy++
            }
            px[y * w + x] = (0xFF shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
        }
    }
}
