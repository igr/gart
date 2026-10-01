package dev.oblac.gart.pixels

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.argb
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.util.defaultWorkers
import dev.oblac.gart.util.parallelForRows

/**
 * The color of one pixel from a color function. [colorAt] gets [aa] x [aa] samples on an even grid
 * inside the pixel at ([x], [y]). The samples are box averaged: an integer mean for each channel,
 * alpha too. This is the same mean as [boxDownsample], but without the big buffer. When [aa] is 1,
 * the one sample is at the pixel center.
 */
inline fun shadeBlock(x: Int, y: Int, aa: Int, colorAt: (x: Float, y: Float) -> Int): Int {
    var a = 0
    var r = 0
    var g = 0
    var b = 0
    for (j in 0 until aa) {
        for (i in 0 until aa) {
            val c = colorAt(x + (i + 0.5f) / aa, y + (j + 0.5f) / aa)
            a += alpha(c)
            r += red(c)
            g += green(c)
            b += blue(c)
        }
    }
    val n = aa * aa
    return argb(a / n, r / n, g / n, b / n)
}

/**
 * Fills these pixels from a color function. Each pixel is a [shadeBlock] of [aa] x [aa] samples.
 * The rows are banded across [workers] and each pixel is a separate call, so the result does not
 * change with the worker count. If [colorAt] needs scratch state for each thread, call [shadeBlock]
 * from your own [parallelForRows] loop, as [dev.oblac.gart.marbling.Marbling.render] does.
 */
inline fun Pixels.shade(aa: Int, workers: Int = defaultWorkers, crossinline colorAt: (x: Float, y: Float) -> Int) {
    require(aa >= 1) { "aa must be at least 1" }
    val w = d.w
    val px = pixels
    parallelForRows(d.h, workers) { y0, y1 ->
        for (y in y0 until y1) {
            for (x in 0 until w) {
                px[y * w + x] = shadeBlock(x, y, aa) { sx, sy -> colorAt(sx, sy) }
            }
        }
    }
}
