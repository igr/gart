package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.ColorHSL
import dev.oblac.gart.color.space.of
import org.jetbrains.skia.Color4f
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Dot-based dithering: divides the image into cells and replaces each cell
 * with a filled circle whose radius is proportional to the cell's darkness.
 * Darker cells get larger dots, lighter cells get smaller (or no) dots.
 *
 * @param dotSize cell size in pixels (each cell becomes one dot)
 * @param foreground dot color (ARGB)
 * @param background cell background color (ARGB)
 */
fun ditherDots(
    bitmap: Pixels,
    dotSize: Int = 8,
    foreground: Int = 0xFF000000.toInt(),
    background: Int = 0xFFFFFFFF.toInt()
) {
    require(dotSize >= 2) { "Dot size must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val maxRadius = dotSize / 2f

    for (cy in 0 until height step dotSize) {
        for (cx in 0 until width step dotSize) {
            val brightness = cellBrightness(bitmap, cx, cy, dotSize)
            // darkness 0..1 (0 = white, 1 = black)
            val darkness = 1f - brightness / 255f
            val radius = maxRadius * darkness

            // clear cell to background
            for (dy in 0 until min(dotSize, height - cy)) {
                for (dx in 0 until min(dotSize, width - cx)) {
                    bitmap[cx + dx, cy + dy] = background
                }
            }

            // draw filled circle
            if (radius > 0.4f) {
                val centerX = cx + maxRadius
                val centerY = cy + maxRadius
                val bound = (radius + 1f).toInt()
                val icx = centerX.toInt()
                val icy = centerY.toInt()

                for (dy in -bound..bound) {
                    for (dx in -bound..bound) {
                        val px = icx + dx
                        val py = icy + dy
                        if (px in 0 until width && py in 0 until height) {
                            val dist = sqrt((dx * dx + dy * dy).toFloat())
                            if (dist <= radius) {
                                bitmap[px, py] = foreground
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun cellBrightness(bitmap: Pixels, cx: Int, cy: Int, dotSize: Int): Float {
    var total = 0f
    var count = 0
    for (dy in 0 until min(dotSize, bitmap.d.h - cy)) {
        for (dx in 0 until min(dotSize, bitmap.d.w - cx)) {
            val pixel = bitmap[cx + dx, cy + dy]
            val hsl = ColorHSL.of(Color4f.of(pixel))
            total += hsl.l
            count++
        }
    }
    return if (count > 0) total / count * 255f else 255f
}
